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
class WebDavServerDescriptor {
  public String name;
  public String rootUrl;
  public String username;
  String password = "";
  boolean savePassword = false;

  WebDavServerDescriptor() {
  }

  WebDavServerDescriptor(String name, String rootUrl, String username) {
    this.name = name;
    this.rootUrl = rootUrl;
    this.username = username;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WebDavServerDescriptor == false) {
      return false;
    }
    WebDavServerDescriptor that = (WebDavServerDescriptor) obj;
    return Objects.equal(this.rootUrl, that.rootUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.rootUrl);
  }
}
