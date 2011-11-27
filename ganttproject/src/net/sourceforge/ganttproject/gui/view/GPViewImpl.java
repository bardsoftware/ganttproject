package net.sourceforge.ganttproject.gui.view;

import java.awt.Container;

import javax.swing.Icon;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartSelectionListener;
import net.sourceforge.ganttproject.gui.GanttTabbedPane;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;

class GPViewImpl implements GPView, ChartSelectionListener, GanttLanguage.Listener {
    private final GanttTabbedPane myTabs;

    private int myIndex;

    private Container myComponent;

    private boolean isVisible;

    private final Icon myIcon;

    private final Chart myChart;

    private final ViewManagerImpl myManager;

    GPViewImpl(ViewManagerImpl manager, GanttTabbedPane tabs, Container component, Chart chart, Icon icon) {
        myManager = manager;
        myTabs = tabs;
        myComponent = component;
        myIcon = icon;
        myChart = chart;
        GanttLanguage.getInstance().addListener(this);
        assert myChart!=null;
    }

    public void setActive(boolean active) {
        if (active) {
            myChart.addSelectionListener(this);
        }
        else {
            myChart.removeSelectionListener(this);
        }
    }

    public void reset() {
        myChart.reset();
    }

    public void setVisible(boolean isVisible) {
        if (isVisible) {
            String tabName = myChart.getName();
            myTabs.addTab(tabName, myIcon, myComponent, tabName, this);
            myTabs.setSelectedComponent(myComponent);
            myIndex = myTabs.getSelectedIndex();

        } else {
            myTabs.remove(myIndex);
        }
        this.isVisible = isVisible;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void selectionChanged() {
        myManager.updateActions();
    }

    public Chart getChart() {
        return myChart;
    }

    public void languageChanged(Event event) {
        if(isVisible()) {
            myTabs.setTitleAt(myIndex, myChart.getName());
        }
    }
}