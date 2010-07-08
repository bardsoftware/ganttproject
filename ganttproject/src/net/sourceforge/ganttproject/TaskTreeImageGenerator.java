package net.sourceforge.ganttproject;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.task.BlankLineNode;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.util.TextLengthCalculator;
import net.sourceforge.ganttproject.util.TextLengthCalculatorImpl;

class TaskTreeImageGenerator {
    private GanttTree2 myTreeView;
    private UIConfiguration myUIConfiguration;
    private int myWidth;

    TaskTreeImageGenerator(GanttTree2 treeView, UIConfiguration uiConfiguration) {
        myTreeView = treeView;
        myUIConfiguration = uiConfiguration;

    }

    private GanttTree2 getTree() {
        return myTreeView;
    }

    List getPrintableNodes(GanttExportSettings settings) {
        List myItemsToConsider;
        if (settings.isOnlySelectedItem()) {
            myItemsToConsider = Arrays.asList(getTree().getSelectedNodes());
        }
        else {
            myItemsToConsider = getTree().getAllVisibleNodes();
        }
        System.out.println("TaskToConsider.size = " + myItemsToConsider.size());

        for (int i = 0; i < myItemsToConsider.size(); i++) {
            if (((DefaultMutableTreeNode) myItemsToConsider.get(i)).isRoot()) {
                myItemsToConsider.remove(i);
                break;
            }
        }
        return myItemsToConsider;

    }
    Image createImage(List myItemsToConsider) {
        BufferedImage tmpImage = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB);

        FontMetrics fmetric = tmpImage.getGraphics().getFontMetrics(
        myUIConfiguration.getChartMainFont().deriveFont(12f));
        int fourEmWidth = fmetric.stringWidth("mmmm");

        int width = 0;
        int height = getTree().getTreeTable().getRowHeight()*3 + HEADER_OFFSET;
        for (Iterator tasks = myItemsToConsider.iterator(); tasks.hasNext();) {
            DefaultMutableTreeNode nextTreeNode = (DefaultMutableTreeNode) tasks
                    .next();

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
                int nbchar = fmetric.stringWidth(next.getName())+next.getManager().getTaskHierarchy().getDepth(next)*fourEmWidth;
                if (nbchar > width) {
                    width = nbchar;
                }
            }
        }

        width += 10;
        myWidth = width;

        BufferedImage image2 = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        // setSize(sizeTOC, getHeight());
        Graphics g2 = image2.getGraphics();
        ((Graphics2D) g2).setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        printTasks(g2, myItemsToConsider);

        GanttImagePanel but = new GanttImagePanel("big.png", 300, 47);
        g2.setColor(new Color(102, 153, 153));
        g2.fillRect(0,0, width, but.getHeight());
        but.paintComponent(g2);
        return image2;
    }

    private int getWidth() {
        return myWidth;
    }

    private void printTasks(Graphics g, List taskNodes) {

        g.setColor(Color.black);

        // g.setFont(myUIConfiguration.getChartMainFont().deriveFont(12f));
        g.setFont(Fonts.PRINT_CHART_FONT);

        // JA Changed 42 to 66
        // This is related to the hardcoded headerheight
        // TODO: Fix hard-coded part
        // printTask(g,5,66,getTree().getAllChildTask(getTree().getRoot()));
        printTask(g,taskNodes);

    }

    private int printTask(Graphics g, List child) {
        int rowCount=0;
        final int h = getTree().getTreeTable().getRowHeight();
        Stack nestingStack = new Stack();
        //int x = 5;
        final int fourEmWidth = new TextLengthCalculatorImpl(g).getTextLength("mmmm");
        int y = getTree().getTable().getTableHeader().getHeight()
        + HEADER_OFFSET;
        for (Iterator tasks = child.iterator(); tasks.hasNext();) {
            DefaultMutableTreeNode nextTreeNode = (DefaultMutableTreeNode) tasks
                    .next();

            boolean blankline = nextTreeNode instanceof BlankLineNode;
            Task next = null;
            if (!blankline) {
                next = (Task) nextTreeNode.getUserObject();
                while (!nestingStack.isEmpty()) {
                    DefaultMutableTreeNode topStackNode = (DefaultMutableTreeNode) nestingStack.pop();
                    if (nextTreeNode.getParent()==topStackNode) {
                        nestingStack.push(topStackNode);
                        break;
                    }
                }
                nestingStack.push(nextTreeNode);
            }
            if (blankline || isVisible(next)) {
                if (rowCount % 2 == 1) {
                    g.setColor(new Color((float) 0.933, (float) 0.933,
                            (float) 0.933));
                    g.fillRect(0, y, getWidth() - h / 2, h);
                }
                g.setColor(Color.black);
                g.drawRect(0, y, getWidth() - h / 2, h);
                if (!blankline) {
                    int charH = (int) g.getFontMetrics().getLineMetrics(
                            next.getName(), g).getAscent();
                    int x = (nestingStack.size()-1)*fourEmWidth+5;
                    g.drawString(next.getName(), x, y + charH
                                    + (h - charH) / 2);

                }

                g.setColor(new Color((float) 0.807, (float) 0.807,
                        (float) 0.807));

                g.drawLine(1, y + h-1, getWidth() - 11, y + h-1);
                y += h;

                rowCount++;

                // if (nextTreeNode.getChildCount() != 0) {
                // y = printTask(g, x + (h / 2), y, getTree().getAllChildTask(
                // nextTreeNode));
                // }
            }
        }
        return y;
    }



    private boolean isVisible(Task thetask) {
        boolean res = true;
        DefaultMutableTreeNode father = getTree().getFatherNode(thetask);
        if (father == null) {
            return false;
        }

        while (father != null) {
            Task taskFather = (Task) (father.getUserObject());
            if (!taskFather.getExpand()) {
                res = false;
            }
            father = (DefaultMutableTreeNode) (father.getParent());
        }
        return res;
    }

    private static final int HEADER_OFFSET = 44;

}
