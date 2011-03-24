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

public class NotificationManagerImpl implements NotificationManager {
    public void showNotification(NotificationChannel channel, JComponent content, Action[] actions,
            NotificationSlider.AnimationView animationView) {
        final NotificationSlider notification = new NotificationSlider(animationView);
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
}
