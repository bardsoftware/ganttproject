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

import javax.swing.AbstractListModel;

import org.apache.webdav.lib.WebdavResource;

/**
 * Model of WebDAV collection contents.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class FilesTableModel extends AbstractListModel<WebdavResource> {

  private WebdavResource myCollection;
  private WebdavResource[] myChildResources;

  void setCollection(WebdavResource collection) throws IOException {
    myCollection = null;
    if (myChildResources != null) {
      fireIntervalRemoved(this, 0, myChildResources.length);
    }
    myCollection = collection;
    myChildResources = myCollection.getChildResources().listResources();
    fireIntervalAdded(this, 0, myChildResources.length);
  }

  @Override
  public int getSize() {
    return myCollection == null ? 0 : myChildResources.length;
  }

  @Override
  public WebdavResource getElementAt(int index) {
    return myChildResources[index];
  }
}
