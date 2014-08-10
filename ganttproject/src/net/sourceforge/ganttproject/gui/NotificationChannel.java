/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JComponent;

import com.google.common.base.Preconditions;

public enum NotificationChannel {
  RSS(Color.GREEN.brighter()), ERROR(UIUtil.ERROR_BACKGROUND), WARNING(Color.YELLOW);

  public interface Listener {
    void notificationAdded();

    void notificationRead(NotificationItem item);

    void channelCleared();
  }

  private final Color myColor;
  private boolean isVisible;
  private final List<NotificationItem> myItems = new ArrayList<NotificationItem>();
  private JComponent myButton;
  private boolean isPulsing;
  private Color myNormalColor;
  private NotificationItem myDefaultNotification;
  private final List<Listener> myListeners = new CopyOnWriteArrayList<Listener>();

  NotificationChannel(Color color) {
    myColor = color;
  }

  Color getColor() {
    return myColor;
  }

  boolean isVisible() {
    return isVisible;
  }

  void setVisible(boolean b) {
    isVisible = b;
  }

  void addNotifications(Collection<NotificationItem> items) {
    for (NotificationItem item : items) {
      Preconditions.checkNotNull(item);
    }
    myItems.addAll(items);
    for (Listener l : myListeners) {
      l.notificationAdded();
    }
  }

  List<NotificationItem> getItems() {
    return myItems;
  }

  void setButton(JComponent button) {
    myButton = button;
  }

  JComponent getButton() {
    return myButton;
  }

  void setPulsing(boolean b) {
    isPulsing = b;
  }

  boolean isPulsing() {
    return isPulsing;
  }

  void saveNormalColor() {
    if (myNormalColor == null) {
      myNormalColor = myButton.getBackground();
    }
  }

  Color getNormalColor() {
    return myNormalColor;
  }

  public void setDefaultNotification(NotificationItem defaultNotification) {
    myDefaultNotification = defaultNotification;
    for (Listener l : myListeners) {
      l.notificationAdded();
    }
  }

  public NotificationItem getDefaultNotification() {
    return myDefaultNotification;
  }

  public void addListener(Listener listener) {
    myListeners.add(listener);
  }

  public int getUnreadCount() {
    int result = 0;
    for (NotificationItem item : myItems) {
      if (item != null && !item.isRead()) {
        result++;
      }
    }
    return result;
  }

  public void setRead(int position) {
    assert position >= 0 && position < myItems.size() : "Attempt to mark read item#" + position + ". I have "
        + myItems.size() + " items";
    NotificationItem item = myItems.get(position);
    item.setRead(true);
    for (Listener l : myListeners) {
      l.notificationRead(item);
    }
  }

  public void clear() {
    myItems.clear();
    for (Listener l : myListeners) {
      l.channelCleared();
    }
  }

  /** @return true if no more items are available in the channel */
  public boolean isEmpty() {
    return getItems().isEmpty();
  }
}
