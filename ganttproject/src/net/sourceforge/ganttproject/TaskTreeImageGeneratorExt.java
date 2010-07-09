package net.sourceforge.ganttproject;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.table.JTableHeader;

import net.sourceforge.ganttproject.GanttTreeTable.DisplayedColumnsList;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskNode;
import net.sourceforge.ganttproject.util.TextLengthCalculator;
import net.sourceforge.ganttproject.util.TextLengthCalculatorImpl;

public class TaskTreeImageGeneratorExt extends TaskTreeImageGenerator {
    TaskTreeImageGeneratorExt(GanttTree2 treeView, UIConfiguration uiConfiguration) {
        super(treeView, uiConfiguration);
    }

    @Override
    protected Dimension calculateDimension(List taskNodes) {
        Dimension d = super.calculateDimension(taskNodes);
        return new Dimension(getTree().getTreeTable().getWidth(), d.height);
    }

    @Override
    protected void paint(Image image, Dimension d, List taskNodes) {
        super.paint(image, d, taskNodes);
        // Insert a bitmap of the Table Header region to complete the
        // generation of the Task tree image.
        JTableHeader ganttTaskHeader = getTree().getTable().getTableHeader();
        image.getGraphics().translate(0, HEADER_OFFSET);
        ganttTaskHeader.paint(image.getGraphics());
    }

  @Override
  protected void paintTask(Graphics g, PaintState state, Task t) {
    final TextLengthCalculatorImpl lengthCalculator = new TextLengthCalculatorImpl(g);
    // The list of column object which are currently being used or referenced
    // to by the code
    final DisplayedColumnsList dispCols = getTree().getTreeTable().getDisplayColumns();

    // A small constant offset for the X Co-ords
    int x = 2;

    // The primary loop works based on the "Order" value of each
    // column entry because the column number does not correspond to
    // the physical location of that entry in the table but the order does
    for (int colOrd = 0; colOrd < dispCols.size(); colOrd++) {

      // Extract the name of the column from the order value
      String colName = dispCols.getNameForOrder(colOrd);

      if (colName == null) {
        continue;
      }

      // Only worry about columns which are actually displayed in the
      // current view
      if (!dispCols.isDisplayed(colName)) {
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
      if (colName.equalsIgnoreCase(GanttTreeTableModel.strColName)) {
        String strToDraw = (String) getTree().getModel().getValueAt(currTaskNode, 3);
        int nameIndent = (state.nestingStack.size() - 1) * state.indent / 2;
        paintString(g, lengthCalculator, strToDraw, state, x, currWidth - nameIndent);
      } else if (colName.equalsIgnoreCase(GanttTreeTableModel.strColBegDate)) {
        String strToDraw = getTree().getModel().getValueAt(currTaskNode, 4).toString();
        paintString(g, lengthCalculator, strToDraw, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(GanttTreeTableModel.strColEndDate)) {
        String strToDraw = getTree().getModel().getValueAt(currTaskNode, 5).toString();
        paintString(g, lengthCalculator, strToDraw, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(GanttTreeTableModel.strColDuration)) {
        String strToDraw = getTree().getModel().getValueAt(currTaskNode, 6).toString();
        paintString(g, lengthCalculator, strToDraw, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(GanttTreeTableModel.strColCompletion)) {
        String strToDraw = getTree().getModel().getValueAt(currTaskNode, 7).toString();
        paintString(g, lengthCalculator, strToDraw, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(GanttTreeTableModel.strColCoordinator)) {
        String strToDraw = (String) getTree().getModel().getValueAt(currTaskNode, 8);
        paintString(g, lengthCalculator, strToDraw, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(GanttTreeTableModel.strColInfo)) {
        ImageIcon infoIcon = (ImageIcon) (getTree().getModel().getValueAt(currTaskNode, 2));
        paintIcon(g, infoIcon, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(GanttTreeTableModel.strColPriority)) {
        ImageIcon infoIcon = (ImageIcon) (getTree().getModel().getValueAt(currTaskNode, 1));
        paintIcon(g, infoIcon, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(GanttTreeTableModel.strColType)) {
        ImageIcon infoIcon = (ImageIcon) (getTree().getModel().getValueAt(currTaskNode, 0));
        paintIcon(g, infoIcon, state, x, currWidth);
      } else if (colName.equalsIgnoreCase(GanttTreeTableModel.strColID)) {
        String strToDraw = getTree().getModel().getValueAt(currTaskNode, 10).toString();
        paintString(g, lengthCalculator, strToDraw, state, x, currWidth);
      }

      x += currWidth;
    }
  }

    private static void paintString(
        Graphics g, TextLengthCalculator lengthCalculator, String s, PaintState paintState,
        int xpos, int widthLimit) {
        if (lengthCalculator.getTextLength(s) > widthLimit) {
            s = s.substring(0, (widthLimit/lengthCalculator.getTextLength("m")) - 5);
            s += "... ";
        }
        int textHeight = lengthCalculator.getTextHeight(s);
        g.drawString(s, xpos, paintState.y + textHeight + (paintState.rowHeight - textHeight) / 2);
    }

    private static void paintIcon(Graphics g, ImageIcon icon, PaintState paintState, int xpos, int widthLimit) {
        if(icon != null) {
            g.drawImage(icon.getImage(), xpos + (widthLimit - icon.getIconWidth())/2,
                paintState.y + (paintState.rowHeight - icon.getIconHeight())/2,
                icon.getImageObserver());
        }

    }
}
