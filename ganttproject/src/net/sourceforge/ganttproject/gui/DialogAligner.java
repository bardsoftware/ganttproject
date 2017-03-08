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

import com.google.common.base.Function;
import net.sourceforge.ganttproject.gui.UIFacade.Centering;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class DialogAligner {
  public static void center(JDialog dialog, Container parent) {
    boolean alignToParent = false;
    if (parent != null) {
      alignToParent = parent.isVisible();
    }
    center(dialog, parent, alignToParent ? Centering.WINDOW : Centering.SCREEN);
  }

  public static void center(JFrame frame) {
    frame.setLocationRelativeTo(null);
  }

  public static void center(final JDialog dlg, Container parent, UIFacade.Centering cente) {
    Function<Rectangle, Rectangle> centering = new Function<Rectangle, Rectangle>() {
      @Override
      public Rectangle apply(@Nullable Rectangle parent) {
        int xCenter = parent.x + parent.width/2;
        int yCenter = parent.y + parent.height/2;
        int dlgLeft = xCenter - dlg.getWidth()/2;
        int dlgTop = yCenter - dlg.getHeight()/2;
        return new Rectangle(dlgLeft, dlgTop, dlg.getWidth(), dlg.getHeight());
      }
    };
    UIUtil.MultiscreenFitResult fitResult = UIUtil.multiscreenFit(parent.getBounds());
    // In multiscreen environment "screen" is the one where most of the parent window is shown.
    // We will check if dialog fits into this screen.
    Rectangle screenBounds = fitResult.argmaxVisibleArea.getBounds();
    // This is a point in the virtual space where (0,0) is the primary screen location.
    // Let's try to make dialog centered relative to parent
    Rectangle centered = centering.apply(parent.getBounds());
    Rectangle intersection = centered.intersection(screenBounds);
    if (1.0 * intersection.width * intersection.height / (dlg.getHeight() * dlg.getWidth()) < 0.25) {
      // If intersection of the centered dialog with the screen is less than 1/4 of the dialog area
      // then we need to do better
      centered = centering.apply(screenBounds);
      if (!screenBounds.contains(centered)) {
        // If "centered" dialog is so big that it goes out of the screen then make it exactly screen size
        centered = screenBounds;
      }
    }
    dlg.setLocation(centered.x, centered.y);
  }
}
