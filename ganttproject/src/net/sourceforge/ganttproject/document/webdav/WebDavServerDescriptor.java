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

import com.google.common.base.Objects;

/**
 * Encapsulates server access information.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class WebDavServerDescriptor {
  public String name;
  private WebDavUri rootUri;
  public String username;
  String password = "";
  boolean savePassword = false;

  WebDavServerDescriptor() {
  }

  WebDavServerDescriptor(String name, String rootUrl, String username) {
    this.name = name;
    setRootUrl(rootUrl);
    this.username = username;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WebDavServerDescriptor == false) {
      return false;
    }
    WebDavServerDescriptor that = (WebDavServerDescriptor) obj;
    return Objects.equal(this.getRootUrl(), that.getRootUrl());
  }

  public String getUsername() {
    return this.username;
  }

  public String getPassword() {
    return this.password;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.getRootUrl());
  }

  public String getRootUrl() {
    return rootUri == null ? null : rootUri.buildRootUrl();
  }

  public void setRootUrl(String rootUrl) {
    while (rootUrl.endsWith("/")) {
      rootUrl = rootUrl.substring(0, rootUrl.length() - 1);
    }
    this.rootUri = new WebDavUri(this.name, rootUrl, "");
  }
}
