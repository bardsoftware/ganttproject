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
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author Dmitry Barashev
 */
public class TopPanel {

  public static JComponent create(String title, String comment) {

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.setBackground(Color.ORANGE);
    topPanel.setForeground(Color.BLACK);
    topPanel.setBorder(BorderFactory.createLineBorder(Color.ORANGE.darker()));

    String labelText = "<html>";
    if (title != null) {
        labelText += "<b>" + title + "</b>";
    }
    if (comment != null) {
      labelText += "<br>" + comment;
    }
    labelText += "</html>";

    JLabel labelTitle = new JLabel(labelText);
    labelTitle.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
    topPanel.add(labelTitle, BorderLayout.NORTH);
    return topPanel;
  }
}
