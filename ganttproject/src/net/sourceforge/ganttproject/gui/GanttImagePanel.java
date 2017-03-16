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

import javax.swing.*;
import java.awt.*;

public class GanttImagePanel extends JLabel {
  private final Image myOriginalImage;
  private final Dimension myOriginalSize;

  public GanttImagePanel(Image image, int width, int height) {
    super("", new ImageIcon(image.getScaledInstance(-1, height, Image.SCALE_DEFAULT)), LEADING);

    myOriginalSize = new Dimension(getIcon().getIconWidth(), getIcon().getIconHeight());
    setPreferredSize(myOriginalSize);
    setMinimumSize(myOriginalSize);
    setMinimumSize(myOriginalSize);
    myOriginalImage = image;
  }

  public void setScale(float scale) {
    Dimension scaled = new Dimension(
        (int)(myOriginalSize.width * scale),
        (int)(myOriginalSize.height * scale));
    setIcon(new ImageIcon(myOriginalImage.getScaledInstance(-1, scaled.height, Image.SCALE_DEFAULT)));
    setPreferredSize(scaled);
    setMinimumSize(scaled);
    setMinimumSize(scaled);
    Rectangle bounds = getBounds();
    bounds.setSize(scaled);
    setBounds(bounds);
  }
}
