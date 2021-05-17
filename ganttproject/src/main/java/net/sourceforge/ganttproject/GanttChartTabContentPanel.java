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

import com.google.common.base.Function;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import net.sourceforge.ganttproject.action.BaselineDialogAction;
import net.sourceforge.ganttproject.action.CalculateCriticalPathAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.overview.GPToolbar;
import net.sourceforge.ganttproject.chart.overview.ToolbarBuilder;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.view.GPView;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManagerImpl;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

class GanttChartTabContentPanel extends ChartTabContentPanel implements GPView {
  private final Container myTaskTree;
  private final JComponent myGanttChart;
  private final TreeTableContainer myTreeFacade;
  private final UIFacade myWorkbenchFacade;
  private final CalculateCriticalPathAction myCriticalPathAction;
  private final BaselineDialogAction myBaselineAction;
  private final IGanttProject myProject;
  private JComponent myComponent;

  GanttChartTabContentPanel(IGanttProject project, UIFacade workbenchFacade, TreeTableContainer treeFacade,
      JComponent ganttChart, UIConfiguration uiConfiguration) {
    super(project, workbenchFacade, workbenchFacade.getGanttChart());
    myProject = project;
    myWorkbenchFacade = workbenchFacade;
    myTreeFacade = treeFacade;
    myTaskTree = (Container) treeFacade.getTreeComponent();
    myGanttChart = ganttChart;
    // FIXME KeyStrokes of these 2 actions are not working...
    myCriticalPathAction = new CalculateCriticalPathAction(project.getTaskManager(), uiConfiguration, workbenchFacade);
    myBaselineAction = new BaselineDialogAction(project, workbenchFacade);
    addChartPanel(createSchedulePanel());
    addTableResizeListeners(myTaskTree, myTreeFacade.getTreeTable().getScrollPane().getViewport());
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
      var treeModel = new TaskManagerImpl.FacadeImpl(myProject.getTaskManager().getRootTask());
      var rootItem = new TreeItem<>(treeModel.getRootTask());
      var treeTable = new TreeTableView<>(rootItem);
      treeTable.getColumns().add(new TreeTableColumn<Task, String>("Name"));
      treeTable.getColumns().add(new TreeTableColumn<Task, String>("Begin"));
      myProject.addProjectEventListener(new ProjectEventListener.Stub() {
        @Override
        public void projectOpened() {
          Platform.runLater(() -> {
            var treeModel = new TaskManagerImpl.FacadeImpl(myProject.getTaskManager().getRootTask());
            treeTable.getRoot().getChildren().clear();
            var task2treeItem = new HashMap<Task, TreeItem<Task>>();
            task2treeItem.put(treeModel.getRootTask(), rootItem);
            treeModel.breadthFirstSearch(treeModel.getRootTask(), (pair) -> {
              if (pair.first() == null) {
                return true;
              }
              var parentItem = task2treeItem.get(pair.first());
              var childItem = new TreeItem<>(pair.second());
              parentItem.getChildren().add(childItem);
              task2treeItem.put(pair.second(), childItem);
              return true;
            });
          });
        }
      });
      jfxPanel.setScene(new Scene(treeTable));
    });
    return jfxPanel;
    //return myTaskTree;

  }

  // //////////////////////////////////////////////
  // GPView
  @Override
  public void setActive(boolean active) {
    if (active) {
      myTaskTree.requestFocus();
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
