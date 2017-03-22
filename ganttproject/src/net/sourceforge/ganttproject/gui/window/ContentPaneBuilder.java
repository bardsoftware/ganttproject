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

import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;
import net.sourceforge.ganttproject.gui.GanttStatusBar;
import net.sourceforge.ganttproject.gui.GanttTabbedPane;
import net.sourceforge.ganttproject.gui.NotificationComponent;
import net.sourceforge.ganttproject.gui.NotificationComponent.AnimationView;

import javax.swing.*;
import java.awt.*;

/**
 * Builds a main frame's content pane and creates an animation host for the
 * notification slider.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class ContentPaneBuilder {
  private final GanttTabbedPane myTabbedPane;
  private final GanttStatusBar myStatusBar;
  private final AnimationHostImpl myAnimationHost = new AnimationHostImpl();

  public ContentPaneBuilder(GanttTabbedPane tabbedPane, GanttStatusBar statusBar) {
    myTabbedPane = tabbedPane;
    myStatusBar = statusBar;
  }

  public void build(Component toolbar, Container contentPane) {
    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.add(toolbar, BorderLayout.NORTH);
    contentPanel.add(myTabbedPane, BorderLayout.CENTER);
    contentPanel.add(myStatusBar, BorderLayout.SOUTH);
    contentPane.add(contentPanel);
  }

  public class AnimationHostImpl implements AnimationView {
    private BalloonTip myBalloon;
    private Runnable myOnHide;

    @Override
    public boolean isReady() {
      return myStatusBar.isShowing();
    }

    @Override
    public void setComponent(JComponent component, JComponent owner, final Runnable onHide) {
      myBalloon = new BalloonTip(owner, component, new EdgedBalloonStyle(Color.WHITE, Color.BLACK),
          BalloonTip.Orientation.LEFT_ABOVE, BalloonTip.AttachLocation.ALIGNED, 30, 10, false);
      myOnHide = onHide;
      myBalloon.setVisible(true);
    }

    @Override
    public void close() {
      myBalloon.setVisible(false);
      myOnHide.run();
    }

    @Override
    public boolean isVisible() {
      return myBalloon != null && myBalloon.isVisible();
    }
  }

  public NotificationComponent.AnimationView getAnimationHost() {
    return myAnimationHost;
  }
}
