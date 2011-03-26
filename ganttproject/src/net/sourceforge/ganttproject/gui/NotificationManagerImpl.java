/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev

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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.NotificationSlider.AnimationView;

public class NotificationManagerImpl implements NotificationManager {
    private final AnimationView myAnimationView;

    public NotificationManagerImpl(AnimationView animationView) {
        myAnimationView = animationView;
    }
    public void showNotification(NotificationChannel channel) {
        final NotificationSlider notification = new NotificationSlider(myAnimationView);
        Action[] actions = channel.getComponent().getActions();
        JPanel buttonPanel = new JPanel(new GridLayout(1, actions.length, 2, 0));
        for (final Action a : actions) {
            JButton button = new TestGanttRolloverButton(a);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    notification.hide();
                }
            });
            buttonPanel.add(button);
        }
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        JPanel result = new JPanel(new BorderLayout());

        JComponent content = channel.getComponent().getComponent();
        result.add(content, BorderLayout.CENTER);
        result.add(buttonPanel, BorderLayout.NORTH);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(channel.getColor().darker()),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        UIUtil.setBackgroundTree(content, channel.getColor());

        notification.setContents(result);
        notification.show();
    }

    JComponent getChannelButtons() {
        JPanel result = new JPanel(new GridLayout(1, 2, 3, 0));
        result.add(new TestGanttRolloverButton(new ShowChannelAction(NotificationChannel.RSS)));
        result.add(new TestGanttRolloverButton(new ShowChannelAction(NotificationChannel.ERROR)));
        return result;
    }

    private class ShowChannelAction extends GPAction {
        private final NotificationChannel myChannel;

        ShowChannelAction(NotificationChannel channel) {
            super("notification.channel." + channel.toString().toLowerCase() + ".label");
            myChannel = channel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showNotification(myChannel);
        }
    }

    @Override
    public void addNotification(NotificationChannel channel, String title, String body) {
        channel.getComponent().addNotification(title, body);
    }
}
