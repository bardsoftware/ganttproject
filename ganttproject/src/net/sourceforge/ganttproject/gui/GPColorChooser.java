/*
Copyright 2014 BarD Software s.r.o

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Custom component which adds a configurable list of recently used colors to Swing standard color chooser.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class GPColorChooser {
  private JColorChooser myChooserImpl;
  private List<Color> myRecentColors;

  public GPColorChooser(List<Color> recentColors) {
    myChooserImpl = new JColorChooser();
    myRecentColors = recentColors;
  }

  public JComponent buildComponent() {
    JPanel result = new JPanel(new BorderLayout());
    result.add(myChooserImpl, BorderLayout.CENTER);
    JPanel southPanel = new JPanel(new BorderLayout());
    southPanel.add(new JLabel("Recent colors"), BorderLayout.WEST);

    Box colorLabels = Box.createHorizontalBox();
    for (Color c : myRecentColors) {
      JPanel label = new JPanel();
      label.setPreferredSize(new Dimension(16, 16));
      label.setMaximumSize(new Dimension(16, 16));
      label.setBackground(c);
      label.setBorder(BorderFactory.createLineBorder(c.darker(), 1));
      colorLabels.add(label);
      colorLabels.add(Box.createHorizontalStrut(5));
    }
    colorLabels.setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 0));
    southPanel.add(colorLabels, BorderLayout.CENTER);
    southPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
    result.add(southPanel, BorderLayout.SOUTH);
    return result;
  }

  public Color getColor() {
    return myChooserImpl.getColor();
  }

  public void setColor(Color color) {
    myChooserImpl.setColor(color);
  }
}
