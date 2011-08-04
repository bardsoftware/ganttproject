/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.gui;

import java.awt.Component;
import java.awt.Frame;
import javax.swing.Action;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.undo.GPUndoManager;

/**
 * @author bard
 */
public interface UIFacade {
    public interface Dialog {
        void show();
        void hide();
    }

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

    void showOptionDialog(int messageType, String message, Action[] actions);

    Dialog createDialog(Component content, Action[] buttonActions, String title);
//    void showDialog(Component content, Action[] buttonActions);
//    void showDialog(Component content, Action[] buttonActions, String title);

    void setStatusText(String text);

    void showErrorDialog(String errorMessage);

	/**
	 * Shows the given exception in an error dialog and also puts it into the
	 * log file
	 * 
	 * @param e
	 *            the exception to show (and log)
	 */
    void showErrorDialog(Throwable e);

    NotificationManager getNotificationManager();

    void logErrorMessage(Throwable e);

    GanttChart getGanttChart();

    Chart getResourceChart();

    Chart getActiveChart();

    /**
     * @return the index of the displayed tab.
     */
    int getViewIndex();

    void setViewIndex(int viewIndex);

    int getGanttDividerLocation();

    void setGanttDividerLocation(int location);

    int getResourceDividerLocation();

    void setResourceDividerLocation(int location);

    /** Refreshes the UI (ie repaints all tasks in the chart) */
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
