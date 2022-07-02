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

import biz.ganttproject.app.Barrier;
import biz.ganttproject.app.BarrierEntrance;
import biz.ganttproject.app.TimerBarrier;
import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.option.DefaultColorOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.customproperty.CustomPropertyListener;
import biz.ganttproject.ganttview.TaskTableActionConnector;
import biz.ganttproject.ganttview.TaskTableChartConnector;
import com.google.common.collect.Lists;
import kotlin.Unit;
import net.sourceforge.ganttproject.chart.*;
import net.sourceforge.ganttproject.chart.gantt.GanttChartController;
import net.sourceforge.ganttproject.chart.item.CalendarChartItem;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import biz.ganttproject.customproperty.CustomPropertyEvent;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.undo.GPUndoManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static net.sourceforge.ganttproject.task.event.TaskListenerAdapterKt.createTaskListenerWithTimerBarrier;

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
  private final TaskTableChartConnector taskTableChartConnector;
  private final Supplier<TaskTableActionConnector> taskTableActionFacade;

  private GanttChartController myChartComponentImpl;

  //private final GanttTree2 tree;

  private final ChartModelImpl myChartModel;

  private final GPUndoManager myUndoManager;

  private final ChartViewState myViewState;

  private GanttPreviousState myBaseline;

  private final ProjectCalendarDialogAction myPublicHolidayDialogAction;

  private final ChartOptionGroup myStateDiffOptions;

  public GanttGraphicArea(GanttProject app, TaskManager taskManager, ZoomManager zoomManager,
                          GPUndoManager undoManager, TaskTableChartConnector taskTableChartConnector,
                          Supplier<TaskTableActionConnector> taskTableActionFacade) {
    super(app.getProject(), app.getUIFacade(), zoomManager);
    this.setBackground(Color.WHITE);
    this.taskTableChartConnector = taskTableChartConnector;
    this.taskTableActionFacade = taskTableActionFacade;
    myUndoManager = undoManager;

    myChartModel = new ChartModelImpl(getTaskManager(), app.getTimeUnitStack(), app.getUIConfiguration());
    myChartModel.addOptionChangeListener(this::repaint);
    myStateDiffOptions = createBaselineColorOptions(myChartModel, app.getUIConfiguration());
    //this.tree = ttree;
    myViewState = new ChartViewState(this, app.getUIFacade());
    app.getUIFacade().getZoomManager().addZoomListener(myViewState);

    super.setStartDate(CalendarFactory.newCalendar().getTime());
    var timerBarrier = new TimerBarrier(1000);
    timerBarrier.await((unit) -> {
      SwingUtilities.invokeLater(this::reset);
      return Unit.INSTANCE;
    });
    taskManager.addTaskListener(createTaskListenerWithTimerBarrier(timerBarrier));
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

  @Override
  public void focus() {
    taskTableChartConnector.getFocus().invoke();
  }

  private int getHeaderHeight() {
    return getImplementation().getHeaderHeight();
  }

  @Override
  protected GPTreeTableBase getTreeTable() {
    //return tree.getTreeTable();
    return null;
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
    return actions.toArray(new Action[0]);
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

  @Override
  protected AbstractChartImplementation getImplementation() {
    return getChartImplementation();
  }

  GanttChartController getChartImplementation() {
    if (myChartComponentImpl == null) {
      myChartComponentImpl = new GanttChartController(getProject(), getUIFacade(), myChartModel, this,
          getViewState(), this.taskTableChartConnector, this.taskTableActionFacade);
    }
    return myChartComponentImpl;
  }

  public void setPreviousStateTasks(List<GanttPreviousStateTask> tasks) {
    int rowHeight = myChartModel.setBaseline(tasks);
    taskTableChartConnector.getRowHeight().setValue(Math.max(
        rowHeight + 0.0, taskTableChartConnector.getMinRowHeight().getValue()
    ));
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
  public void projectOpened(BarrierEntrance barrierRegistry, Barrier<IGanttProject> barrier) {
  }

  @Override
  public void projectCreated() {
    // TODO Auto-generated method stub

  }

  @Override
  public void projectRestoring(Barrier<Document> completion) {
  }

  @Override
  public ChartViewState getViewState() {
    return myViewState;
  }

  private static ChartOptionGroup createBaselineColorOptions(ChartModelImpl chartModel, final UIConfiguration projectConfig) {
    final ColorOption myTaskAheadOfScheduleColor = new DefaultColorOption("ganttChartStateDiffColors.taskAheadOfScheduleColor", new Color(50, 229, 50));
    myTaskAheadOfScheduleColor.addChangeValueListener(evt -> projectConfig.setEarlierPreviousTaskColor(myTaskAheadOfScheduleColor.getValue()));
    //
    final ColorOption myTaskBehindScheduleColor = new DefaultColorOption("ganttChartStateDiffColors.taskBehindScheduleColor", new Color(229, 50, 50));
    myTaskBehindScheduleColor.addChangeValueListener(evt -> projectConfig.setLaterPreviousTaskColor(myTaskBehindScheduleColor.getValue()));
    //
    final ColorOption myTaskOnScheduleColor = new DefaultColorOption("ganttChartStateDiffColors.taskOnScheduleColor", Color.LIGHT_GRAY);
    myTaskOnScheduleColor.addChangeValueListener(evt -> projectConfig.setPreviousTaskColor(myTaskOnScheduleColor.getValue()));
    //
    return new ChartOptionGroup("ganttChartStateDiffColors", new GPOption[] { myTaskOnScheduleColor,
        myTaskAheadOfScheduleColor, myTaskBehindScheduleColor }, chartModel.getOptionEventDispatcher());
  }
}
