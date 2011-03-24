/*
 * Created on 26.02.2005
 */
package net.sourceforge.ganttproject.gui;

import java.awt.Component;
import java.awt.Frame;
import javax.swing.Action;
import javax.swing.JComponent;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.gui.NotificationSlider.AnimationView;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.undo.GPUndoManager;

/**
 * @author bard
 */
public interface UIFacade {
    public static class Choice {
        public static final Choice YES = new Choice();
        public static final Choice NO = new Choice();
        public static final Choice CANCEL = new Choice();
        public static final Choice OK = new Choice();
    }

    public static final int GANTT_INDEX = 0;

    public static final int RESOURCES_INDEX = 1;

    ScrollingManager getScrollingManager();

    //ChartViewState getGanttChartViewState();

    ZoomManager getZoomManager();
    GPUndoManager getUndoManager();

    void setLookAndFeel(GanttLookAndFeelInfo laf);
    GanttLookAndFeelInfo getLookAndFeel();

    Choice showConfirmationDialog(String message, String title);

    void showPopupMenu(Component invoker, Action[] actions, int x, int y);

    void showDialog(Component content, Action[] buttonActions);
    void showDialog(Component content, Action[] buttonActions, String title);

    void setStatusText(String text);

    void showErrorDialog(String errorMessage);

    void showErrorDialog(Throwable e);

    NotificationManager getNotificationManager();

    void logErrorMessage(Throwable e);

    GanttChart getGanttChart();

    Chart getResourceChart();

    Chart getActiveChart();

    /**
     * Returns the index of the displayed tab.
     *
     * @return the index of the displayed tab.
     */
    int getViewIndex();

    void setViewIndex(int viewIndex);

    int getGanttDividerLocation();

    void setGanttDividerLocation(int location);

    int getResourceDividerLocation();

    void setResourceDividerLocation(int location);

    void refresh();

    Frame getMainFrame();

    void setWorkbenchTitle(String title);

    TaskTreeUIFacade getTaskTree();

    ResourceTreeUIFacade getResourceTree();
    //void changeWorkingDirectory(File parentFile);

    TaskSelectionManager getTaskSelectionManager();
    TaskSelectionContext getTaskSelectionContext();

    GPOptionGroup getOptions();

}
