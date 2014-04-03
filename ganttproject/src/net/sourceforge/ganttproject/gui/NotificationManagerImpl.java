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

import java.awt.GridLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sourceforge.ganttproject.action.ShowChannelAction;
import net.sourceforge.ganttproject.gui.NotificationComponent.AnimationView;

public class NotificationManagerImpl implements NotificationManager {
  private AnimationView myAnimationView;
  private NotificationChannel myFirstChannel;

  public NotificationManagerImpl(AnimationView animationView) {
    myAnimationView = animationView;
  }

  @Override
  public void showNotification(final NotificationChannel channel) {
    if (channel.getItems().isEmpty() && channel.getDefaultNotification() == null) {
      return;
    }
    if (!myAnimationView.isReady()) {
      if (myFirstChannel == null) {
        myFirstChannel = channel;
      }
      return;
    }
    if (myAnimationView.isVisible()) {
      return;
    }
    NotificationComponent nc = new NotificationComponent(channel, myAnimationView);
    channel.setVisible(true);
    nc.processItems();
    myAnimationView.setComponent(nc.getComponent(), channel.getButton(), new Runnable() {
      @Override
      public void run() {
        channel.getButton().setBackground(channel.getNormalColor());
        channel.setVisible(false);
      }
    });
  }

  public void showPending() {
    if (myFirstChannel != null) {
      showNotification(myFirstChannel);
    }
  }

  JComponent getChannelButtons() {
    final JPanel result = new JPanel(new GridLayout(1, 2, 3, 0));
    TestGanttRolloverButton rssButton = new TestGanttRolloverButton(
        new ShowChannelAction(this, NotificationChannel.RSS));

    NotificationChannel.RSS.setButton(rssButton);
    result.add(rssButton);

    TestGanttRolloverButton warningButton = new TestGanttRolloverButton(new ShowChannelAction(this, NotificationChannel.WARNING));
    result.add(warningButton);
    NotificationChannel.WARNING.setButton(warningButton);

    TestGanttRolloverButton errorButton = new TestGanttRolloverButton(new ShowChannelAction(this,
        NotificationChannel.ERROR));
    NotificationChannel.ERROR.setButton(errorButton);
    result.add(errorButton);


    result.addComponentListener(new ComponentListener() {
      @Override
      public void componentShown(ComponentEvent e) {
      }

      @Override
      public void componentResized(ComponentEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            showPending();
          }
        });
        result.removeComponentListener(this);
      }

      @Override
      public void componentMoved(ComponentEvent e) {
      }

      @Override
      public void componentHidden(ComponentEvent e) {
      }
    });

    return result;
  }

  @Override
  public void addNotifications(final NotificationChannel channel, Collection<NotificationItem> items) {
    channel.addNotifications(items);
    if (!channel.isVisible()) {
      boolean hasVisibleChannel = false;
      for (NotificationChannel ch : NotificationChannel.values()) {
        hasVisibleChannel |= ch.isVisible();
      }
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          channel.saveNormalColor();
          channel.getButton().setBackground(channel.getColor());
        }
      });
      if (!hasVisibleChannel) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            showNotification(channel);
          }
        });
      }
    }
  }

  @Override
  public void hideNotification() {
    myAnimationView.close();
  }

  @Override
  public AnimationView setAnimationView(AnimationView view) {
    AnimationView oldView = myAnimationView;
    myAnimationView = view;
    return oldView;
  }
}
