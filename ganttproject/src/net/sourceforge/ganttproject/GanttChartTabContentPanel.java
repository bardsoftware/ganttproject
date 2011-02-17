/*
 * Created on 22.10.2005
 */
package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import net.sourceforge.ganttproject.action.CalculateCriticalPathAction;
import net.sourceforge.ganttproject.action.task.LinkTasksAction;
import net.sourceforge.ganttproject.action.task.UnlinkTasksAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.overview.ToolbarBuilder;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;

import org.eclipse.core.runtime.IAdaptable;

class GanttChartTabContentPanel extends ChartTabContentPanel implements IAdaptable {
    private final Container myTaskTree;
    private final JComponent myGanttChart;
    private final TaskTreeUIFacade myTreeFacade;
    private final IGanttProject myProject;
    private final UIFacade myWorkbenchFacade;
    private final CalculateCriticalPathAction myCriticalPathAction;

    GanttChartTabContentPanel(
            IGanttProject project, UIFacade workbenchFacade, TaskTreeUIFacade treeFacade,
            JComponent ganttChart, UIConfiguration uiConfiguration) {
        super(project, workbenchFacade, workbenchFacade.getGanttChart());
        myProject = project;
        myWorkbenchFacade = workbenchFacade;
        myTreeFacade = treeFacade;
        myTaskTree = (Container) treeFacade.getTreeComponent();
        myGanttChart = ganttChart;
        myCriticalPathAction = new CalculateCriticalPathAction(
            project.getTaskManager(), "16", uiConfiguration, workbenchFacade);
        addChartPanel(createSchedulePanel());
    }

    private Component createSchedulePanel() {
        return new ToolbarBuilder()
            .withBackground(myWorkbenchFacade.getGanttChart().getStyle().getSpanningHeaderBackgroundColor())
            .addButton(myCriticalPathAction)
            .build();
    }

    Component getComponent() {
        return createContentComponent();
    }

    protected Component createButtonPanel() {
        JToolBar buttonBar = new JToolBar();
        buttonBar.setFloatable(false);
        buttonBar.setBorderPainted(false);
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

