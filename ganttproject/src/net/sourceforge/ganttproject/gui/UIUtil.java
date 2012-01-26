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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import net.sourceforge.ganttproject.action.GPAction;

public abstract class UIUtil {
    public static void repackWindow(JComponent component) {
        Window windowAncestor = SwingUtilities.getWindowAncestor(component);
        if (windowAncestor != null) {
            windowAncestor.invalidate();
            windowAncestor.pack();
            windowAncestor.repaint();
        }
        DialogAligner.centerOnScreen(windowAncestor);
    }

    public static void setEnabledTree(JComponent root, boolean isEnabled) {
        root.setEnabled(isEnabled);
        Component[] components = root.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JComponent) {
                setEnabledTree((JComponent) components[i], isEnabled);
            }
        }
    }

    public static void setBackgroundTree(JComponent root, Color background) {
        root.setBackground(background);
        Component[] components = root.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JComponent) {
                setBackgroundTree((JComponent) components[i], background);
            }
        }
    }

    public static void createTitle(JComponent component, String title) {
        Border lineBorder = BorderFactory.createMatteBorder(1,0,0,0,Color.BLACK);
        component.setBorder(BorderFactory.createTitledBorder(lineBorder, title));
    }

    public static void pushAction(JComponent root, KeyStroke keyStroke, GPAction action) {
        root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, action.getID());
        root.getActionMap().put(action.getID(), action);
        for (Component child : root.getComponents()) {
            if (child instanceof JComponent) {
                pushAction((JComponent)child, keyStroke, action);
            }
        }
    }

    public static void setupTableUI(JTable table) {
        table.setPreferredScrollableViewportSize(new Dimension(
                table.getPreferredScrollableViewportSize().width,
                table.getRowHeight() * 10));
    }


}
