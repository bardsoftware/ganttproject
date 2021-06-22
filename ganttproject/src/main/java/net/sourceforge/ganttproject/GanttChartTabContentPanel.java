/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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

import biz.ganttproject.ganttview.TaskTable;
import com.google.common.base.Function;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import net.sourceforge.ganttproject.action.BaselineDialogAction;
import net.sourceforge.ganttproject.action.CalculateCriticalPathAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.overview.GPToolbar;
import net.sourceforge.ganttproject.chart.overview.ToolbarBuilder;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.view.GPView;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

class GanttChartTabContentPanel extends ChartTabContentPanel implements GPView {
  //private final Container myTaskTree;
  private final JComponent myGanttChart;
  private final TreeTableContainer myTreeFacade;
  private final UIFacade myWorkbenchFacade;
  private final CalculateCriticalPathAction myCriticalPathAction;
  private final BaselineDialogAction myBaselineAction;
  private final Supplier<TaskTable> myTaskTableSupplier;
  private JComponent myComponent;

  GanttChartTabContentPanel(IGanttProject project, UIFacade workbenchFacade, TreeTableContainer treeFacade,
                            JComponent ganttChart, UIConfiguration uiConfiguration, Supplier<TaskTable> taskTableSupplier) {
    super(project, workbenchFacade, workbenchFacade.getGanttChart());
    myTaskTableSupplier = taskTableSupplier;
    myWorkbenchFacade = workbenchFacade;
    myTreeFacade = treeFacade;
    //myTaskTree = (Container) treeFacade.getTreeComponent();
    myGanttChart = ganttChart;
    // FIXME KeyStrokes of these 2 actions are not working...
    myCriticalPathAction = new CalculateCriticalPathAction(project.getTaskManager(), uiConfiguration, workbenchFacade);
    myBaselineAction = new BaselineDialogAction(project, workbenchFacade);
    addChartPanel(createSchedulePanel());
    //addTableResizeListeners(myTaskTree, myTreeFacade.getTreeTable().getScrollPane().getViewport());
  }

  private Component createSchedulePanel() {
    return new ToolbarBuilder()
        .withDpiOption(myWorkbenchFacade.getDpiOption())
        .withLafOption(getUiFacade().getLafOption(), new Function<String, Float>() {
          @Override
          public Float apply(@Nullable String s) {
            return (s.indexOf("nimbus") >= 0) ? 2f : 1f;
          }
        })
        .withGapFactory(ToolbarBuilder.Gaps.VDASH)
        .withBackground(myWorkbenchFacade.getGanttChart().getStyle().getSpanningHeaderBackgroundColor())
        .withHeight(24)
        .addButton(myCriticalPathAction)
        .addButton(myBaselineAction)
        .build()
        .getToolbar();
  }

  JComponent getComponent() {
    if (myComponent == null) {
      myComponent = createContentComponent();
    }
    return myComponent;
  }

  @Override
  protected Component createButtonPanel() {
    ToolbarBuilder builder = new ToolbarBuilder()
        .withHeight(24)
        .withSquareButtons()
        .withDpiOption(myWorkbenchFacade.getDpiOption())
        .withLafOption(myWorkbenchFacade.getLafOption(), new Function<String, Float>() {
          @Override
          public Float apply(@Nullable String s) {
            return (s.indexOf("nimbus") >= 0) ? 2f : 1f;
          }
        });
    myTreeFacade.addToolbarActions(builder);
    final GPToolbar toolbar = builder.build();
    return toolbar.getToolbar();
  }

  @Override
  public Component getChartComponent() {
    return myGanttChart;
  }

  @Override
  protected Component getTreeComponent() {
    var jfxPanel = new JFXPanel();
    Platform.runLater(() -> {
      jfxPanel.setScene(new Scene(myTaskTableSupplier.get().getControl()));
      setMyHeaderHeight(() -> {
        return myTaskTableSupplier.get().getHeaderHeightProperty().intValue();
      });
    });
    var taskTable = myTaskTableSupplier.get();
    taskTable.getHeaderHeightProperty().addListener((observable, oldValue, newValue) -> {
      updateTimelineHeight();
    });
    taskTable.setRequestSwingFocus(() -> {
      jfxPanel.requestFocus();
      return null;
    });
    taskTable.getColumnListWidthProperty().addListener((observable, oldValue, newValue) -> {
      System.err.println("column list width="+newValue);
      jfxPanel.setSize(newValue.intValue(), jfxPanel.getHeight());
      setTableWidth(newValue.doubleValue());

    });
    return jfxPanel;
    //return myTaskTree;

  }

  // //////////////////////////////////////////////
  // GPView
  @Override
  public void setActive(boolean active) {
    if (active) {
      //myTaskTree.requestFocus();
      myTreeFacade.getNewAction().updateAction();
    }
  }

  @Override
  public Chart getChart() {
    return myWorkbenchFacade.getGanttChart();
  }

  @Override
  public Component getViewComponent() {
    return getComponent();
  }
}
