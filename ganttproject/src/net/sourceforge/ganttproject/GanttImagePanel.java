/***************************************************************************
 GanttImagePanel.java  -  description
 -------------------
 begin                : dec 2002
 copyright            : (C) 2002 by Thomas Alexandre
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

package net.sourceforge.ganttproject;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Class to create a panel with the image above the Tree
 */
public class GanttImagePanel extends JPanel {
    private final int x, y;

    private final ImageIcon image;

    public GanttImagePanel(String imagename, int x, int y) {
        super(new GridBagLayout());
        image = new ImageIcon(getClass().getResource("/icons/" + imagename));
        this.x = x;
        this.y = y;
        applyComponentOrientation(GanttLanguage.getInstance()
                .getComponentOrientation());
    }

    /** Repaint the component */
    public void paintComponent(Graphics g) {
        image.paintIcon(this, g, 0, 0);
    }

    /** The preferred size of this panel */
    public Dimension getPreferredSize() {
        return new Dimension(x, y);
    }
}
