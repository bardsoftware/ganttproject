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
package net.sourceforge.ganttproject;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.table.JTableHeader;
import javax.swing.tree.DefaultMutableTreeNode;

import biz.ganttproject.core.chart.canvas.TextMetrics;
import biz.ganttproject.core.chart.render.TextLengthCalculatorImpl;
import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.table.ColumnList.Column;

import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskNode;

public class TaskTreeImageGeneratorExt extends TaskTreeImageGenerator {
  TaskTreeImageGeneratorExt(GanttTree2 treeView, UIConfiguration uiConfiguration, Image logo) {
    super(treeView, uiConfiguration, logo);
  }

  @Override
  protected Dimension calculateDimension(List<DefaultMutableTreeNode> taskNodes) {
    Dimension d = super.calculateDimension(taskNodes);
    return new Dimension(getTree().getTreeTable().getWidth(), d.height);
  }

  @Override
  protected void paint(Image image, Dimension d, List<DefaultMutableTreeNode> taskNodes) {
    super.paint(image, d, taskNodes);
    // Insert a bitmap of the Table Header region to complete the
    // generation of the Task tree image.
    JTableHeader ganttTaskHeader = getTree().getTable().getTableHeader();
    Graphics g = image.getGraphics();
    g.translate(0, HEADER_OFFSET);
    ganttTaskHeader.paint(g);
  }

  @Override
  protected void paintTask(Graphics g, PaintState state, Task t) {
    final TextLengthCalculatorImpl lengthCalculator = new TextLengthCalculatorImpl((Graphics2D) g);
    // The list of column object which are currently being used or referenced
    // to by the code
    final ColumnList dispCols = getTree().getTreeTable().getVisibleFields();

    // A small constant offset for the X coordinates
    int x = 2;

    List<Column> columns = new ArrayList<Column>();
    for (int i = 0; i < dispCols.getSize(); i++) {
      Column c = dispCols.getField(i);
      if (c.isVisible()) {
        columns.add(c);
      }
    }
    Collections.sort(columns, new Comparator<Column>() {
      @Override
      public int compare(Column left, Column right) {
        return left.getOrder() - right.getOrder();
      }
    });
    // The primary loop works based on the "Order" value of each
    // column entry because the column number does not correspond to
    // the physical location of that entry in the table but the order does
    for (Column c : columns) {

      // Extract the name of the column from the order value
      String colName = c.getName();

      if (colName == null) {
        continue;
      }

      // Local width of the current column being processed
      int currWidth = getTree().getTreeTable().getColumn(colName).getWidth();

      TaskNode currTaskNode = new TaskNode(t);

      // Now do the actual work of recognising the type of column, and
      // extracting the relevant data from the Task entries in each row
      // (NOTE: There should be a better way to do this!!)
      // The length of the text in the column is clipped based on the actual
      // width of each column as set in the main java
      if (colName.equalsIgnoreCase(TaskDefaultColumn.NAME.getName())) {
        String strToDraw = (String) getTree().getModel().getValueAt(currTaskNode, 3);
        int nameIndent = (state.nestingStack.size() - 1) * state.indent / 2;
        paintString(g, lengthCalculator, strToDraw, state, x + nameIndent, currWidth - nameIndent);
      } else if (colName.equalsIgnoreCase(TaskDefaultColumn.BEGIN_DATE.getName())) {
        String strToDraw = getTree().getModel().getValueAt(currTaskNode, 4).toString();
        paintString(g, lengthCalculator, strToDraw, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(TaskDefaultColumn.END_DATE.getName())) {
        String strToDraw = getTree().getModel().getValueAt(currTaskNode, 5).toString();
        paintString(g, lengthCalculator, strToDraw, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(TaskDefaultColumn.DURATION.getName())) {
        String strToDraw = getTree().getModel().getValueAt(currTaskNode, 6).toString();
        paintString(g, lengthCalculator, strToDraw, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(TaskDefaultColumn.COMPLETION.getName())) {
        String strToDraw = getTree().getModel().getValueAt(currTaskNode, 7).toString();
        paintString(g, lengthCalculator, strToDraw, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(TaskDefaultColumn.COORDINATOR.getName())) {
        String strToDraw = (String) getTree().getModel().getValueAt(currTaskNode, 8);
        paintString(g, lengthCalculator, strToDraw, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(TaskDefaultColumn.INFO.getName())) {
        ImageIcon infoIcon = (ImageIcon) (getTree().getModel().getValueAt(currTaskNode, 2));
        paintIcon(g, infoIcon, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(TaskDefaultColumn.PRIORITY.getName())) {
        ImageIcon infoIcon = (ImageIcon) (getTree().getModel().getValueAt(currTaskNode, 1));
        paintIcon(g, infoIcon, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(TaskDefaultColumn.INFO.getName())) {
        ImageIcon infoIcon = (ImageIcon) (getTree().getModel().getValueAt(currTaskNode, 0));
        paintIcon(g, infoIcon, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(TaskDefaultColumn.ID.getName())) {
        String strToDraw = getTree().getModel().getValueAt(currTaskNode, 10).toString();
        paintString(g, lengthCalculator, strToDraw, state, x, currWidth);
      }

      x += currWidth;
    }
  }

  private static void paintString(Graphics g, TextMetrics lengthCalculator, String s, PaintState paintState,
      int xpos, int widthLimit) {
    if (lengthCalculator.getTextLength(s) > widthLimit) {
      s = s.substring(0, (widthLimit / lengthCalculator.getTextLength("m")) - 5);
      s += "... ";
    }
    int textHeight = lengthCalculator.getTextHeight(s);
    g.drawString(s, xpos, paintState.y + textHeight + (paintState.rowHeight - textHeight) / 2);
  }

  private static void paintIcon(Graphics g, ImageIcon icon, PaintState paintState, int xpos, int widthLimit) {
    if (icon != null) {
      g.drawImage(icon.getImage(), xpos + (widthLimit - icon.getIconWidth()) / 2, paintState.y
          + (paintState.rowHeight - icon.getIconHeight()) / 2, icon.getImageObserver());
    }
  }
}
