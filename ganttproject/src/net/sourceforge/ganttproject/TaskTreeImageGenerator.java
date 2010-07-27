package net.sourceforge.ganttproject;

import java.awt.Color;
import java.awt.Dimension;
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

    protected GanttTree2 getTree() {
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

    protected Dimension calculateDimension(List/*<DefaultMutableTreeNode>*/ taskNodes) {
        BufferedImage tmpImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

        FontMetrics fmetric = tmpImage.getGraphics().getFontMetrics(
                myUIConfiguration.getChartMainFont().deriveFont(12f));
        int fourEmWidth = fmetric.stringWidth("mmmm");

        int width = 0;
        int height = getTree().getTreeTable().getRowHeight()*3 + HEADER_OFFSET;
        for (Iterator tasks = taskNodes.iterator(); tasks.hasNext();) {
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
        return new Dimension(width, height);
    }

    Image createImage(List/*<DefaultMutableTreeNode>*/ taskNodes) {
        Dimension d = calculateDimension(taskNodes);
        myWidth = d.width;

        BufferedImage image = new BufferedImage(getWidth(), d.height,
                BufferedImage.TYPE_INT_RGB);
        paint(image, d, taskNodes);
        return image;
    }

    protected void paint(Image image, Dimension d, List taskNodes) {
        Graphics g2 = image.getGraphics();
        ((Graphics2D) g2).setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), d.height);
        printTasks(g2, taskNodes);

        GanttImagePanel but = new GanttImagePanel("big.png", 300, 47);
        g2.setColor(new Color(102, 153, 153));
        g2.fillRect(0,0, getWidth(), but.getHeight());
        but.paintComponent(g2);
    }

    private int getWidth() {
        return myWidth;
    }

    static class PaintState {
        int y;
        int rowCount = 0;
        Stack nestingStack = new Stack();
        int rowHeight;
        int indent;
    }

    private int printTasks(Graphics g, List taskNodes) {
        g.setColor(Color.black);
        g.setFont(Fonts.PRINT_CHART_FONT);

        PaintState state = new PaintState();
        state.y = getTree().getTable().getTableHeader().getHeight() + HEADER_OFFSET;
        state.rowHeight = getTree().getTreeTable().getRowHeight();
        //int x = 5;
        state.indent = new TextLengthCalculatorImpl(g).getTextLength("mmmm");
        for (Iterator tasks = taskNodes.iterator(); tasks.hasNext();) {
            DefaultMutableTreeNode nextTreeNode = (DefaultMutableTreeNode) tasks.next();

            boolean blankline = nextTreeNode instanceof BlankLineNode;
            Task next = null;
            if (!blankline) {
                next = (Task) nextTreeNode.getUserObject();
                while (!state.nestingStack.isEmpty()) {
                    DefaultMutableTreeNode topStackNode = (DefaultMutableTreeNode) state.nestingStack.pop();
                    if (nextTreeNode.getParent()==topStackNode) {
                        state.nestingStack.push(topStackNode);
                        break;
                    }
                }
                state.nestingStack.push(nextTreeNode);
            }
            if (blankline || isVisible(next)) {
                if (state.rowCount % 2 == 1) {
                    g.setColor(new Color((float) 0.933, (float) 0.933,
                            (float) 0.933));
                    g.fillRect(0, state.y, getWidth() - state.rowHeight / 2, state.rowHeight);
                }
                g.setColor(Color.black);
                g.drawRect(0, state.y, getWidth() - state.rowHeight / 2, state.rowHeight);
                if (!blankline) {
                    paintTask(g, state, next);
                }

                g.setColor(new Color((float) 0.807, (float) 0.807,
                        (float) 0.807));

                g.drawLine(1, state.y + state.rowHeight-1, getWidth() - 11, state.y + state.rowHeight-1);
                state.y += state.rowHeight;

                state.rowCount++;
            }
        }
        return state.y;
    }

    protected void paintTask(Graphics g, PaintState state, Task t) {
        int charH = (int) g.getFontMetrics().getLineMetrics(t.getName(), g).getAscent();
        int x = (state.nestingStack.size()-1)*state.indent+5;
        g.drawString(t.getName(), x, state.y + charH + (state.rowHeight - charH) / 2);
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

    protected static final int HEADER_OFFSET = 44;

}
