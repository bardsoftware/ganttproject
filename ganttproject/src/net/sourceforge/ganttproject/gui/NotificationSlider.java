/**
 * Based on the code snippet from 'Swing Hacks' book.
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
        JWindow tempWindow = new JWindow();
        tempWindow.setVisible(false);
        tempWindow.getContentPane().add(contents);
        tempWindow.pack();
        tempWindow.getContentPane().removeAll();
        myOffscreenImage = createOffscreenImage(contents);
        myHost.setImage(myOffscreenImage);
    }

    public void show() {
        Timeline timeline = new Timeline(myHost);
        timeline.addPropertyToInterpolate("height", 0, myOffscreenImage.getHeight());
        timeline.setDuration(ANIMATION_TIME_MS);
        timeline.addCallback(new TimelineCallback() {
            public void onTimelinePulse(float arg0, float arg1) {
            }
            public void onTimelineStateChanged(TimelineState from, TimelineState to, float arg2, float arg3) {
                if (TimelineState.DONE == to) {
                    myHost.setComponent(contents, myOnHide);
                }
            }
        });
        timeline.play();
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
