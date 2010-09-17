/*
 * Created on 22.10.2005
 */
package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.eclipse.core.runtime.IAdaptable;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.chart.overview.NavigationPanel;
import net.sourceforge.ganttproject.chart.overview.ZoomingPanel;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.UIFacade;

class ResourceChartTabContentPanel extends ChartTabContentPanel implements IAdaptable {
    private ResourceTreeUIFacade myTreeFacade;
    private Component myResourceChart;
    private JComponent myTabContentPanel;
    private NavigationPanel myNavigationPanel;
    private ZoomingPanel myZoomingPanel;

    ResourceChartTabContentPanel(IGanttProject project, UIFacade workbenchFacade, ResourceTreeUIFacade resourceTree, Component resourceChart) {
        super(project, workbenchFacade, (TimelineChart) workbenchFacade.getResourceChart());
        myTreeFacade = resourceTree;
        myResourceChart = resourceChart;
        myNavigationPanel = new NavigationPanel(project, (TimelineChart) workbenchFacade.getResourceChart(), workbenchFacade);
        myZoomingPanel = new ZoomingPanel(workbenchFacade);
    }

    Component getComponent() {
        if (myTabContentPanel==null) {
            myTabContentPanel = createContentComponent();
        }
        return myTabContentPanel;
    }

    protected Component createButtonPanel() {
        Box buttonBar = Box.createHorizontalBox();
        TestGanttRolloverButton upButton = new TestGanttRolloverButton(myTreeFacade.getMoveUpAction());
        upButton.setTextHidden(true);
        buttonBar.add(upButton);
        //
        TestGanttRolloverButton downButton = new TestGanttRolloverButton(myTreeFacade.getMoveDownAction());
        downButton.setTextHidden(true);
        buttonBar.add(downButton);
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
            return myResourceChart;
        }
        return null;
    }

    @Override
    protected Component getChartComponent() {
        return myResourceChart;
    }

    @Override
    protected Component getTreeComponent() {
        return myTreeFacade.getUIComponent();
    }

}