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
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkListener;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.NotificationSlider.AnimationView;

import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.Timeline.RepeatBehavior;
import org.pushingpixels.trident.Timeline.TimelineState;
import org.pushingpixels.trident.callback.TimelineCallback;

public class NotificationManagerImpl implements NotificationManager {
    private final AnimationView myAnimationView;
    private NotificationChannel myFirstChannel;

    public NotificationManagerImpl(AnimationView animationView) {
        myAnimationView = animationView;
    }

    @Override
    public void showNotification(final NotificationChannel channel) {
        if (!myAnimationView.isReady()) {
            if (myFirstChannel == null) {
                myFirstChannel = channel;
            }
            return;
        }
        NotificationComponent nc = new NotificationComponent(channel);
        final NotificationSlider notification = new NotificationSlider(myAnimationView);
        Action[] actions = nc.getActions();
        JPanel buttonPanel = new JPanel(new GridLayout(1, actions.length, 2, 0));
        for (final Action a : actions) {
            JButton button = new TestGanttRolloverButton(a);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    notification.hide();
                    channel.setVisible(false);
                }
            });
            buttonPanel.add(button);
        }
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        JPanel result = new JPanel(new BorderLayout());

        JComponent content = nc.getComponent();
        result.add(content, BorderLayout.CENTER);
        result.add(buttonPanel, BorderLayout.NORTH);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        channel.setVisible(true);
        notification.setContents(result, new Runnable() {
            @Override
            public void run() {
                System.err.println("on hide!");
                channel.getButton().setBackground(channel.getNormalColor());
            }
        });
        notification.show();
    }

    public void showPending() {
        if (myFirstChannel != null) {
            showNotification(myFirstChannel);
        }
    }
    JComponent getChannelButtons() {
        JPanel result = new JPanel(new GridLayout(1, 2, 3, 0));
        TestGanttRolloverButton rssButton = new TestGanttRolloverButton(new ShowChannelAction(NotificationChannel.RSS));

        NotificationChannel.RSS.setButton(rssButton);
        result.add(rssButton);

        TestGanttRolloverButton errorButton =
            new TestGanttRolloverButton(new ShowChannelAction(NotificationChannel.ERROR));
        NotificationChannel.ERROR.setButton(errorButton);
        result.add(errorButton);
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
            if (myChannel.isPulsing()) {
                myChannel.getButton().setBackground(myChannel.getNormalColor());
                myChannel.setPulsing(false);
            }
        }
    }

    @Override
    public void addNotification(NotificationChannel channel, String title, String body) {
        addNotification(channel, title, body, DEFAULT_HYPERLINK_LISTENER);
    }
    @Override
    public void addNotification(
            final NotificationChannel channel, String title, String body, HyperlinkListener hyperlinkListener) {
        channel.addNotification(new NotificationItem(title, body, hyperlinkListener));
        System.err.println("added notification to channel=" + channel);
        if (!channel.isVisible()) {
            for (NotificationChannel ch : NotificationChannel.values()) {
                if (ch.isVisible()) {
                    System.err.println("channel=" + ch + " is visible. running pulsing animation");
                    runPulsingAnimation(channel);
                    return;
                }
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    System.err.println("show notification channel=" + channel);
                    channel.saveNormalColor();
                    channel.getButton().setBackground(channel.getColor());
                    showNotification(channel);
                }
            });
        }

    }

    private static void runPulsingAnimation(final NotificationChannel channel) {
        channel.setPulsing(true);
        channel.saveNormalColor();
        Timeline t = createTimeline(channel);
        t.addCallback(new TimelineCallback() {
            @Override
            public void onTimelinePulse(float arg0, float arg1) {
            }
            @Override
            public void onTimelineStateChanged(TimelineState arg0, TimelineState newState, float arg2, float arg3) {
                if (newState == TimelineState.DONE) {
                    createTimeline(channel).play();
                }
            }
        });
        t.playLoop(6, RepeatBehavior.REVERSE);
    }

    private static Timeline createTimeline(NotificationChannel channel) {
        Timeline t = new Timeline(channel.getButton());
        t.addPropertyToInterpolate("background", channel.getNormalColor(), channel.getColor());
        t.setDuration(3000);
        return t;
    }
}
