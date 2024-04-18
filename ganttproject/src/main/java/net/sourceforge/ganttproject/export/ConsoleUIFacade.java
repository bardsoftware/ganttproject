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
package net.sourceforge.ganttproject.export;

import biz.ganttproject.app.Barrier;
import biz.ganttproject.app.ResolvedBarrier;
import biz.ganttproject.app.SimpleBarrier;
import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import biz.ganttproject.lib.fx.SimpleTreeCollapseView;
import biz.ganttproject.lib.fx.TreeCollapseView;
import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.IntegerOption;
import javafx.scene.Node;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import net.sourceforge.ganttproject.action.zoom.ZoomActionSet;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.*;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.view.GPViewManager;
import net.sourceforge.ganttproject.gui.view.ViewProvider;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.TaskView;
import net.sourceforge.ganttproject.undo.GPUndoManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ConsoleUIFacade implements UIFacade {
  private final UIFacade myRealFacade;
  private final TreeCollapseView<Task> myTaskCollapseView = new SimpleTreeCollapseView<>();
  private final TaskView myTaskView = new TaskView();

  public ConsoleUIFacade(UIFacade realFacade) {
    myRealFacade = realFacade;
  }

  @Override
  public IntegerOption getDpiOption() {
    return null;
  }

  @Override
  public GPOption<String> getLafOption() {
    return null;
  }

  @Override
  public ScrollingManager getScrollingManager() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ZoomManager getZoomManager() {
    return myRealFacade != null ? myRealFacade.getZoomManager() : new ZoomManager(new GPTimeUnitStack());
  }

  @Override
  public ZoomActionSet getZoomActionSet() {
    return myRealFacade.getZoomActionSet();
  }

  @Override
  public void showPopupMenu(Component invoker, Action[] actions, int x, int y) {
  }

  @Override
  public void showPopupMenu(Component invoker, Collection<Action> actions, int x, int y) {
  }

  @Override
  public Dialog createDialog(JComponent content, Action[] buttonActions, String title) {
    return null;
  }

  @Override
  public void showOptionDialog(int messageType, String message, Action[] actions) {
    System.err.println("[ConsoleUIFacade]: " + message);
  }

  @Override
  public void showErrorDialog(String errorMessage) {
    System.err.println("[ConsoleUIFacade] ERROR: " + errorMessage);
  }

  @Override
  public void showNotificationDialog(NotificationChannel channel, String message) {
    System.err.println("[ConsoleUIFacade] " + channel.toString() + ": " + message);
  }

  @Override
  public void showErrorDialog(Throwable e) {
    System.err.println("[ConsoleUIFacade] ERROR: " + e.getMessage());
    e.printStackTrace();
  }

  public void showSettingsDialog(String pageID) {
    // TODO Auto-generated method stub

  }

  @Override
  public GanttChart getGanttChart() {
    return myRealFacade.getGanttChart();
  }

  @Override
  public TimelineChart getResourceChart() {
    return myRealFacade.getResourceChart();
  }

  @Override
  public Chart getActiveChart() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getViewIndex() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void setViewIndex(int viewIndex) {
    // TODO Auto-generated method stub
  }

    @Override
    public GPViewManager getViewManager() {
        return null;
    }

    @Override
  public void refresh() {
    // TODO Auto-generated method stub
  }

    @Override
    public void onWindowOpened(Runnable code) {

    }

    @Override
    public SimpleBarrier<Boolean> getWindowOpenedBarrier() {
        return null;
    }

    @Override
  public Image getLogo() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setWorkbenchTitle(String title) {
    // TODO Auto-generated method stub
  }

  @Override
  public GPUndoManager getUndoManager() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TaskView getCurrentTaskView() {
    return myTaskView;
  }

//  @Override
//  public TaskTreeUIFacade getTaskTree() {
//    return myRealFacade.getTaskTree();
//  }

  @Override
  public TreeCollapseView<Task> getTaskCollapseView() {
    return myTaskCollapseView;
  }

  @Override
  public ColumnList getTaskColumnList() {
    return myRealFacade.getTaskColumnList();
  }

  @Override
  public ResourceTreeUIFacade getResourceTree() {
    return myRealFacade.getResourceTree();
  }

  @Override
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
  public DefaultEnumerationOption<Locale> getLanguageOption() {
    return null;
  }

  @Override
  public GPOptionGroup[] getOptions() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addOnUpdateComponentTreeUi(Runnable callback) {
  }

  @Override
  public GanttLookAndFeelInfo getLookAndFeel() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NotificationManager getNotificationManager() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ViewProvider getGanttViewProvider() {
    return new ViewProviderStub();
  }

  @Override
  public ViewProvider getResourceViewProvider() {
    return null;
  }

  @Override
  public Barrier<Boolean> quitApplication(boolean withSystemExit) {
    if (withSystemExit) {
      System.exit(0);
      return new ResolvedBarrier<>(true);
    } else {
      return new ResolvedBarrier<>(false);
    }
  }
}

class ViewProviderStub implements ViewProvider {

  @NotNull
  @Override
  public List<GPOption<?>> getOptions() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Chart getChart() {
    return null;
  }

  @Nullable
  @Override
  public Node getNode() {
    return null;
  }

  @NotNull
  @Override
  public String getId() {
    return "";
  }

  @NotNull
  @Override
  public Function0<Unit> getRefresh() {
    return () -> null;
  }
}