/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject;

import biz.ganttproject.app.GPCursor;
import biz.ganttproject.core.option.*;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.core.time.TimeUnitStack;
import biz.ganttproject.print.PrintChartApi;
import net.sourceforge.ganttproject.action.view.ViewChartOptionsDialogAction;
import net.sourceforge.ganttproject.chart.*;
import net.sourceforge.ganttproject.chart.mouse.MouseInteraction;
import net.sourceforge.ganttproject.chart.mouse.MouseWheelListenerBase;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.task.TaskManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.util.Date;

public abstract class ChartComponentBase extends JPanel implements TimelineChart {

  private final ObservableImpl<GPCursor> myCursorProperty = new ObservableImpl<>(GPCursor.Default);
  private final IGanttProject myProject;

  private final ZoomManager myZoomManager;

  private final MouseWheelListenerBase myMouseWheelListener;

  private final UIFacade myUIFacade;

  private final ViewChartOptionsDialogAction myOptionsDialogAction;

  public ChartComponentBase(IGanttProject project, UIFacade uiFacade, ZoomManager zoomManager) {
    myProject = project;
    myUIFacade = uiFacade;
    myZoomManager = zoomManager;

    myOptionsDialogAction = new ViewChartOptionsDialogAction(this, uiFacade);

    myMouseWheelListener = new MouseWheelListenerBase(zoomManager) {
      @Override
      protected void fireScroll(MouseWheelEvent e) {
        MouseInteraction activeInteraction = getImplementation().getActiveInteraction();
        if (activeInteraction == null) {
          getImplementation().beginScrollViewInteraction(e);
          requestFocus();
        } else {
          activeInteraction.apply(e);
          getImplementation().reset();
        }
      }
    };
    myProject.getActiveCalendar().addListener(() -> {
      getChartModel().resetOffsets();
      reset();
    });
  }

  @Override
  public void init(IGanttProject project, IntegerOption dpiOption, FontOption chartFontOption) {
    // Skip as we already have a project instance.
  }

  protected void initMouseListeners() {
    addMouseListener(getMouseListener());
    addMouseMotionListener(getMouseMotionListener());
    addMouseWheelListener(myMouseWheelListener);
  }

  @Override
  public Object getAdapter(Class adapter) {
    if (Component.class.isAssignableFrom(adapter)) {
      return this;
    }
    return null;
  }

  public GPObservable<GPCursor> getCursorProperty() {
    return myCursorProperty;
  }

  public abstract ChartViewState getViewState();

  public ZoomListener getZoomListener() {
    return getImplementation();
  }

  public ZoomManager getZoomManager() {
    return myZoomManager;
  }

  @Override
  public GPOptionGroup[] getOptionGroups() {
    return getChartModel().getChartOptionGroups();
  }

  @Override
  public void addSelectionListener(ChartSelectionListener listener) {
    getImplementation().addSelectionListener(listener);
  }

  @Override
  public void removeSelectionListener(ChartSelectionListener listener) {
    getImplementation().removeSelectionListener(listener);
  }

  protected abstract GPTreeTableBase getTreeTable();

  protected UIFacade getUIFacade() {
    return myUIFacade;
  }

  protected TaskManager getTaskManager() {
    return myProject.getTaskManager();
  }

  protected TimeUnitStack getTimeUnitStack() {
    return myProject.getTimeUnitStack();
  }

  protected UIConfiguration getUIConfiguration() {
    return myProject.getUIConfiguration();
  }

  public Action getOptionsDialogAction() {
    return myOptionsDialogAction;
  }

  @Override
  public ChartModel getModel() {
    return getChartModel();
  }

  @Override
  public ChartUIConfiguration getStyle() {
    return getChartModel().getChartUIConfiguration();
  }

  protected abstract ChartModelBase getChartModel();

  protected abstract MouseListener getMouseListener();

  protected abstract MouseMotionListener getMouseMotionListener();

  protected abstract AbstractChartImplementation getImplementation();

  @Override
  public Date getStartDate() {
    return getImplementation().getStartDate();
  }

  @Override
  public void setStartDate(Date startDate) {
    getImplementation().setStartDate(startDate);
    repaint();
  }

  @Override
  public IGanttProject getProject() {
    return myProject;
  }

  @Override
  public Date getEndDate() {
    return getImplementation().getEndDate();
  }

  @Override
  public void setVScrollController(VScrollController vscrollController) {
    getImplementation().setVScrollController(vscrollController);
  }

  @Override
  public void scrollBy(TimeDuration duration) {
    getImplementation().scrollBy(duration);
    repaint();
  }

  @Override
  public void setStartOffset(int pixels) {
    getImplementation().setStartOffset(pixels);
    repaint();
  }

  @Override
  public void setDimensions(int height, int width) {
    getImplementation().setDimensions(height, width);
  }

  @Override
  public void setBottomUnit(TimeUnit bottomUnit) {
    getImplementation().setBottomUnit(bottomUnit);
  }

  @Override
  public void setTopUnit(TimeUnit topUnit) {
    getImplementation().setTopUnit(topUnit);
  }

  @Override
  public void setBottomUnitWidth(int width) {
    getImplementation().setBottomUnitWidth(width);
  }

  @Override
  public void addRenderer(ChartRendererBase renderer) {
    getImplementation().addRenderer(renderer);
  }

  @Override
  public void resetRenderers() {
    getImplementation().resetRenderers();
  }

  /** draw the panel */
  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    getChartModel().setBounds(getSize());
    getImplementation().paintChart(g);
  }

  @Override
  public void reset() {
    repaint();
  }

  public Action[] getPopupMenuActions(MouseEvent e) {
    return new Action[0];
  }

  JComponent getJComponent() {
    return new JLayer<>(this, getImplementation().createMouseHoverLayer());
  }

  @Override
  public void setTimelineHeight(int height) {
    getImplementation().setTimelineHeight(height);
  }

  @Override
  public PrintChartApi asPrintChartApi() {
    return getImplementation().asPrintChartApi();
  }
}
