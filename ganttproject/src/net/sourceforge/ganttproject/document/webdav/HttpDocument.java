/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 GanttProject Team

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
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.document.AbstractURLDocument;
import net.sourceforge.ganttproject.document.Document;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.webdav.lib.WebdavResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * This class implements the interface Document for file access on HTTP-servers
 * and WebDAV-enabled-servers.
 *
 * @author Michael Haeusler (michael at akatose.de)
 */
public class HttpDocument extends AbstractURLDocument {

  private String url;

  private String lastError;

  private final HttpURL httpURL;

  private final WebdavResource webdavResource;

  private boolean locked = false;

  private boolean malformedURL = false;

  private final String myUsername;

  private final String myPassword;

  private final int myTimeout;

//  private static int lockDAVMinutes = 240;

  public HttpDocument(String url, String username, String password) throws IOException {
    this(url, username, password, -1);
  }

  public HttpDocument(String url, String username, String password, int lockTimeout) throws IOException {
    webdavResource = WebDavStorageImpl.createResource(url, username, password);
    this.httpURL = webdavResource.getHttpURL();
    this.url = url;
    myUsername = username;
    myPassword = password;
    myTimeout = lockTimeout;
  }

  WebdavResource getWebdavResource() {
    return webdavResource;
  }

  @Override
  public String getFileName() {
    // TODO return filename instead of URL?
    String filenName = httpURL.toString();
    return (filenName != null ? filenName : url);
  }

  @Override
  public boolean canRead() {
    WebdavResource res = getWebdavResource();
    return (null == res ? false : (res.exists() && !res.isCollection()));
  }

  @Override
  public IStatus canWrite() {
    WebdavResource res = getWebdavResource();
    if (null == res) {
      return new Status(IStatus.ERROR, Document.PLUGIN_ID, Document.ErrorCode.GENERIC_NETWORK_ERROR.ordinal(),
          lastError, null);
    }
    try {
      res.setProperties(0);
    } catch (HttpException e) {
      if (404 != e.getReasonCode()) {
        return new Status(IStatus.ERROR, Document.PLUGIN_ID, Document.ErrorCode.GENERIC_NETWORK_ERROR.ordinal(),
            e.getMessage(), e);
      }
    } catch (IOException e) {
      return new Status(IStatus.ERROR, Document.PLUGIN_ID, Document.ErrorCode.GENERIC_NETWORK_ERROR.ordinal(),
          e.getMessage(), e);
    }

    if (res.exists()) {
      return (res.isCollection()) ? new Status(IStatus.ERROR, Document.PLUGIN_ID,
          Document.ErrorCode.IS_DIRECTORY.ordinal(), res.getPath(), null) : Status.OK_STATUS;
    }

    try {
      HttpURL parentUrl = httpURL instanceof HttpsURL ? new HttpsURL(httpURL.toString()) : new HttpURL(httpURL.toString());
      parentUrl.setPath(httpURL.getCurrentHierPath());
      WebdavResource parentRes = WebDavStorageImpl.createResource(parentUrl.toString(), myUsername, myPassword);
      parentRes.listWebdavResources();
      if (!parentRes.isCollection()) {
        return new Status(IStatus.ERROR, Document.PLUGIN_ID, Document.ErrorCode.PARENT_IS_NOT_DIRECTORY.ordinal(),
            parentRes.getPath(), null);
      }
      return Status.OK_STATUS;
    } catch (HttpException e) {
      return new Status(IStatus.ERROR, Document.PLUGIN_ID, Document.ErrorCode.GENERIC_NETWORK_ERROR.ordinal(),
          (e.getReason() == null ? "Code: " + getHTTPError(e.getReasonCode()) : e.getReason()), e);
    } catch (Exception e) {
      return new Status(IStatus.ERROR, Document.PLUGIN_ID, Document.ErrorCode.GENERIC_NETWORK_ERROR.ordinal(),
          e.getMessage(), e);
    }
  }

  @Override
  public boolean isValidForMRU() {
    return (!malformedURL);
  }

  @Override
  public boolean acquireLock() {
    if (locked || myTimeout < 0) {
      return true;
    }
    if (null == getWebdavResource()) {
      return false;
    }
    try {
      locked = getWebdavResource().lockMethod(getUsername(), myTimeout * 60);
      return locked;
    } catch (HttpException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
    } catch (IOException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
    }
    return false;
  }

  @Override
  public void releaseLock() {
    if (null == getWebdavResource()) {
      return;
    }
    try {
      locked = false;
      if (!getWebdavResource().isLocked()) {
        return;
      }
      getWebdavResource().unlockMethod();
    } catch (HttpException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
    } catch (IOException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
    }
  }

  @Override
  public InputStream getInputStream() throws IOException {
    if (null == getWebdavResource())
      throw new IOException(lastError);
    try {
      return getWebdavResource().getMethodData();
    } catch (HttpException e) {
      throw new IOException(e.getMessage() + "(" + e.getReasonCode() + ")");
    } catch (IOException e) {
      throw new IOException(HttpDocument.getHTTPError(getWebdavResource().getStatusCode()) + "\n" + e.getMessage(), e);
    }
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    if (null == getWebdavResource()) {
      throw new IOException(lastError);
    }
    return new HttpDocumentOutputStream(this);
  }

  @Override
  public String getPath() {
    return getFileName();
  }

  @Override
  public String getUsername() {
    return myUsername;
  }

  @Override
  public String getPassword() {
    return myPassword;
  }

  @Override
  public String getLastError() {
    return lastError;
  }

//  public static void setLockDAVMinutes(int i) {
//    // FIXME should not be static, as each derived object should have its own
//    // setting
//    lockDAVMinutes = i;
//  }

  @Override
  public void write() throws IOException {
    // TODO Auto-generated method stub
  }

  @Override
  public URI getURI() {
    try {
      return new URI(url);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  public static String getHTTPError(int code) {
    // TODO Use language dependent texts
    switch (code) {
    case 401:
      return "Unauthorized (401)";
    default:
      return "<unspecified> (" + code + ")";
    }
  }
}
