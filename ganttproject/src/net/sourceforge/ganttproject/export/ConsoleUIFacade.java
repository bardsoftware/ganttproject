package net.sourceforge.ganttproject.export;

import java.awt.Component;
import java.awt.Frame;
import java.io.File;

import javax.swing.Action;
import javax.swing.JComponent;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.gui.GanttLookAndFeelInfo;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.NotificationSlider.AnimationView;
import net.sourceforge.ganttproject.gui.TaskSelectionContext;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.undo.GPUndoManager;

public class ConsoleUIFacade implements UIFacade {
    private final UIFacade myRealFacade;

    ConsoleUIFacade(UIFacade realFacade) {
        myRealFacade = realFacade;
    }
    public ScrollingManager getScrollingManager() {
        // TODO Auto-generated method stub
        return null;
    }

    public ZoomManager getZoomManager() {
        // TODO Auto-generated method stub
        return null;
    }

    public Choice showConfirmationDialog(String message, String title) {
        // TODO Auto-generated method stub
        return null;
    }

    public void showPopupMenu(Component invoker, Action[] actions, int x, int y) {
        // TODO Auto-generated method stub

    }

    public void showDialog(Component content, Action[] buttonActions) {
        // TODO Auto-generated method stub

    }
    public void showDialog(Component content, Action[] buttonActions, String title) {
        // TODO Auto-generated method stub

    }

    public void setStatusText(String text) {
        // TODO Auto-generated method stub

    }

    public void showErrorDialog(String errorMessage) {
        System.err.println("[ConsoleUIFacade] ERROR: "+errorMessage);
    }

    public void showErrorDialog(Throwable e) {
        System.err.println("[ConsoleUIFacade] ERROR: "+e.getMessage());
           e.printStackTrace();
    }

    public void logErrorMessage(Throwable e) {
        System.err.println("[ConsoleUIFacade] ERROR:"+e.getMessage());
        e.printStackTrace();
    }
    public GanttChart getGanttChart() {
        return myRealFacade.getGanttChart();
    }

    public Chart getResourceChart() {
        return myRealFacade.getResourceChart();
    }

    public Chart getActiveChart() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getViewIndex() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setViewIndex(int viewIndex) {
        // TODO Auto-generated method stub

    }

    public int getGanttDividerLocation() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setGanttDividerLocation(int location) {
        // TODO Auto-generated method stub

    }

    public int getResourceDividerLocation() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setResourceDividerLocation(int location) {
        // TODO Auto-generated method stub

    }

    public void refresh() {
        // TODO Auto-generated method stub

    }

    public Frame getMainFrame() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setWorkbenchTitle(String title) {
        // TODO Auto-generated method stub

    }

    public void changeWorkingDirectory(File parentFile) {
        // TODO Auto-generated method stub

    }
    public GPUndoManager getUndoManager() {
        // TODO Auto-generated method stub
        return null;
    }
    public TaskTreeUIFacade getTaskTree() {
        return myRealFacade.getTaskTree();
    }
    public ResourceTreeUIFacade getResourceTree() {
        return myRealFacade.getResourceTree();
    }
    public TaskSelectionContext getTaskSelectionContext() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public TaskSelectionManager getTaskSelectionManager() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public void setLookAndFeel(GanttLookAndFeelInfo laf) {
        // TODO Auto-generated method stub

    }
    @Override
    public GPOptionGroup getOptions() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public GanttLookAndFeelInfo getLookAndFeel() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public void showNotificationPopup(JComponent content, Action[] actions, AnimationView animationView) {
        // TODO Auto-generated method stub

    }

}
