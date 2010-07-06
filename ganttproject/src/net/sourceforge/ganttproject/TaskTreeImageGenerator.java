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

import javax.swing.ImageIcon;
import javax.swing.table.JTableHeader;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.GanttTreeTable.DisplayedColumn;
import net.sourceforge.ganttproject.GanttTreeTable.DisplayedColumnsList;
import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.task.BlankLineNode;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskNode;
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
            }
        }

        // Get the entire witdth of the Task Tree table for the graphical 
        // area to be considered
        myWidth = getTree().getTreeTable().getWidth();
        
        BufferedImage image2 = new BufferedImage(getWidth(), height,
                BufferedImage.TYPE_INT_RGB);
        // setSize(sizeTOC, getHeight());
        Graphics g2 = image2.getGraphics();
        ((Graphics2D) g2).setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), height);
        printTasks(g2, myItemsToConsider);
        
        GanttImagePanel but = new GanttImagePanel("big.png", 300, 47);
        g2.setColor(new Color(102, 153, 153));
        g2.fillRect(0,0, getWidth(), but.getHeight());
        but.paintComponent(g2);        
        
        // Insert a bitmap of the Table Header region to complete the 
        // generation of the Task tree image.
        JTableHeader ganttTaskHeader = getTree().getTable().getTableHeader();
        g2.translate(0, HEADER_OFFSET);
        ganttTaskHeader.paint(g2);        
        
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
        
        // Generic object for calculating the graphical length of a text string
        final TextLengthCalculatorImpl lengthCalculator = new TextLengthCalculatorImpl(g);
        
        final int fourEmWidth = lengthCalculator.getTextLength("mmmm");
        int y = getTree().getTable().getTableHeader().getHeight() + HEADER_OFFSET;
        
        FontMetrics fmetric = g.getFontMetrics(myUIConfiguration.getChartMainFont().deriveFont(12f));        
        
        // The list of column object which are currently being used or referenced 
        // to by the code
		DisplayedColumnsList dispCols = getTree().getTreeTable().getDisplayColumns();
		
        // Total number of columns displayed in the tree (this is required to iterate 
        // through the different columns whose order, width and type can be dynamically 
        // changed by a user)
        int numDispCols = dispCols.size();
        
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
                    g.fillRect(0, y, getWidth() - 2, h);
                }
                g.setColor(Color.black);
                g.drawRect(0, y, getWidth() - 2, h);
                
                if (!blankline) {
                    int charH = (int) g.getFontMetrics().getLineMetrics(next.getName(), g).getAscent();

                    // Use the Task Hierarchy indentation only for the Task name, 
                    // and not for all the other columns also
                    int xOfs = (nestingStack.size()-1)*fourEmWidth/2; //+5;
                    
                    // A small constant offset for the X Co-ords
                    int x = 2;
                    
                    // The primary loop works based on the "Order" value of each 
                    // column entry because the column number does not correspond to 
                    // the physical location of that entry in the table but the order does
                    for(int colOrd = 0; colOrd < numDispCols; colOrd++) {
                    	
                    	// Extract the name of the column from the order value
                    	String colName = dispCols.getNameForOrder(colOrd);

                    	if(colName == null)
                    	{
                    		continue;
                    	}
                    	
                    	// Only worry about columns which are actually displayed in the 
                    	// current view
                    	if(!dispCols.isDisplayed(colName))
                    	{
                    		continue;
                    	}
   
                    	// Local width of the current column being processed
                    	int currWidth = getTree().getTreeTable().getColumn(colName).getWidth();                    	
                    	
						TaskNode currTaskNode = new TaskNode(next);
						
						
						// Now do the actual work of recognising the type of column, and 
						// extracting the relevant data from the Task entries in each row
						// (NOTE: There should be a better way to do this!!)
						// The length of the text in the column is clipped based on the actual 
						// width of each column as set in the main java
						if(colName.equalsIgnoreCase(GanttTreeTableModel.strColName)) {
							String strToDraw = (String)getTree().getModel().getValueAt(currTaskNode, 3);
							if((lengthCalculator.getTextLength(strToDraw) + xOfs) > currWidth) {
								strToDraw = strToDraw.substring(0, ((currWidth - xOfs)/NAME_STR_DIVIDER) - 5);
								strToDraw += "... ";								
							}
                    		g.drawString(strToDraw, x + xOfs, y + charH + (h - charH) / 2);
                    	}
                    	else if(colName.equalsIgnoreCase(GanttTreeTableModel.strColBegDate)) {
							String strToDraw = getTree().getModel().getValueAt(currTaskNode, 4).toString();
							if(lengthCalculator.getTextLength(strToDraw) > currWidth) {
								strToDraw = strToDraw.substring(0, (currWidth/DATE_STR_DIVIDER) - 5);
								strToDraw += "... ";								
							}
							
                    		g.drawString(strToDraw, (x + (currWidth - lengthCalculator.getTextLength(strToDraw))/2), y + charH + (h - charH) / 2);
                    	}
                    	else if(colName.equalsIgnoreCase(GanttTreeTableModel.strColEndDate)) {
							String strToDraw = getTree().getModel().getValueAt(currTaskNode, 5).toString();
							if(lengthCalculator.getTextLength(strToDraw) > currWidth) {
								strToDraw = strToDraw.substring(0, (currWidth/DATE_STR_DIVIDER) - 5);
								strToDraw += "... ";								
							}
                    		g.drawString(strToDraw, (x + (currWidth - lengthCalculator.getTextLength(strToDraw))/2), y + charH + (h - charH) / 2);
                    	}
                    	else if(colName.equalsIgnoreCase(GanttTreeTableModel.strColDuration)) {
							String strToDraw = getTree().getModel().getValueAt(currTaskNode, 6).toString();
							if(lengthCalculator.getTextLength(strToDraw) > currWidth) {
								strToDraw = strToDraw.substring(0, (currWidth/NUM_STR_DIVIDER) - 5);
								strToDraw += "... ";								
							}
                    		g.drawString(strToDraw, (x + (currWidth - lengthCalculator.getTextLength(strToDraw))/2), y + charH + (h - charH) / 2);
                    	}
                    	else if(colName.equalsIgnoreCase(GanttTreeTableModel.strColCompletion)) {
							String strToDraw = getTree().getModel().getValueAt(currTaskNode, 7).toString();
							if(lengthCalculator.getTextLength(strToDraw) > currWidth) {
								strToDraw = strToDraw.substring(0, (currWidth/NUM_STR_DIVIDER) - 5);
								strToDraw += "... ";								
							}
	                		g.drawString(strToDraw, (x + (currWidth - lengthCalculator.getTextLength(strToDraw))/2), y + charH + (h - charH) / 2);
                    	}
                    	else if(colName.equalsIgnoreCase(GanttTreeTableModel.strColCoordinator)) {
							String strToDraw = (String)getTree().getModel().getValueAt(currTaskNode, 8);
							if(lengthCalculator.getTextLength(strToDraw) > currWidth) {
								strToDraw = strToDraw.substring(0, (currWidth/NAME_STR_DIVIDER) - 5);
								strToDraw += "... ";								
							}
                    		g.drawString(strToDraw,(x + (currWidth - lengthCalculator.getTextLength(strToDraw))/2),y + charH + (h - charH) / 2);
                    	}
                    	else if(colName.equalsIgnoreCase(GanttTreeTableModel.strColInfo)) {
                    		ImageIcon infoIcon = (ImageIcon)(getTree().getModel().getValueAt(currTaskNode, 2));
                    		if(infoIcon != null) {
                        		g.drawImage(infoIcon.getImage(), x + (currWidth - infoIcon.getIconWidth())/2, y + (h - infoIcon.getIconHeight())/2, infoIcon.getImageObserver());                    			
                    		}
                    	}
                    	else if(colName.equalsIgnoreCase(GanttTreeTableModel.strColPriority)) {
                    		ImageIcon infoIcon = (ImageIcon)(getTree().getModel().getValueAt(currTaskNode, 1));
                    		if(infoIcon != null) {
                        		g.drawImage(infoIcon.getImage(), x + (currWidth - infoIcon.getIconWidth())/2, y + (h - infoIcon.getIconHeight())/2, infoIcon.getImageObserver());                    									
                    		}
                    	}
                    	else if(colName.equalsIgnoreCase(GanttTreeTableModel.strColType)) {
                    		ImageIcon infoIcon = (ImageIcon)(getTree().getModel().getValueAt(currTaskNode, 0));
                    		if(infoIcon != null) {
                        		g.drawImage(infoIcon.getImage(), x + (currWidth - infoIcon.getIconWidth())/2, y + (h - infoIcon.getIconHeight())/2, infoIcon.getImageObserver());                    									
                    		}
                    	}
                    	else if(colName.equalsIgnoreCase(GanttTreeTableModel.strColID)) {
							String strToDraw = getTree().getModel().getValueAt(currTaskNode, 10).toString();
							if(lengthCalculator.getTextLength(strToDraw) > currWidth) {
								strToDraw = strToDraw.substring(0, (currWidth/NUM_STR_DIVIDER) - 5);
								strToDraw += "... ";								
							}
                    		g.drawString(strToDraw, (x + (currWidth - lengthCalculator.getTextLength(strToDraw))/2), y + charH + (h - charH) / 2);                									
                   		}
           			
						x += currWidth;
                    }                    
                }

                g.setColor(new Color((float) 0.807, (float) 0.807,
                        (float) 0.807));

                g.drawLine(1, y + h-1, getWidth(), y + h-1);
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
    private static final int DATE_STR_DIVIDER = 4;
    private static final int NAME_STR_DIVIDER = 6;    
    private static final int NUM_STR_DIVIDER = 4;
}
