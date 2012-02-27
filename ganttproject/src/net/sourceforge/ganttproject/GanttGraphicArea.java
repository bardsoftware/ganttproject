/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;

import javax.swing.Action;

import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartModelImpl;
import net.sourceforge.ganttproject.chart.ChartViewState;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.PublicHolidayDialogAction;
import net.sourceforge.ganttproject.chart.export.RenderedChartImage;
import net.sourceforge.ganttproject.chart.gantt.GanttChartController;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.model.GPOptionChangeListener;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.CustomPropertyEvent;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskScheduleAlgorithm;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.event.TaskDependencyEvent;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent;
import net.sourceforge.ganttproject.time.gregorian.GregorianCalendar;
import net.sourceforge.ganttproject.undo.GPUndoManager;


/**
 * Class for the graphic part of the soft
 */
public class GanttGraphicArea extends ChartComponentBase implements GanttChart,
        CustomPropertyListener, ProjectEventListener {

    static {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        URL cursorResource = GanttGraphicArea.class.getClassLoader()
                .getResource("icons/cursorpercent.gif");
        Image image = toolkit.getImage(cursorResource);
        CHANGE_PROGRESS_CURSOR = toolkit.createCustomCursor(image, new Point(
                10, 5), "CursorPercent");
    }

    public static final Cursor W_RESIZE_CURSOR = new Cursor(
            Cursor.W_RESIZE_CURSOR);

    public static final Cursor E_RESIZE_CURSOR = new Cursor(
            Cursor.E_RESIZE_CURSOR);

    public static final Cursor CHANGE_PROGRESS_CURSOR;

    private final GanttTree2 tree;

    public static Color taskDefaultColor = new Color(140, 182, 206);


    private UIConfiguration myUIConfiguration;

    private final ChartModelImpl myChartModel;

    private final TaskManager myTaskManager;

    private GPUndoManager myUndoManager;

    private ChartViewState myViewState;

    private GanttPreviousState myBaseline;

    private final PublicHolidayDialogAction myPublicHolidayDialogAction;

    public GanttGraphicArea(GanttProject app, GanttTree2 ttree, TaskManager taskManager, ZoomManager zoomManager,
            GPUndoManager undoManager) {
        super(app.getProject(), app.getUIFacade(), zoomManager);
        this.setBackground(Color.WHITE);
        myTaskManager = taskManager;
        myUndoManager = undoManager;

        myChartModel = new ChartModelImpl(getTaskManager(), app.getTimeUnitStack(), app.getUIConfiguration());
        myChartModel.addOptionChangeListener(new GPOptionChangeListener() {
            @Override
            public void optionsChanged() {
                repaint();
            }
        });
        this.tree = ttree;
        myViewState = new ChartViewState(this, app.getUIFacade());
        app.getUIFacade().getZoomManager().addZoomListener(myViewState);

        super.setStartDate(GregorianCalendar.getInstance().getTime());
        myTaskManager.addTaskListener(new TaskListenerAdapter() {
            @Override
            public void taskScheduleChanged(TaskScheduleEvent e) {
                adjustDependencies((Task) e.getSource());
            }

            @Override
            public void dependencyAdded(TaskDependencyEvent e) {
                adjustDependencies(e.getDependency().getDependee());
                repaint();
            }

            @Override
            public void dependencyRemoved(TaskDependencyEvent e) {
                repaint();
            }

            private void adjustDependencies(Task task) {
                RecalculateTaskScheduleAlgorithm alg = myTaskManager
                        .getAlgorithmCollection()
                        .getRecalculateTaskScheduleAlgorithm();
                if (!alg.isRunning()) {
                    try {
                        alg.run(task);
                    } catch (TaskDependencyException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        myPublicHolidayDialogAction = new PublicHolidayDialogAction(getProject(), getUIFacade());
        getProject().getTaskCustomColumnManager().addListener(this);
        initMouseListeners();
    }

    /** @return the color of the task */
    public Color getTaskColor() {
        return myUIConfiguration.getTaskColor();
    }

    /** Change the color of the task */
    public void setProjectLevelTaskColor(Color c) {
        myUIConfiguration.setProjectLevelTaskColor(c);
    }

    /** @return the preferred size of the panel. */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(465, 600);
    }

    public ChartModelImpl getMyChartModel() {
        return myChartModel;
    }

    public void drawGPVersion(Graphics g) {
        g.setColor(Color.black);
        g.setFont(Fonts.GP_VERSION_FONT);
        g.drawString("GanttProject (" + GanttProject.version + ")", 3,
                getHeight() + 18);
    }

    @Override
    public String getName() {
        return GanttLanguage.getInstance().getText("gantt");
    }

    private int getHeaderHeight() {
        return getImplementation().getHeaderHeight(tree, tree.getTreeTable().getTable());
    }

    /** @return an image with the gantt chart */
    // TODO: 1.11 take into account flags "render this and don't render that"
    public BufferedImage getChart(GanttExportSettings settings) {
        RenderedChartImage renderedImage = (RenderedChartImage) getRenderedImage(settings);
        int width = renderedImage.getWidth();
        int height = renderedImage.getHeight();
        System.err.println("width="+width+" height="+height);
        BufferedImage result = renderedImage.getWholeImage();
        repaint();
        return result;
    }

    @Override
    protected GPTreeTableBase getTreeTable() {
        return tree.getTreeTable();
    }

    GPUndoManager getUndoManager() {
        return myUndoManager;
    }

    @Override
    protected ChartModelBase getChartModel() {
        return myChartModel;
    }

    @Override
    protected MouseListener getMouseListener() {
        return getChartImplementation().getMouseListener();
    }

    @Override
    protected MouseMotionListener getMouseMotionListener() {
        return getChartImplementation().getMouseMotionListener();
    }

    @Override
    public Action[] getPopupMenuActions() {
        return new Action[] { getOptionsDialogAction(), myPublicHolidayDialogAction };
    }

    @Override
    public void repaint() {
        if (myChartModel != null && isShowing()) {
            myChartModel.setHeaderHeight(getHeaderHeight());
        }
        super.repaint();
    }

    @Override
    public void setBaseline(GanttPreviousState baseline) {
        if (baseline == null) {
            setPreviousStateTasks(null);
        } else {
            setPreviousStateTasks(baseline.load());
        }
        myBaseline = baseline;
    }

    @Override
    public GanttPreviousState getBaseline() {
        return myBaseline;
    }



    static class MouseSupport {
        private final ChartModelImpl myChartModel;

        MouseSupport(ChartModelImpl chartModel) {
            myChartModel = chartModel;
        }
        protected Task findTaskUnderMousePointer(int xpos, int ypos) {
            ChartItem chartItem = myChartModel.getChartItemWithCoordinates(xpos, ypos);
            return chartItem == null ? null : chartItem.getTask();
        }

        protected ChartItem getChartItemUnderMousePoint(int xpos, int ypos) {
            ChartItem result = myChartModel.getChartItemWithCoordinates(xpos, ypos);
            return result;
        }
    }

    @Override
    protected AbstractChartImplementation getImplementation() {
        return getChartImplementation();
    }

    GanttChartController getChartImplementation() {
        if (myChartComponentImpl == null) {
            myChartComponentImpl = new GanttChartController(
                    getProject(), getUIFacade(), myChartModel, this, tree, getViewState());
        }
        return myChartComponentImpl;
    }

    public void setPreviousStateTasks(List<GanttPreviousStateTask> tasks) {
        int rowHeight = myChartModel.setBaseline(tasks);
        tree.getTable().setRowHeight(rowHeight);
    }

    private GanttChartController myChartComponentImpl;

    @Override
    public void customPropertyChange(CustomPropertyEvent event) {
        repaint();
    }

    public void setUIConfiguration(UIConfiguration configuration) {
        myUIConfiguration = configuration;
    }

    @Override
    public void projectModified() {
        // TODO Auto-generated method stub
    }

    @Override
    public void projectSaved() {
        // TODO Auto-generated method stub
    }

    @Override
    public void projectClosed() {
        repaint();
        setProjectLevelTaskColor(null);
        setPreviousStateTasks(null);
    }
    @Override

    public void projectOpened() {
    }

    @Override
    public void projectCreated() {
        // TODO Auto-generated method stub

    }
    @Override
    public ChartViewState getViewState() {
        return myViewState;
    }

}
