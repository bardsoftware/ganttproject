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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.swing.tree.DefaultMutableTreeNode;

import biz.ganttproject.core.chart.render.TextLengthCalculatorImpl;

import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.task.BlankLineNode;
import net.sourceforge.ganttproject.task.Task;

class TaskTreeImageGenerator {
  private GanttTree2 myTreeView;
  private UIConfiguration myUIConfiguration;
  private int myWidth;
  private final Image myLogo;

  TaskTreeImageGenerator(GanttTree2 treeView, UIConfiguration uiConfiguration, Image logo) {
    myTreeView = treeView;
    myUIConfiguration = uiConfiguration;
    myLogo = logo;
  }

  protected GanttTree2 getTree() {
    return myTreeView;
  }

  protected Dimension calculateDimension(List<DefaultMutableTreeNode> taskNodes) {
    BufferedImage tmpImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

    FontMetrics fmetric = tmpImage.getGraphics().getFontMetrics(Fonts.DEFAULT_CHART_FONT.deriveFont(12f));
    int fourEmWidth = fmetric.stringWidth("mmmm");

    int width = 0;
    int height = getTree().getTreeTable().getRowHeight() * 3 + HEADER_OFFSET;
    for (Iterator<DefaultMutableTreeNode> tasks = taskNodes.iterator(); tasks.hasNext();) {
      DefaultMutableTreeNode nextTreeNode = tasks.next();

      if (nextTreeNode instanceof BlankLineNode) {
        height += getTree().getTreeTable().getRowHeight();
        continue;
      }

      Task next = (Task) nextTreeNode.getUserObject();
      if ("None".equals(next.toString())) {
        continue;
      }
      if (isVisible(next)) {
        height += getTree().getTreeTable().getRowHeight();
        int nbchar = fmetric.stringWidth(next.getName()) + next.getManager().getTaskHierarchy().getDepth(next)
            * fourEmWidth;
        if (nbchar > width) {
          width = nbchar;
        }
      }
    }
    width += 10;
    return new Dimension(width, height);
  }

  Image createImage(List<DefaultMutableTreeNode> taskNodes) {
    Dimension d = calculateDimension(taskNodes);
    myWidth = d.width;

    BufferedImage image = new BufferedImage(getWidth(), d.height, BufferedImage.TYPE_INT_RGB);
    paint(image, d, taskNodes);
    return image;
  }

  protected void paint(Image image, Dimension d, List<DefaultMutableTreeNode> taskNodes) {
    Graphics2D g = (Graphics2D) image.getGraphics();
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, getWidth(), d.height);
    printTasks(g, taskNodes);

    // GanttImagePanel but = new GanttImagePanel("big.png", 300, 47);
    g.setColor(new Color(102, 153, 153));
    g.drawImage(myLogo, 0, 0, null);
    // but.paintComponent(g2);
  }

  private int getWidth() {
    return myWidth;
  }

  static class PaintState {
    int y;
    int rowCount = 0;
    Stack<DefaultMutableTreeNode> nestingStack = new Stack<DefaultMutableTreeNode>();
    int rowHeight;
    int indent;
  }

  private int printTasks(Graphics2D g, List<DefaultMutableTreeNode> taskNodes) {
    g.setColor(Color.black);
    g.setFont(Fonts.PRINT_CHART_FONT);

    PaintState state = new PaintState();
    state.y = getTree().getTable().getTableHeader().getHeight() + HEADER_OFFSET;
    state.rowHeight = getTree().getTreeTable().getRowHeight();
    // int x = 5;
    state.indent = TextLengthCalculatorImpl.getTextLength(g, "mmmm");
    for (Iterator<DefaultMutableTreeNode> tasks = taskNodes.iterator(); tasks.hasNext();) {
      DefaultMutableTreeNode nextTreeNode = tasks.next();

      boolean blankline = nextTreeNode instanceof BlankLineNode;
      Task next = null;
      if (!blankline) {
        next = (Task) nextTreeNode.getUserObject();
        while (!state.nestingStack.isEmpty()) {
          DefaultMutableTreeNode topStackNode = state.nestingStack.pop();
          if (nextTreeNode.getParent() == topStackNode) {
            state.nestingStack.push(topStackNode);
            break;
          }
        }
        state.nestingStack.push(nextTreeNode);
      }
      if (blankline || isVisible(next)) {
        if (state.rowCount % 2 == 1) {
          // Make alternating background pattern
          // TODO Define background color for the alternating rows (and use that
          // everywhere)
          g.setColor(new Color(0.933f, 0.933f, 0.933f));
          g.fillRect(0, state.y, getWidth() - state.rowHeight / 2, state.rowHeight);
        }
        g.setColor(Color.black);
        g.drawRect(0, state.y, getWidth() - state.rowHeight / 2, state.rowHeight);
        if (!blankline) {
          paintTask(g, state, next);
        }

        g.setColor(new Color(0.807f, 0.807f, 0.807f));

        g.drawLine(1, state.y + state.rowHeight - 1, getWidth() - 11, state.y + state.rowHeight - 1);
        state.y += state.rowHeight;

        state.rowCount++;
      }
    }
    return state.y;
  }

  protected void paintTask(Graphics g, PaintState state, Task t) {
    int charH = (int) g.getFontMetrics().getLineMetrics(t.getName(), g).getAscent();
    int x = (state.nestingStack.size() - 1) * state.indent + 5;
    g.drawString(t.getName(), x, state.y + charH + (state.rowHeight - charH) / 2);
  }

  private boolean isVisible(Task thetask) {
    return getTree().isVisible(thetask);
  }

  protected static final int HEADER_OFFSET = 44;
}
