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
package net.sourceforge.ganttproject.shape;

/*
 *@author Etienne L'kenfack (etienne.lkenfack@itcogita.com)
 */

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class PreviewPanel extends JPanel {
  protected ShapePaint pattern = ShapeConstants.DEFAULT;

  public PreviewPanel() {
    setOpaque(true);
    setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Preview"),
        BorderFactory.createEmptyBorder(0, 4, 8, 4)));
    setPreferredSize(new Dimension(70, 70));
  }

  public ShapePaint getPattern() {
    return new ShapePaint(pattern, getForeground(), getBackground());
  }

  public void setPattern(ShapePaint pattern) {
    this.pattern = pattern;
  }

  @Override
  public void paintComponent(Graphics gc) {
    Graphics2D g = (Graphics2D) gc;
    int w = getSize().width;
    int h = getSize().height;
    g.setColor(getParent().getBackground());
    g.fillRect(0, 0, w, h);
    if (pattern == null)
      return;
    Insets insets = getInsets();
    Rectangle rect = new Rectangle(insets.left, insets.top, w - (insets.left + insets.right), h
        - (insets.top + insets.bottom));
    g.setPaint(new ShapePaint(pattern, getForeground(), getBackground()));
    g.fill(rect);

  }
}
