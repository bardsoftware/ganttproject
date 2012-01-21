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
package net.sourceforge.ganttproject.gui.window;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import net.sourceforge.ganttproject.gui.GanttStatusBar;
import net.sourceforge.ganttproject.gui.GanttTabbedPane;
import net.sourceforge.ganttproject.gui.NotificationSlider;
import net.sourceforge.ganttproject.gui.NotificationSlider.AnimationView;

/**
 * Builds a main frame's content pane and creates an animation host for the
 * notification slider.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class ContentPaneBuilder {
    private final GanttTabbedPane myTabbedPane;
    private final GanttStatusBar myStatusBar;
    private final JToolBar myToolBar;
    private final AnimationHostImpl myAnimationHost = new AnimationHostImpl();

    public ContentPaneBuilder(JToolBar toolBar, GanttTabbedPane tabbedPane, GanttStatusBar statusBar) {
        myTabbedPane = tabbedPane;
        myStatusBar = statusBar;
        myToolBar = toolBar;
    }

    public void build(Container contentPane, JLayeredPane layeredPane) {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(myToolBar, BorderLayout.NORTH);
        contentPanel.add(myTabbedPane, BorderLayout.CENTER);
        contentPanel.add(myStatusBar, BorderLayout.SOUTH);
        contentPane.add(contentPanel);
        myAnimationHost.setLayeredPane(layeredPane);
    }

    public class AnimationHostImpl implements AnimationView {
        Composite myAlphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        private BufferedImage myImage;
        private JLayeredPane myLayeredPane;
        private JLabel myLabel;
        private JComponent myComponent;

        private void setLayeredPane(JLayeredPane layeredPane) {
            myLayeredPane = layeredPane;
        }

        @Override
        public boolean isReady() {
            return myLayeredPane != null && myLayeredPane.isShowing();
        }

        @Override
        public void setImage(BufferedImage image) {
            myImage = image;
            myLabel = new JLabel(new ImageIcon(image)) {
                @Override
                public void paint(Graphics g) {
                    if (myImage == null) {
                        return;
                    }
                    Composite was = ((Graphics2D)g).getComposite();
                    ((Graphics2D)g).setComposite(myAlphaComposite);
                    g.drawImage(myImage, 0, 0, null);
                    ((Graphics2D)g).setComposite(was);
                }
            };
            myLayeredPane.add(myLabel, JLayeredPane.POPUP_LAYER);
            myLabel.setBounds(0, getTopX(), image.getWidth(), 0);
        }

        @Override
        public void setHeight(int height) {
            myLabel.setBounds(0, getTopX() - height, myImage.getWidth(), height);
        }

        @Override
        public void setComponent(JComponent component, final Runnable onHide) {
            myLayeredPane.remove(myLabel);
            myLayeredPane.add(component);
            myComponent = component;
            component.setBounds(0, getTopX() - component.getHeight(), component.getWidth(), component.getHeight());
        }

        @Override
        public void close() {
            myLayeredPane.remove(myComponent);
            myLayeredPane.repaint();
        }

        private int getTopX() {
            return myLayeredPane.getSize().height - myStatusBar.getHeight();
        }
    }

    public NotificationSlider.AnimationView getAnimationHost() {
        return myAnimationHost;
    }
}
