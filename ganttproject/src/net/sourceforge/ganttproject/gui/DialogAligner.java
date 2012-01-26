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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JFrame;

import net.sourceforge.ganttproject.gui.UIFacade.Centering;

public class DialogAligner {
    public static void center(JDialog dialog, Container parent) {
        boolean alignToParent = false;
        if (parent != null) {
            alignToParent = parent.isVisible();
        }
        center(dialog, parent, alignToParent ? Centering.WINDOW : Centering.SCREEN);
    }

    public static void center(JFrame frame) {
        centerOnScreen(frame);
    }

    public static void centerOnScreen(Component component) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        component.setLocation(
                screenSize.width / 2 - (component.getWidth() / 2),
                screenSize.height / 2 - (component.getHeight() / 2));
    }

    public static void center(JDialog dlg, Container parent, UIFacade.Centering centering) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Point point = parent.getLocationOnScreen();
        int x = (int) point.getX() + parent.getWidth() / 2;
        int y = (int) point.getY() + parent.getHeight() / 2;

        switch (centering) {
        case SCREEN:
            dlg.setLocation(
                    screenSize.width / 2 - (dlg.getWidth() / 2),
                    screenSize.height / 2 - (dlg.getHeight() / 2));
            break;
        case WINDOW:
            int borderRight = x + dlg.getWidth() / 2;
            int borderBottom = y + dlg.getHeight() / 2;
            if (borderRight > screenSize.width) {
                x -= borderRight - screenSize.width;
            }
            if (borderBottom > screenSize.height) {
                y -= borderBottom - screenSize.height;
            }
            dlg.setLocation(x - dlg.getWidth() / 2, y - dlg.getHeight() / 2);
            break;
        }
    }
}
