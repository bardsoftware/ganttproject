/*
 * Created on 22.10.2005
 */
package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;

import javax.swing.Box;
import javax.swing.JPanel;

import org.eclipse.core.runtime.IAdaptable;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;

class ResourceChartTabContentPanel implements IAdaptable {
    private ResourceTreeUIFacade myTreeFacade;
    private final Component myResourceChart;
    private JPanel myTabContentPanel;

    ResourceChartTabContentPanel(ResourceTreeUIFacade resourceTree, Component resourceChart) {
        myTreeFacade = resourceTree;
        myResourceChart = resourceChart;
    }
    
    Component getComponent() {
    	if (myTabContentPanel==null) {
	        myTabContentPanel = new JPanel(new BorderLayout());
	        Component buttonPanel = createButtonPanel();
	        myTabContentPanel.add(buttonPanel, BorderLayout.NORTH);
	        myTabContentPanel.add(myTreeFacade.getUIComponent(), BorderLayout.CENTER);
    	}
        return myTabContentPanel;
    }

    private Component createButtonPanel() {
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
}
