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

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JWindow;

import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.Timeline.TimelineState;
import org.pushingpixels.trident.callback.TimelineCallback;

/**
 * Controls sliding animation of the notifier component.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class NotificationSlider {

    public static interface AnimationView {
        boolean isReady();

        void setImage(BufferedImage image);

        void setHeight(int height);

        void setComponent(JComponent component, Runnable onHide);

        void close();
    }

    private static final int ANIMATION_TIME_MS = 1000;

    private JComponent contents;
    private final AnimationView myHost;
    private BufferedImage myOffscreenImage;
    private Runnable myOnHide;

    public NotificationSlider(AnimationView host) {
        myHost = host;
    }

    public void setContents(JComponent contents, Runnable onHide) {
        myOnHide = onHide;
        this.contents = contents;
//        JWindow tempWindow = new JWindow();
//        tempWindow.setVisible(false);
//        tempWindow.getContentPane().add(contents);
//        tempWindow.pack();
//        tempWindow.getContentPane().removeAll();
//        myOffscreenImage = createOffscreenImage(contents);
//        myHost.setImage(myOffscreenImage);
    }

    public void show() {

//        Timeline timeline = new Timeline(myHost);
//        timeline.addPropertyToInterpolate("height", 0, myOffscreenImage.getHeight());
//        timeline.setDuration(ANIMATION_TIME_MS);
//        timeline.addCallback(new TimelineCallback() {
//            @Override
//            public void onTimelinePulse(float arg0, float arg1) {
//            }
//            @Override
//            public void onTimelineStateChanged(TimelineState from, TimelineState to, float arg2, float arg3) {
//                if (TimelineState.DONE == to) {
                    myHost.setComponent(contents, myOnHide);
//                }
//            }
//        });
//        timeline.play();
    }

    public void hide() {
        myHost.close();
    }

    public static BufferedImage createOffscreenImage(JComponent source) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration gfxConfig = ge.getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage offscreenImage = gfxConfig.createCompatibleImage(source.getWidth(), source.getHeight());
        Graphics2D offscreenGraphics = (Graphics2D) offscreenImage.getGraphics();
        offscreenGraphics.setColor(source.getBackground());
        offscreenGraphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        source.paint(offscreenGraphics);
        return offscreenImage;
    }
}
