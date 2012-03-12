/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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
package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import net.sourceforge.ganttproject.gui.NotificationChannel;
import net.sourceforge.ganttproject.gui.NotificationItem;
import net.sourceforge.ganttproject.gui.NotificationManager;

public class ShowChannelAction extends GPAction implements NotificationChannel.Listener {
  private final NotificationChannel myChannel;
  private final NotificationManager myManager;

  public ShowChannelAction(NotificationManager manager, NotificationChannel channel) {
    super("notification.channel." + channel.toString().toLowerCase() + ".label");
    myChannel = channel;
    myManager = manager;
    myChannel.addListener(this);
    updateState();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    myManager.showNotification(myChannel);
  }

  @Override
  protected String getLocalizedName() {
    int unreadCount = myChannel == null ? 0 : myChannel.getUnreadCount();
    String channelName = super.getLocalizedName();
    return unreadCount == 0 ? MessageFormat.format(getI18n("notification.channel.clearformat"), channelName)
        : MessageFormat.format(getI18n("notification.channel.unreadformat"), channelName, unreadCount);
  }

  private void updateState() {
    if (myChannel.isEmpty() && myChannel.getDefaultNotification() == null) {
      setEnabled(false);
    } else {
      setEnabled(true);
    }
    updateName();
  }

  @Override
  public void notificationAdded() {
    updateState();
  }

  @Override
  public void notificationRead(NotificationItem item) {
    updateState();
  }

  @Override
  public void channelCleared() {
    updateState();
  }
}
