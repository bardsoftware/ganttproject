/***************************************************************************
 GanttCellListRenderer.java  -  description
 -------------------
 begin                : jan 2003
 copyright            : (C) 2003 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package net.sourceforge.ganttproject.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import net.sourceforge.ganttproject.language.GanttLanguage;

/** Class to render the background of selected cell to special blue */
public class GanttCellListRenderer extends JLabel implements ListCellRenderer {
    public GanttCellListRenderer() {
        setOpaque(true);
        applyComponentOrientation(GanttLanguage.getInstance()
                .getComponentOrientation());
    }

    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        setText(String.valueOf(value));
        setBackground(isSelected ? new Color((float) 0.290, (float) 0.349,
                (float) 0.643) : Color.white);
        setForeground(isSelected ? Color.white : Color.black);
        return this;
    }

}
