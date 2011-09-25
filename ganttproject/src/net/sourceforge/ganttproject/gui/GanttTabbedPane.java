package net.sourceforge.ganttproject.gui;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.sourceforge.ganttproject.gui.NotificationSlider.AnimationView;

public class GanttTabbedPane extends JTabbedPane {

    private Map<Component, Object> myUserObjectsMap = new HashMap<Component, Object>();

    public GanttTabbedPane() {
        super();
    }

    public GanttTabbedPane(int tabPlacement) {
        super(tabPlacement);
    }

    public GanttTabbedPane(int tabPlacement, int tabLayoutPolicy) {
        super(tabPlacement, tabLayoutPolicy);

    }

    public void addTab(String title, Component component, Object userObject) {
        super.addTab(title, component);
        myUserObjectsMap.put(component, userObject);
    }

    public void addTab(String title, Icon icon, Component component,
            Object userObject) {
        super.addTab(title, icon, component);
        myUserObjectsMap.put(component, userObject);
    }

    public void addTab(String title, Icon icon, Component component,
            String tip, Object userObject) {
        super.addTab(title, icon, component, tip);
        myUserObjectsMap.put(component, userObject);
    }

    public Object getSelectedUserObject() {
        Object selectedComp = this.getSelectedComponent();
        return myUserObjectsMap.get(selectedComp);
    }

    private AnimationHostImpl myAnimationHost;

    public class AnimationHostImpl implements AnimationView {
        Composite myAlphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        private BufferedImage myImage;
        private JPopupMenu popup;

        @Override
        public boolean isReady() {
            return GanttTabbedPane.this.isShowing();
        }
        @Override
        public void setImage(BufferedImage image) {
            myImage = image;
        }

        @Override
        public void setHeight(int height) {
            GanttTabbedPane.this.repaint(new Rectangle(0, getHeight() - height, myImage.getWidth(), height));
        }

        @Override
        public void setComponent(JComponent component, final Runnable onHide) {
            myAnimationHost = null;
            GanttTabbedPane.this.repaint(new Rectangle(
                0, getHeight() - myImage.getHeight(), myImage.getWidth(), myImage.getHeight()));
            popup = new JPopupMenu();
            popup.add(component);
            popup.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
                }
                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
                }
                @Override
                public void popupMenuCanceled(PopupMenuEvent arg0) {
                    onHide.run();
                }
            });
            popup.show(GanttTabbedPane.this, 0, getHeight()-myImage.getHeight());
        }

        @Override
        public void close() {
            popup.setVisible(false);
        }

        void paint(Graphics g) {
            if (myImage == null) {
                return;
            }
            Composite was = ((Graphics2D)g).getComposite();
            ((Graphics2D)g).setComposite(myAlphaComposite);
            g.drawImage(myImage, 0, getHeight()-myImage.getHeight(), null);
            ((Graphics2D)g).setComposite(was);
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (myAnimationHost != null) {
            myAnimationHost.paint(g);
        }
    }

    public NotificationSlider.AnimationView getAnimationHost() {
        if (myAnimationHost == null) {
            myAnimationHost = new AnimationHostImpl();
        }
        return myAnimationHost;
    }
}
