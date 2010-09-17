/*
 * Created on 22.10.2005
 */
package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.AbstractBorder;

import net.sourceforge.ganttproject.action.task.LinkTasksAction;
import net.sourceforge.ganttproject.action.task.UnlinkTasksAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

import org.eclipse.core.runtime.IAdaptable;

class GanttChartTabContentPanel extends ChartTabContentPanel implements IAdaptable {
    private Container myTaskTree;
    private JComponent myGanttChart;
    private final TaskTreeUIFacade myTreeFacade;
    //private JPanel myTabContentPanel;
    private final IGanttProject myProject;
    private final UIFacade myWorkbenchFacade;
    //private  CustomScrollPane scrollPane2 ;
    GanttChartTabContentPanel(
            IGanttProject project, UIFacade workbenchFacade, TaskTreeUIFacade treeFacade,
            JComponent ganttChart) {
        super(project, workbenchFacade, workbenchFacade.getGanttChart());
        myProject = project;
        myWorkbenchFacade = workbenchFacade;
        myTreeFacade = treeFacade;
        myTaskTree = (Container) treeFacade.getTreeComponent();
        myGanttChart = ganttChart;
        //scrollPane2 = new CustomScrollPane(myGanttChart);
    }

    Component getComponent() {
        return createContentComponent();
    }


    class BorderImpl extends AbstractBorder {
        @Override
        public boolean isBorderOpaque() {
            return true;
        }

        @Override
        public void paintBorder(Component arg0, Graphics g, int x, int y, int width, int height) {
            int thickness = 2;
            for (int i = 0; i < thickness; i++) {
                g.drawRoundRect(x, y, width, height, thickness, thickness);
                x += 1;
              y+= 1;
              width -= 2;
              height -= 2;
            }
        }

    }

    protected void onChangingZoom(DefaultBoundedRangeModel model) {

    }

    protected Component createButtonPanel() {
        Box buttonBar = Box.createHorizontalBox();
        //JToolBar buttonBar = new JToolBar();
        //buttonBar.setFloatable(false);

        //
//        TestGanttRolloverButton expandAllButton = new TestGanttRolloverButton(myTreeFacade.getExpandAllAction()) {
//            public String getText() {
//                return null;
//            }
//        };
//        buttonBar.add(expandAllButton);
//        //
//        TestGanttRolloverButton collapseAllButton = new TestGanttRolloverButton(myTreeFacade.getCollapseAllAction()) {
//            public String getText() {
//                return null;
//            }
//        };
//        buttonBar.add(collapseAllButton);
//        //
//        buttonBar.add(Box.createHorizontalStrut(8));
        //

        TestGanttRolloverButton unindentButton = new TestGanttRolloverButton(myTreeFacade.getUnindentAction()) {
            public String getText() {
                return null;
            }
        };
        buttonBar.add(unindentButton);

        TestGanttRolloverButton indentButton = new TestGanttRolloverButton(myTreeFacade.getIndentAction()) {
            public String getText() {
                return null;
            }
        };
        buttonBar.add(indentButton);
        //
        buttonBar.add(Box.createHorizontalStrut(3));
        //
        TestGanttRolloverButton upButton = new TestGanttRolloverButton(myTreeFacade.getMoveUpAction()) {
            public String getText() {
                return null;
            }
        };
        buttonBar.add(upButton);
        //
        TestGanttRolloverButton downButton = new TestGanttRolloverButton(myTreeFacade.getMoveDownAction()) {
            public String getText() {
                return null;
            }
        };
        buttonBar.add(downButton);
        //
        buttonBar.add(Box.createHorizontalStrut(8));
        Action linkAction = new LinkTasksAction(myProject.getTaskManager(), Mediator.getTaskSelectionManager(), myWorkbenchFacade);
        myTreeFacade.setLinkTasksAction(linkAction);
        TestGanttRolloverButton linkButton = new TestGanttRolloverButton(linkAction) {
            public String getText() {
                return null;
            }
        };
        buttonBar.add(linkButton);
        //
        Action unlinkAction = new UnlinkTasksAction(myProject.getTaskManager(), Mediator.getTaskSelectionManager(), myWorkbenchFacade);
        myTreeFacade.setUnlinkTasksAction(unlinkAction);
        TestGanttRolloverButton unlinkButton = new TestGanttRolloverButton(unlinkAction) {
            public String getText() {
                return null;
            }
        };
        buttonBar.add(unlinkButton);
        //
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(buttonBar, BorderLayout.WEST);
        return buttonPanel;
    }
    public Object getAdapter(Class adapter) {
        if (Container.class.equals(adapter)) {
            return getComponent();
        }
        if (Chart.class.equals(adapter)) {
            return myGanttChart;
        }
        return null;
    }

    @Override
    protected Component getChartComponent() {
        return myGanttChart;
    }

    @Override
    protected Component getTreeComponent() {
        return myTaskTree;
    }
}

