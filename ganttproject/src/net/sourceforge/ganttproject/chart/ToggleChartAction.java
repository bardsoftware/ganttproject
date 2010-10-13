/*
 * Created on 20.05.2005
 */
package net.sourceforge.ganttproject.chart;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import net.sourceforge.ganttproject.GPView;
import net.sourceforge.ganttproject.GPViewManager;
import net.sourceforge.ganttproject.action.GPAction;

/**
 * @author bard
 */
public class ToggleChartAction extends GPAction {
    private Chart myChart;

    private GPView myView;
    
    GPViewManager myViewManager;

    public ToggleChartAction(Chart chart, GPViewManager viewManager) {
        myChart = chart;
        putValue(Action.NAME, getLocalizedName());
        // putValue(Action.SMALL_ICON,chart.getIcon());
        myView = viewManager.createView(chart, chart.getIcon());
        myViewManager = viewManager;
    }

    protected String getLocalizedName() {
        return myChart == null ? null : myChart.getName();
    }

    protected String getIconFilePrefix() {
        return null;
    }

    public void actionPerformed(ActionEvent e) {
        System.err
                .println("[ToggleChartAction] actionPerformed: toggling chart="
                        + myChart.getName());
        myView.setVisible(!myView.isVisible());
    }

}
