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
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JWindow;

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

    private class AnimationHostImpl implements AnimationView {
        Composite myAlphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        private BufferedImage myImage;

        @Override
        public void setImage(BufferedImage image) {
            myImage = image;
        }

        @Override
        public void update(int height) {
            GanttTabbedPane.this.repaint(new Rectangle(0, getHeight() - height, myImage.getWidth(), height));
        }

        @Override
        public void setComponent(JComponent component) {
            myAnimationHost = null;
            GanttTabbedPane.this.repaint(new Rectangle(0, getHeight() - myImage.getHeight(), myImage.getWidth(), myImage.getHeight()));
            JPopupMenu popup = new JPopupMenu();
            popup.add(component);
            popup.show(GanttTabbedPane.this, 0, getHeight()-myImage.getHeight());
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
