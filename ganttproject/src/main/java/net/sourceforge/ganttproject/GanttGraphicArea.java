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

import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.option.DefaultColorOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionChangeListener;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.CalendarFactory;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartModelImpl;
import net.sourceforge.ganttproject.chart.ChartOptionGroup;
import net.sourceforge.ganttproject.chart.ChartViewState;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.ProjectCalendarDialogAction;
import net.sourceforge.ganttproject.chart.export.RenderedChartImage;
import net.sourceforge.ganttproject.chart.gantt.GanttChartController;
import net.sourceforge.ganttproject.chart.item.CalendarChartItem;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.gui.UIConfiguration;
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
import net.sourceforge.ganttproject.undo.GPUndoManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * Class for the graphic part of the soft
 */
public class GanttGraphicArea extends ChartComponentBase implements GanttChart, CustomPropertyListener,
    ProjectEventListener {

  static {
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    URL cursorResource = GanttGraphicArea.class.getClassLoader().getResource("icons/cursorpercent.gif");
    Image image = toolkit.getImage(cursorResource);
    CHANGE_PROGRESS_CURSOR = toolkit.createCustomCursor(image, new Point(10, 5), "CursorPercent");
  }

  public static final Cursor W_RESIZE_CURSOR = new Cursor(Cursor.W_RESIZE_CURSOR);

  public static final Cursor E_RESIZE_CURSOR = new Cursor(Cursor.E_RESIZE_CURSOR);

  public static final Cursor CHANGE_PROGRESS_CURSOR;

  private GanttChartController myChartComponentImpl;

  private final GanttTree2 tree;

  private final ChartModelImpl myChartModel;

  private final TaskManager myTaskManager;

  private GPUndoManager myUndoManager;

  private ChartViewState myViewState;

  private GanttPreviousState myBaseline;

  private final ProjectCalendarDialogAction myPublicHolidayDialogAction;

  private final ChartOptionGroup myStateDiffOptions;

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
    myStateDiffOptions = createBaselineColorOptions(myChartModel, app.getUIConfiguration());
    this.tree = ttree;
    myViewState = new ChartViewState(this, app.getUIFacade());
    app.getUIFacade().getZoomManager().addZoomListener(myViewState);

    super.setStartDate(CalendarFactory.newCalendar().getTime());
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
        RecalculateTaskScheduleAlgorithm alg = myTaskManager.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm();
        if (!alg.isRunning()) {
          try {
            alg.run(task);
          } catch (TaskDependencyException e1) {
            e1.printStackTrace();
          }
        }
      }
    });
    myPublicHolidayDialogAction = new ProjectCalendarDialogAction(getProject(), getUIFacade());
    getProject().getTaskCustomColumnManager().addListener(this);
    initMouseListeners();
  }

  @Override
  public GPOptionGroup getBaselineColorOptions() {
    return myStateDiffOptions;
  }

  @Override
  public ColorOption getTaskDefaultColorOption() {
    return myChartModel.getTaskDefaultColorOption();
  }

  @Override
  public GPOptionGroup getTaskLabelOptions() {
    return myChartModel.getTaskLabelOptions();
  }

  /** @return the preferred size of the panel. */
  @Override
  public Dimension getPreferredSize() {
    return new Dimension(465, 600);
  }

  public ChartModelImpl getMyChartModel() {
    return myChartModel;
  }

  @Override
  public String getName() {
    return GanttLanguage.getInstance().getText("gantt");
  }

  private int getHeaderHeight() {
    return getImplementation().getHeaderHeight(tree, tree.getTreeTable().getScrollPane().getViewport());
  }

  /** @return an image with the gantt chart */
  // TODO: 1.11 take into account flags "render this and don't render that"
  public BufferedImage getChart(GanttExportSettings settings) {
    RenderedChartImage renderedImage = (RenderedChartImage) getRenderedImage(settings);
    int width = renderedImage.getWidth();
    int height = renderedImage.getHeight();
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
  public Action[] getPopupMenuActions(MouseEvent e) {
    List<Action> actions = Lists.newArrayList(getOptionsDialogAction(), myPublicHolidayDialogAction);
    actions.addAll(createToggleHolidayAction(e.getX()));
    return actions.toArray(new Action[actions.size()]);
  }

  private List<Action> createToggleHolidayAction(int x) {
    List<Action> result = Lists.newArrayList();
    ChartItem chartItem = myChartModel.getChartItemWithCoordinates(x, 0);
    if (chartItem instanceof CalendarChartItem) {
      Date date = ((CalendarChartItem) chartItem).getDate();
      CalendarEvent event = getProject().getActiveCalendar().getEvent(date);
      int dayMask = getProject().getActiveCalendar().getDayMask(date);
      if ((dayMask & GPCalendar.DayMask.WEEKEND) != 0) {
        switch (dayMask & GPCalendar.DayMask.WORKING) {
        case 0:
          if (event == null) {
            result.add(CalendarEventAction.addException(getProject().getActiveCalendar(), date, getUndoManager()));
          }
          break;
        case GPCalendar.DayMask.WORKING:
          result.add(CalendarEventAction.removeException(getProject().getActiveCalendar(), date, getUndoManager()));
          break;
        }
      }
      if ((dayMask & GPCalendar.DayMask.HOLIDAY) != 0) {
        result.add(CalendarEventAction.removeHoliday(getProject().getActiveCalendar(), date, getUndoManager()));
      } else {
        if (event == null) {
          result.add(CalendarEventAction.addHoliday(getProject().getActiveCalendar(), date, getUndoManager()));
        }
      }
    }
    return result;
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
      myChartComponentImpl = new GanttChartController(getProject(), getUIFacade(), myChartModel, this, tree,
          getViewState());
    }
    return myChartComponentImpl;
  }

  public void setPreviousStateTasks(List<GanttPreviousStateTask> tasks) {
    int rowHeight = myChartModel.setBaseline(tasks);
    tree.getTable().setRowHeight(rowHeight);
  }


  @Override
  public void customPropertyChange(CustomPropertyEvent event) {
    repaint();
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

  private static ChartOptionGroup createBaselineColorOptions(ChartModelImpl chartModel, final UIConfiguration projectConfig) {
    final ColorOption myTaskAheadOfScheduleColor = new DefaultColorOption("ganttChartStateDiffColors.taskAheadOfScheduleColor", new Color(50, 229, 50));
    myTaskAheadOfScheduleColor.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        projectConfig.setEarlierPreviousTaskColor(myTaskAheadOfScheduleColor.getValue());
      }
    });
    //
    final ColorOption myTaskBehindScheduleColor = new DefaultColorOption("ganttChartStateDiffColors.taskBehindScheduleColor", new Color(229, 50, 50));
    myTaskBehindScheduleColor.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        projectConfig.setLaterPreviousTaskColor(myTaskBehindScheduleColor.getValue());
      }
    });
    //
    final ColorOption myTaskOnScheduleColor = new DefaultColorOption("ganttChartStateDiffColors.taskOnScheduleColor", Color.LIGHT_GRAY);
    myTaskOnScheduleColor.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        projectConfig.setPreviousTaskColor(myTaskOnScheduleColor.getValue());
      }
    });
    //
    return new ChartOptionGroup("ganttChartStateDiffColors", new GPOption[] { myTaskOnScheduleColor,
        myTaskAheadOfScheduleColor, myTaskBehindScheduleColor }, chartModel.getOptionEventDispatcher());
  }
}
