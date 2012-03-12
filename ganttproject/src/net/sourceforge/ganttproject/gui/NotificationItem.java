/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.gui;

import javax.swing.event.HyperlinkListener;

public class NotificationItem {
  final String myTitle;
  final String myBody;
  final HyperlinkListener myHyperlinkListener;
  boolean isRead;

  public NotificationItem(String title, String body, HyperlinkListener hyperlinkListener) {
    myTitle = title == null ? "" : title;
    myBody = body == null ? "" : body;
    myHyperlinkListener = hyperlinkListener;
  }

  public boolean isRead() {
    return isRead;
  }

  void setRead(boolean b) {
    isRead = b;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof NotificationItem) {
      NotificationItem that = (NotificationItem) obj;
      return myTitle.equals(that.myTitle) && myBody.equals(that.myBody);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return myBody.hashCode();
  }
}
