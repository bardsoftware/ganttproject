/**
 * Based on the code snippet from 'Swing Hacks' book.
 */
package net.sourceforge.ganttproject.gui;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.Timer;

public class NotificationSlider {

    public static interface AnimationView {
        boolean isReady();

        void setImage(BufferedImage image);

        void update(int height);

        void setComponent(JComponent component, Runnable onHide);

        void close();
    }

    protected static final int ANIMATION_TIME = 500;
    protected static final float ANIMATION_TIME_F = (float) ANIMATION_TIME;
    protected static final int ANIMATION_DELAY = 50;

    JComponent contents;
    Timer animationTimer;
    long animationStart;
    private AnimationView myHost;
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
        ActionListener animationLogic = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                long elapsed = System.currentTimeMillis() - animationStart;
                if (elapsed > ANIMATION_TIME) {
                    myHost.setComponent(contents, myOnHide);
                    animationTimer.stop();
                    animationTimer = null;
                } else {
                    // calculate % done
                    float progress = (float) elapsed / ANIMATION_TIME_F;
                    // get height to show
                    int animatingHeight = (int) (progress * myOffscreenImage.getHeight());
                    animatingHeight = Math.max(animatingHeight, 1);
                    myHost.update(animatingHeight);
                }
            }
        };
        animationTimer = new Timer(ANIMATION_DELAY, animationLogic);
        animationStart = System.currentTimeMillis();
        animationTimer.start();
    }

    public void hide() {
        // TODO Auto-generated method stub

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
