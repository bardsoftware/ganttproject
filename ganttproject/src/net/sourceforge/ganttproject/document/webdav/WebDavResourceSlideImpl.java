/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.document.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.webdav.lib.WebdavResource;
import org.apache.webdav.lib.methods.DepthSupport;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Implemenation of WebDavResource which uses Jakarta Slide library.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class WebDavResourceSlideImpl implements WebDavResource {
  private WebdavResource myImpl;
  private HttpURL myUrl;
  private String myUsername;
  private Boolean myExistance;
  private Boolean isCollection;
  private List<WebdavResource> myChildren;
  private final WebDavResourceSlideFactory myFactory;

  WebDavResourceSlideImpl(WebdavResource impl, WebDavResourceSlideFactory factory) {
    myImpl = impl;
    myUrl = impl.getHttpURL();
    myFactory = factory;
  }

  WebDavResourceSlideImpl(String urlString, String username, String password, WebDavResourceSlideFactory factory) throws WebDavException {
    try {
      myFactory = factory;
      myUsername = username;
      myUrl = urlString.toLowerCase().startsWith("https") ? new HttpsURL(urlString) : new HttpURL(urlString);
      if (username != null && password != null) {
        myUrl.setUserinfo(username, password);
        Credentials credentials = new UsernamePasswordCredentials(username, password);
        myImpl = new WebdavResource(myUrl, credentials, WebdavResource.NOACTION, 0);
      } else {
        myImpl = new WebdavResource(myUrl, WebdavResource.NOACTION, 0);
      }
      myImpl.setFollowRedirects(true);
    } catch (URIException e) {
      throw new WebDavException(MessageFormat.format("Failed to parse url {0}", urlString), e);
    } catch (IOException e) {
      throw new WebDavException(MessageFormat.format("Failed to access resource {0}", urlString), e);
    }
  }

  @Override
  public boolean exists() throws WebDavException {
    if (myExistance == null) {
      try {
        myImpl.setFollowRedirects(true);
        myImpl.setProperties(WebdavResource.BASIC, DepthSupport.DEPTH_0);
        myExistance = myImpl.exists();
        isCollection = myImpl.isCollection();
      } catch (HttpException e) {
        myExistance = false;
        if (e.getReasonCode() != 404) {
          throw new WebDavException(
              MessageFormat.format("HTTP problem when accessing {0} on {1}<br>Error code: {2}<br>Error message: {3}", getPath(), getHost(), e.getReasonCode(), e.getReason()), e);
        }
      } catch (IOException e) {
        throw new WebDavException(MessageFormat.format("I/O problem when accessing {0} on {1}", getPath(), getHost()), e);
      }
    }
    return myExistance;
  }

  private void assertExists() {
    try {
      if (!exists()) {
        throw new WebDavRuntimeException(MessageFormat.format("Resource {0} does not exist on {1}", getPath(), getHost()));
      }
    } catch (WebDavException e) {
      throw new WebDavRuntimeException(MessageFormat.format("Resource {0} does not exist on {1}", getPath(), getHost()), e);
    }
  }

  private void assertExistanceStatus() {
    if (myExistance == null) {
      throw new WebDavRuntimeException("exists() has not been called");
    }
  }
  private String getPath() {
    return myImpl.getPath();
  }

  private String getHost() {
    try {
      return myImpl.getHost();
    } catch (URIException e) {
      throw new WebDavRuntimeException(MessageFormat.format("Failed to extract host from url {0}", getUrl()), e);
    }
  }

  @Override
  public String getUrl() {
    return myUrl.toString();
  }


  public WebDavUri getWebDavUri() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public WebDavResource getParent() {
    HttpURL httpURL = myUrl;
    try {
      HttpURL parentUrl = httpURL instanceof HttpsURL ? new HttpsURL(httpURL.toString()) : new HttpURL(httpURL.toString());
      parentUrl.setPath(httpURL.getCurrentHierPath());
      return myFactory.createResource(parentUrl.toString(), httpURL.getUser(), httpURL.getPassword());
    } catch (URIException e) {
      throw new WebDavRuntimeException(e);
    } catch (WebDavException e) {
      throw new WebDavRuntimeException(e);
    }
  }

  @Override
  public boolean isCollection() throws WebDavException {
    assertExists();
    if (isCollection == null) {
      getChildren();
      isCollection = Boolean.valueOf(myImpl.isCollection());
    }
    return isCollection;
  }

  @Override
  public List<WebDavResource> getChildResources() throws WebDavException {
    return Lists.transform(getChildren(), new Function<WebdavResource, WebDavResource>() {
      @Override
      public WebDavResource apply(WebdavResource input) {
        return myFactory.createResource(input);
      }
    });
  }

  private List<WebdavResource> getChildren() throws WebDavException {
    if (myChildren == null) {
      WebdavResource[] children = null;
      try {
        children = myImpl.listWebdavResources();
      } catch (HttpException e) {
        if (e.getReasonCode() >= 400 && e.getReasonCode() < 500) {
          children = null;
        } else {
          throw new WebDavException(MessageFormat.format("HTTP problem when reading child resources of {0} on {1}. Error code={2}", getPath(), getHost(), e.getReasonCode()), e);
        }
      } catch (IOException e) {
        throw new WebDavException(MessageFormat.format("I/O problem when reading child resources of {0} on {1}", getPath(), getHost()), e);
      }
      myChildren = children == null  ? Collections.<WebdavResource>emptyList() : Arrays.asList(children);
    }
    return myChildren;
  }

  @Override
  public boolean isWritable() {
    assertExists();
    return !isLocked();
  }

  @Override
  public List<String> getLockOwners() {
    assertExists();
    Enumeration ownersEnum = myImpl.getActiveLockOwners();
    return ownersEnum == null ? Collections.emptyList() : Collections.list(myImpl.getActiveLockOwners());
  }

  @Override
  public boolean isLocked() {
    return !getLockOwners().isEmpty();
  }

  @Override
  public boolean canLock() throws WebDavException {
    assertExists();
    List<String> lockOwners = getLockOwners();
    return lockOwners.isEmpty() || lockOwners.equals(ImmutableList.of(myUsername));
  }

  @Override
  public void lock(int timeout) throws WebDavException {
    assertExists();
    try {
      myImpl.lockMethod(myUsername, timeout);
    } catch (HttpException e) {
      throw new WebDavException(MessageFormat.format("HTTP problem when locking resource {0} on {1}", getPath(), getHost()), e);
    } catch (IOException e) {
      throw new WebDavException(MessageFormat.format("I/O problem when locking resource {0} on {1}", getPath(), getHost()), e);
    }
  }

  @Override
  public void unlock() throws WebDavException {
    assertExists();
    try {
      myImpl.unlockMethod();
    } catch (HttpException e) {
      throw new WebDavException(MessageFormat.format("HTTP problem when unlocking resource {0} on {1}", getPath(), getHost()), e);
    } catch (IOException e) {
      throw new WebDavException(MessageFormat.format("I/O problem when unlocking resource {0} on {1}", getPath(), getHost()), e);
    }
  }

  @Override
  public String getName() {
    return myImpl.getName();
  }

  @Override
  public void write(byte[] byteArray) throws WebDavException {
    assertExistanceStatus();
    try {
      myImpl.putMethod(byteArray);
      myExistance = null;
      exists();
    } catch (HttpException e) {
      throw new WebDavException(MessageFormat.format("HTTP problem when writing to {0} on {1}", getPath(), getHost()), e);
    } catch (IOException e) {
      throw new WebDavException(MessageFormat.format("I/O problem when writing to {0} on {1}", getPath(), getHost()), e);
    }
  }

  @Override
  public InputStream getInputStream() throws WebDavException {
    assertExists();
    try {
      return myImpl.getMethodData();
    } catch (HttpException e) {
      throw new WebDavException(MessageFormat.format("HTTP problem when reading from {0} on {1}", getPath(), getHost()), e);
    } catch (IOException e) {
      throw new WebDavException(MessageFormat.format("I/O problem when reading from on {1}", getPath(), getHost()), e);
    }
  }
}
