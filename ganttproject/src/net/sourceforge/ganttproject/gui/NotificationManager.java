/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev

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

import java.util.Collection;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sourceforge.ganttproject.gui.NotificationComponent.AnimationView;
import net.sourceforge.ganttproject.util.BrowserControl;

public interface NotificationManager {
  /**
   * Sets the animation view of the manager to the given view.
   * @return the old view
   */
  AnimationView setAnimationView(AnimationView view);
  void addNotifications(NotificationChannel channel, Collection<NotificationItem> notifications);

  void showNotification(NotificationChannel channel);

  void hideNotification();

  HyperlinkListener DEFAULT_HYPERLINK_LISTENER = new HyperlinkListener() {
    @Override
    public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
      if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        BrowserControl.displayURL(hyperlinkEvent.getURL().toString());
      }
    }
  };
}
