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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractListModel;

import net.sourceforge.ganttproject.document.webdav.WebDavResource.WebDavException;

/**
 * Model of WebDAV collection contents.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class FilesTableModel extends AbstractListModel {

  private WebDavResource myCollection;
  private List<WebDavResource> myChildResources;

  WebDavResource getCollection() {
    return myCollection;
  }

  void setCollection(WebDavResource collection, List<WebDavResource> children) {
    myCollection = null;
    if (myChildResources != null) {
      fireIntervalRemoved(this, 0, myChildResources.size());
    }
    myCollection = collection;
    myChildResources = children;

    Collections.sort(myChildResources, new Comparator<WebDavResource>() {
        @Override
        public int compare(WebDavResource o1, WebDavResource o2) {
          try {
            int folder1 = o1.isCollection() ? 1 : 0;
            int folder2 = o2.isCollection() ? 1 : 0;
            return (folder1 - folder2 == 0) ? o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase()) : (folder2 - folder1);
          } catch (WebDavException e) {
            throw new WebDavResource.WebDavRuntimeException(e);
          }
        }
      });
    fireIntervalAdded(this, 0, myChildResources.size());
  }

  @Override
  public int getSize() {
    return myCollection == null ? 0 : myChildResources.size();
  }

  @Override
  public WebDavResource getElementAt(int index) {
    return myChildResources.get(index);
  }
}
