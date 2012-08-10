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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.RenderedImage;
import java.util.Date;

import javax.swing.Action;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.action.view.ViewChartOptionsDialogAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartRendererBase;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartSelectionListener;
import net.sourceforge.ganttproject.chart.ChartUIConfiguration;
import net.sourceforge.ganttproject.chart.ChartViewState;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.chart.export.ChartImageVisitor;
import net.sourceforge.ganttproject.chart.mouse.MouseWheelListenerBase;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitStack;

import org.eclipse.core.runtime.IStatus;

public abstract class ChartComponentBase extends JPanel implements TimelineChart {
  private static final Cursor DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

  private final IGanttProject myProject;

  private final ZoomManager myZoomManager;

  private MouseWheelListenerBase myMouseWheelListener;

  private final UIFacade myUIFacade;

  private final ViewChartOptionsDialogAction myOptionsDialogAction;

  public ChartComponentBase(IGanttProject project, UIFacade uiFacade, ZoomManager zoomManager) {
    myProject = project;
    myUIFacade = uiFacade;
    myZoomManager = zoomManager;

    myOptionsDialogAction = new ViewChartOptionsDialogAction(this, uiFacade);

    myMouseWheelListener = new MouseWheelListenerBase(zoomManager);
  }

  @Override
  public void init(IGanttProject project) {
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
  public Chart createCopy() {
    return new AbstractChartImplementation(myProject, getUIFacade(), getChartModel().createCopy(), this);
  }

  @Override
  public ChartSelection getSelection() {
    return getImplementation().getSelection();
  }

  @Override
  public IStatus canPaste(ChartSelection selection) {
    return getImplementation().canPaste(selection);
  }

  @Override
  public void paste(ChartSelection selection) {
    getImplementation().paste(selection);
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

  public void setDefaultCursor() {
    setCursor(DEFAULT_CURSOR);
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

  // protected abstract MouseWheelListener getMouseWheelListener();

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
  public void scrollBy(TaskLength duration) {
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

  // public void paintChart(Graphics g) {
  // getImplementation().paintChart(g);
  // }
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
  public void buildImage(GanttExportSettings settings, ChartImageVisitor imageVisitor) {
    getImplementation().buildImage(settings, imageVisitor);
  }

  @Override
  public RenderedImage getRenderedImage(GanttExportSettings settings) {
    return getImplementation().getRenderedImage(settings);
  }

  @Override
  public void reset() {
    repaint();
  }

  public Action[] getPopupMenuActions() {
    return new Action[0];
  }
}