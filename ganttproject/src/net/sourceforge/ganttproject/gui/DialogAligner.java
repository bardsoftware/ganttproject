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
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JFrame;

public class DialogAligner {
    public static void center(JDialog dialog, Container parent) {
        boolean alignToParent = false;
        if (parent != null) {
            alignToParent = parent.isVisible();
        }

        if (alignToParent) {
            Point point = parent.getLocationOnScreen();
            int x = (int) point.getX() + parent.getWidth() / 2;
            int y = (int) point.getY() + parent.getHeight() / 2;
            dialog.setLocation(x - dialog.getWidth() / 2, y
                    - dialog.getHeight() / 2);
        } else {
            centerOnScreen(dialog);
        }
    }

    public static void center(JFrame frame) {
        centerOnScreen(frame);
    }

    public static void centerOnScreen(Component component) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        component.setLocation(
                screenSize.width / 2 - (component.getPreferredSize().width / 2),
                screenSize.height / 2 - (component.getPreferredSize().height / 2));
    }
}
