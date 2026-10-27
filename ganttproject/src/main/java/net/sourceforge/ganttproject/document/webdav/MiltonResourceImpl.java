/*
Copyright 2012 GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject.document.webdav;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.milton.common.Path;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.httpclient.File;
import io.milton.httpclient.Folder;
import io.milton.httpclient.Host;
import io.milton.httpclient.HttpException;
import io.milton.httpclient.IfMatchCheck;
import io.milton.httpclient.ProgressListener;
import io.milton.httpclient.PropFindResponse;
import io.milton.httpclient.Resource;
import io.milton.httpclient.Utils.CancelledException;
import net.sourceforge.ganttproject.GPLogger;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * Implementation which uses Milton client library.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class MiltonResourceImpl implements WebDavResource {
  private static final ProgressListener PROGRESS_LISTENER_STUB = null;

  /**
   * The ETag has to be asked for by name.
   *
   * Milton's default PROPFIND requests DAV:etag, which is not a WebDAV property at all - RFC 4918
   * calls it DAV:getetag - while PropFindResponse#getEtag() reads DAV:getetag. The two never meet,
   * so getEtag() returned null whatever the server sent, and every write went out unconditionally.
   */
  private static final QName ETAG_PROPERTY = new QName("DAV:", "getetag");

  private Resource myImpl;

  /**
   * The ETag the resource carried when it was read: the version the pending changes are based on.
   * Null until something has been read.
   */
  private String myEtagAtRead;

  private final WebDavUri myUrl;
  private final Host myHost;
  private Boolean myExistance;
  private MiltonResourceFactory myFactory;

  MiltonResourceImpl(WebDavUri webDavUri, Resource impl, MiltonResourceFactory factory) {
    myUrl = webDavUri;
    myImpl = impl;
    myFactory = factory;
    myExistance = true;
    myHost = impl.host();
  }

  MiltonResourceImpl(WebDavUri uri, Host host, MiltonResourceFactory factory) {
    myFactory = factory;
    myUrl = uri;
    myHost = host;
  }

  @Override
  public boolean exists() throws WebDavException {
    if (myExistance == null) {
      Resource impl = getOptionalImpl();
      myExistance = Boolean.valueOf(impl != null);
    }
    return myExistance;
  }

  private void assertExists() {
    try {
      if (!exists()) {
        throw new WebDavRuntimeException(MessageFormat.format("Resource {0} does not exist on {1}", myUrl.path, myUrl.hostName));
      }
    } catch (WebDavException e) {
      throw new WebDavRuntimeException(MessageFormat.format("Resource {0} does not exist on {1}", myUrl.path, myUrl.hostName), e);
    }
  }
  @Override
  public boolean isCollection() {
    assertExists();
    return (myImpl instanceof File) == false;
  }

  private Resource getOptionalImpl() throws WebDavException {
    if (myImpl != null) {
      return myImpl;
    }
    Host host = getHost();
    try {
      Resource resolved = host.find(myUrl.path);
      if (resolved != null) {
        myImpl = resolved;
        return myImpl;
      }
    } catch (NotAuthorizedException e) {
      throw new WebDavException(MessageFormat.format("User {0} is not authorized to access {1}", getUsername(), myUrl.hostName), e);
    } catch (BadRequestException e) {
      throw new WebDavException(MessageFormat.format("Bad request when accessing {0}", myUrl.hostName), e);
    } catch (IOException e) {
      throw new WebDavException(MessageFormat.format("I/O problems when accessing {0}", myUrl.hostName), e);
    } catch (HttpException e) {
      throw new WebDavException(MessageFormat.format("HTTP problems when accessing {0}", myUrl.hostName), e);
    }
    return null;
  }

  private Host getHost() {
    return myHost;
  }

  @Override
  public boolean isLockSupported(boolean exclusive) {
    assertExists();
    if (myImpl.getSupportedLock() == null) {
      return false;
    }
    if (exclusive) {
      return myImpl.getSupportedLock().exclusive;
    }
    return myImpl.getSupportedLock().shared;
  }

  @Override
  public List<String> getLockOwners() {
    if (myImpl == null) {
      return Collections.emptyList();
    }
    String lockOwner = myImpl.getLockOwner();
    if (lockOwner != null) {
      return ImmutableList.of(lockOwner);
    }
    String lockToken = myImpl.getLockToken();
    return lockToken == null ? Collections.emptyList() : ImmutableList.of("Unknown user");
  }

  public boolean canLock(String username) {
    assertExists();
    if (!isLockSupported(true)) {
      return false;
    }
    List<String> lockOwners = getLockOwners();
    return lockOwners.isEmpty() || lockOwners.equals(ImmutableList.of(username));
  }

  @Override
  public boolean isLocked() {
    return !getLockOwners().isEmpty();
  }

  @Override
  public void lock(int timeout) throws WebDavException {
    assertExists();
    try {
      myImpl.lock(timeout);
      myImpl.parent.flush();
    } catch (NotAuthorizedException e) {
      throw new WebDavException(MessageFormat.format("User {0} is probably not authorized to access {1}", getUsername(), myUrl.hostName), e);
    } catch (BadRequestException e) {
      throw new WebDavException(MessageFormat.format("Bad request when accessing {0}", myUrl.hostName), e);
    } catch (HttpException e) {
      if (e.getResult() == 423) {
        throw new WebDavException(MessageFormat.format("Document {0} at {1} seems to be already locked", myUrl.path, myUrl.hostName), e);
      } else {
        throw new WebDavException(MessageFormat.format("HTTP error {1} when accessing {0}", myUrl.hostName, e.getResult()), e);
      }
    } catch (ConflictException e) {
      throw new WebDavException(MessageFormat.format("Conflict when accessing {0}", myUrl.hostName), e);
    } catch (NotFoundException e) {
      throw new WebDavException(MessageFormat.format("Resource {0} is not found on {1}", myUrl.path, myUrl.hostName), e);
    } catch (RuntimeException e) {
      throw new WebDavException(MessageFormat.format("Something went wrong when locking {0}: {1}", myUrl.buildUrl(), e.getMessage()), e);
    } catch (IOException e) {
      throw new WebDavException(MessageFormat.format("Something went wrong when locking {0}: {1}", myUrl.buildUrl(), e.getMessage()), e);
    }
  }

  @Override
  public void unlock() throws WebDavException {
    if (!isLocked()) {
      return;
    }
    assertExists();
    try {
      myImpl.unlock();
      myImpl.parent.flush();
    } catch (NotAuthorizedException e) {
      throw new WebDavException(MessageFormat.format("User {0} is probably not authorized to access {1}", getUsername(), myUrl.hostName), e);
    } catch (BadRequestException e) {
      throw new WebDavException(MessageFormat.format("Bad request when accessing {0}", myUrl.hostName), e);
    } catch (HttpException e) {
      throw new WebDavException(MessageFormat.format("HTTP problems when accessing {0}", myUrl.hostName), e);
    } catch (ConflictException e) {
      throw new WebDavException(MessageFormat.format("Conflict when accessing {0}", myUrl.hostName), e);
    } catch (NotFoundException e) {
      throw new WebDavException(MessageFormat.format("Resource {0} is not found on {1}", myUrl.path, myUrl.hostName), e);
    } catch (IOException e) {
      throw new WebDavException(MessageFormat.format("Something went wrong when locking {0}: {1}", myUrl.buildUrl(), e.getMessage()), e);
    }
  }


  @Override
  public WebDavResource getParent() {
    if (myImpl != null) {
      return new MiltonResourceImpl(myUrl.buildParent(), myImpl.parent, myFactory);
    }
    return new MiltonResourceImpl(myUrl.buildParent(), myHost, myFactory);
  }

  @Override
  public WebDavUri getWebDavUri() {
    return myUrl;
  }

  @Override
  public String getUrl() {
    return myUrl == null ? myImpl.encodedUrl() : myUrl.buildUrl();
  }

  @Override
  public List<WebDavResource> getChildResources() throws WebDavException {
    assertExists();
    try {
      return Lists.transform(((Folder)myImpl).children(), new Function<Resource, WebDavResource>() {
        @Override
        public WebDavResource apply(Resource r) {
          return new MiltonResourceImpl(myUrl.buildChild(r.name), r, myFactory);
        }
      });
    } catch (NotAuthorizedException e) {
      throw new WebDavException(MessageFormat.format("User {0} is probably not authorized to access {1}", getUsername(), myUrl.hostName), e);
    } catch (BadRequestException e) {
      throw new WebDavException(MessageFormat.format("Bad request when accessing {0}", myUrl.hostName), e);
    } catch (IOException e) {
      throw new WebDavException(MessageFormat.format("I/O problems when accessing {0}", myUrl.hostName), e);
    } catch (HttpException e) {
      throw new WebDavException(MessageFormat.format("HTTP problems when accessing {0}", myUrl.hostName), e);
    }
  }

  @Override
  public String getAbsolutePath() {
    if (myImpl != null) {
      return myImpl.path().toPath();
    }
    return Path.path(myUrl.path).toPath();

  }
  @Override
  public String getName() {
    if (myImpl != null) {
      return myImpl.name;
    }
    return Path.path(myUrl.path).getName();
  }

  /**
   * Asks the server for the ETag which the resource carries right now.
   *
   * A request of its own rather than {@code myImpl.getEtag()}: the cached resource carries the
   * value from the last listing, and that is precisely not the question here.
   *
   * Returns null when the question cannot be answered. The callers handle "do not know"
   * explicitly, and an invented version would be worse than none.
   */
  private String fetchCurrentEtag() {
    try {
      List<PropFindResponse> responses =
          getHost().propFind(Path.path(myUrl.path), 0, Collections.singletonList(ETAG_PROPERTY));
      return (responses == null || responses.isEmpty()) ? null : responses.get(0).getEtag();
    } catch (Exception e) {
      // RuntimeException included: this is an extra question and must never be the reason why
      // opening or saving fails.
      GPLogger.log(e);
      return null;
    }
  }

  @Override
  public void write(byte[] byteArray) throws WebDavException {
    MiltonResourceImpl parent = (MiltonResourceImpl) getParent();
    if (!parent.exists()) {
      throw new WebDavException(MessageFormat.format("Folder {0} does not exist", parent.getName()));
    }
    assert parent.myImpl instanceof Folder;
    Folder parentFolder = (Folder) parent.myImpl;
    try {
      InputStream is = new BufferedInputStream(new ByteArrayInputStream(byteArray));
      if (myImpl != null && myImpl.getLockToken() != null) {
        // The lock token goes out in the If: header. IfMatchCheck carries exactly one string, so
        // a token and an ETag cannot be sent together; If-Match is needed precisely when no token
        // is held.
        parentFolder.upload(getName(), is, Long.valueOf(byteArray.length),
            "application/xml", new IfMatchCheck(myImpl.getLockToken(), false, true), null);
      } else {
        IfMatchDecision decision =
            IfMatchResolutionKt.resolveIfMatch(myEtagAtRead, this::fetchCurrentEtag);
        if (decision instanceof IfMatchDecision.Conflict) {
          throw new WebDavConflictException(MessageFormat.format(
              "File {0} has been changed by somebody else since it was read", myUrl.path));
        }
        if (decision instanceof IfMatchDecision.VersioningUnavailable) {
          // Refusing rather than writing blind. A fallback to an unconditional write would fire on
          // every save behind a compressing proxy or a CDN, and the protection would be off
          // without a sign of it.
          throw new WebDavVersioningUnavailableException(MessageFormat.format(
              "Only weak ETags are reported for {0}, so this write cannot be made conditional",
              myUrl.path));
        }
        if (decision instanceof IfMatchDecision.Send) {
          parentFolder.upload(getName(), is, Long.valueOf(byteArray.length), "application/xml",
              new IfMatchCheck(((IfMatchDecision.Send) decision).getEtag(), true, false), null);
        } else {
          // Nothing remembered: a first write, with no version to be conditional on.
          parentFolder.upload(getName(), is, Long.valueOf(byteArray.length), null);
        }
      }
      // Apache returns no ETag on PUT, so the new version has to be asked for. If that fails the
      // value stays null: writing unconditionally next time is better than remembering a version
      // against which every later comparison would fail.
      myEtagAtRead = fetchCurrentEtag();
    } catch (NotAuthorizedException e) {
      throw new WebDavException(MessageFormat.format("User {0} is probably not authorized to access {1}", getUsername(), myUrl.hostName), e);
    } catch (BadRequestException e) {
      throw new WebDavException(MessageFormat.format("Bad request when accessing {0}", myUrl.hostName), e);
    } catch (HttpException e) {
      // 412 is not a transport problem but the server's answer to If-Match: the file changed since
      // it was read. Milton maps it to GenericHttpException, since processResultCode only
      // special-cases 400/401/404/409, so the status has to be read off the exception.
      if (e.getResult() == 412) {
        throw new WebDavConflictException(MessageFormat.format(
            "File {0} has been changed by somebody else since it was read", myUrl.path), e);
      }
      throw new WebDavException(MessageFormat.format("HTTP problems when accessing {0}", myUrl.hostName), e);
    } catch (ConflictException e) {
      throw new WebDavException(MessageFormat.format("Conflict when accessing {0}", myUrl.hostName), e);
    } catch (NotFoundException e) {
      throw new WebDavException(MessageFormat.format("Resource {0} is not found on {1}", myUrl.path, myUrl.hostName), e);
    } catch (FileNotFoundException e) {
      throw new WebDavException(MessageFormat.format("I/O problems when uploading {0} to {1}", myUrl.path, myUrl.hostName), e);
    } catch (IOException e) {
      throw new WebDavException(MessageFormat.format("I/O problems when uploading {0} to {1}", myUrl.path, myUrl.hostName), e);
    }
  }

  @Override
  public InputStream getInputStream() throws WebDavException {
    assertExists();
    assert myImpl instanceof File;
    File file = (File) myImpl;
    ByteArrayOutputStream content = new ByteArrayOutputStream();
    try {
      // Remember the version the coming changes are based on, and deliberately before the
      // download. If the file changes in between, the remembered ETag is older than the content
      // that was read and saving reports a conflict which strictly speaking is not one. The other
      // order - ETag newer than content - would silently overwrite somebody else's change. Of the
      // two errors, the superfluous question is the harmless one.
      myEtagAtRead = fetchCurrentEtag();
      if (myEtagAtRead == null) {
        GPLogger.log(MessageFormat.format(
            "No ETag for {0}; the server cannot be asked to refuse a write over a concurrent"
                + " change", myUrl.path));
      }
      file.download(content, PROGRESS_LISTENER_STUB);
      return new ByteArrayInputStream(content.toByteArray());
    } catch (CancelledException e) {
      throw new WebDavException("File download has been canceled", e);
    } catch (HttpException e) {
      throw new WebDavException(MessageFormat.format("HTTP error {0} while downloading file", e.getResult()), e);
    }
  }

  @Override
  public boolean isWritable() {
    try {
      if (exists()) {
        return doCanLock() != CanLockStatus.LOCK_UNAVAILABLE;
      }
      WebDavResource parent = getParent();
      return parent.exists() && parent.isWritable();
    } catch (WebDavException e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public boolean canLock() throws WebDavException {
    return doCanLock() == CanLockStatus.LOCK_AVAILABLE;
  }

  enum CanLockStatus {
    LOCK_AVAILABLE, LOCK_UNSUPPORTED, LOCK_UNAVAILABLE
  }
  private CanLockStatus doCanLock() {
    assertExists();
    if (myImpl.getSupportedLock() == null) {
      return CanLockStatus.LOCK_UNSUPPORTED;
    }
    if (!myImpl.getSupportedLock().exclusive) {
      return CanLockStatus.LOCK_UNSUPPORTED;
    }
    List<String> lockOwners = getLockOwners();
    if (lockOwners.isEmpty() || lockOwners.equals(ImmutableList.of(getUsername()))) {
      return CanLockStatus.LOCK_AVAILABLE;
    } else {
      return CanLockStatus.LOCK_UNAVAILABLE;
    }
  }
  private String getUsername() {
    return myHost.user;
  }

  @Override
  public void delete() throws WebDavException {
    assertExists();
    try {
      myImpl.delete();
    } catch (NotAuthorizedException e) {
      throw new WebDavException(MessageFormat.format("User {0} is probably not authorized to access {1}", getUsername(), myUrl.hostName), e);
    } catch (BadRequestException e) {
      throw new WebDavException(MessageFormat.format("Bad request when deleting {0}", myUrl.hostName), e);
    } catch (HttpException e) {
      throw new WebDavException(MessageFormat.format("HTTP problems when deleting {0}", myUrl.hostName), e);
    } catch (ConflictException e) {
      throw new WebDavException(MessageFormat.format("Conflict when deleting {0}", myUrl.hostName), e);
    } catch (NotFoundException e) {
      throw new WebDavException(MessageFormat.format("Resource {0} is not found on {1}", myUrl.path, myUrl.hostName), e);
    } catch (IOException e) {
      throw new WebDavException(MessageFormat.format("I/O problems when deleting {0}", myUrl.hostName), e);
    }
  }
}
