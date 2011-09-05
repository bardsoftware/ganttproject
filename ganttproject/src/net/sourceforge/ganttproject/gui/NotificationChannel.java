/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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
import java.util.List;

import javax.swing.JButton;

public enum NotificationChannel {
    RSS(Color.YELLOW.brighter()), ERROR(new Color(255, 191, 207));

   public interface Listener {
        void notificationAdded();

        void notificationRead(NotificationItem item);

        void channelCleared();
    }

    private final Color myColor;
    private boolean isVisible;
    private final List<NotificationItem> myItems = new ArrayList<NotificationItem>();
    private JButton myButton;
    private boolean isPulsing;
    private Color myNormalColor;
    private NotificationItem myDefaultNotification;
    private final List<Listener> myListeners = new ArrayList<Listener>();

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

    void addNotification(NotificationItem item) {
        myItems.add(item);
        for (Listener l : myListeners) {
            l.notificationAdded();
        }
    }

    List<NotificationItem> getItems() {
        return myItems;
    }

    void setButton(JButton button) {
        myButton = button;
    }

    JButton getButton() {
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
            if (!item.isRead()) {
                result++;
            }
        }
        return result;
    }

    public void setRead(int position) {
        assert position >= 0 && position < myItems.size() : "Attempt to mark read item#" + position + ". I have " + myItems.size() + " items";
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
