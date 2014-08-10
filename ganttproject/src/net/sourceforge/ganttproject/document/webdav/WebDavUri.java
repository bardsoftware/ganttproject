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

import io.milton.common.Path;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Encapsulates WebDAV resource location.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class WebDavUri {
  public final String hostUrl;
  public final String rootPath;
  public final String path;
  public final String hostName;
  public final int port;
  public final boolean isSecure;

  public WebDavUri(String fullUrl) {
    String tryHostUrl;
    String tryPath;
    int tryPort;
    boolean trySecure = false;
    try {
      URL url = new URL(fullUrl);
      tryHostUrl = url.getHost();
      trySecure = "https".equals(url.getProtocol().toLowerCase());
      tryPort = url.getPort();
      tryPath = url.getPath();
    } catch (MalformedURLException e) {
      tryHostUrl = fullUrl;
      tryPort = -1;
      tryPath = "";
    }
    this.hostName = "";
    this.hostUrl = tryHostUrl;
    this.port = tryPort;
    this.rootPath = "";
    this.path = tryPath;
    this.isSecure = trySecure;
  }

  public WebDavUri(String hostName, String hostUrl, String path) {
    assert !hostUrl.endsWith("/");
    assert path.isEmpty() || path.startsWith("/");
    this.hostName = hostName;

    String tryHostUrl;
    String tryRootPath;
    int tryPort;
    boolean trySecure = false;
    try {
      URL url = new URL(hostUrl);
      tryHostUrl = url.getHost();
      trySecure = "https".equals(url.getProtocol().toLowerCase());
      tryPort = url.getPort();
      tryRootPath = url.getPath();
    } catch (MalformedURLException e) {
      tryHostUrl = hostUrl;
      tryPort = 80;
      tryRootPath = "";
    }
    this.hostUrl = tryHostUrl;
    this.port = tryPort;
    this.rootPath = tryRootPath;
    this.path = path;
    this.isSecure = trySecure;
  }

  public String buildUrl() {
    return buildRootUrl() + path;
  }

  String buildRootUrl() {
    return (isSecure ? "https://" : "http://") + hostUrl + (port == -1 ? "" :  ":" + port) + rootPath;
  }
  public WebDavUri buildParent() {
    return new WebDavUri(hostName, buildRootUrl(), Path.path(path).getParent().toString());
  }

  public WebDavUri buildChild(String name) {
    return new WebDavUri(hostName, buildRootUrl(), Path.path(path).child(name).toString());
  }
}
