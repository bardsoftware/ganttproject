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

import biz.ganttproject.app.*;
import biz.ganttproject.core.option.*;
import biz.ganttproject.ganttview.TaskFilterActionSet;
import biz.ganttproject.ganttview.TaskTableFiltersKt;
import biz.ganttproject.ganttview.TaskViewActionSet;
import biz.ganttproject.ganttview.TaskTable;
import biz.ganttproject.task.TaskActions;
import com.google.common.base.Suppliers;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import net.sourceforge.ganttproject.action.BaselineDialogAction;
import net.sourceforge.ganttproject.action.CalculateCriticalPathAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.gantt.GanttChartSelection;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.view.ViewProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

class GanttChartTabContentPanel extends ChartTabContentPanel implements ViewProvider {
  private final JComponent myGanttChart;
  private final UIFacade myWorkbenchFacade;
  private final CalculateCriticalPathAction myCriticalPathAction;
  private final BaselineDialogAction myBaselineAction;
  private final Supplier<TaskTable> myTaskTableSupplier;
  private final TaskActions myTaskActions;
  private final Function0<Unit> myInitializationCompleted;
  private final GPObservable<GPCursor> myCursorProperty;
  private final Consumer<MenuBuilder> myContextMenuBuilder;
  private TaskTable taskTable;
  private ViewComponents myViewComponents;
  private final GanttChartSelection mySelection;
  private final DoubleOption myDividerOption = new DefaultDoubleOption("divider", 0.5);

  GanttChartTabContentPanel(IGanttProject project, UIFacade workbenchFacade,
                            JComponent ganttChart,
                            GPObservable<GPCursor> cursorProperty,
                            Consumer<MenuBuilder> contextMenuBuilder,
                            UIConfiguration uiConfiguration, Supplier<TaskTable> taskTableSupplier,
                            TaskActions taskActions, BarrierEntrance initializationPromise) {
    super(project, workbenchFacade, workbenchFacade.getGanttChart());
    myInitializationCompleted = initializationPromise.register("Task table inserted into the component tree");
    myTaskActions = taskActions;
    myTaskTableSupplier = taskTableSupplier;
    myWorkbenchFacade = workbenchFacade;
    myGanttChart = ganttChart;
    myCursorProperty = cursorProperty;
    myContextMenuBuilder = contextMenuBuilder;
    // FIXME KeyStrokes of these 2 actions are not working...
    myCriticalPathAction = new CalculateCriticalPathAction(project.getTaskManager(), uiConfiguration, workbenchFacade);
    myCriticalPathAction.putValue(GPAction.TEXT_DISPLAY, ContentDisplay.TEXT_ONLY);
    myBaselineAction = new BaselineDialogAction(project, workbenchFacade);
    myBaselineAction.putValue(GPAction.TEXT_DISPLAY, ContentDisplay.TEXT_ONLY);

    setImageHeight(() -> Double.valueOf(myViewComponents.getImage().getHeight()).intValue());
    myDividerOption.addChangeValueListener(event -> {
      if (event.getNewValue() != event.getOldValue() && event.getTriggerID() != GanttChartTabContentPanel.this
        && myViewComponents != null) {
        myViewComponents.getSplitPane().setDividerPosition(0, myDividerOption.getValue());
      }
    });
    mySelection = new GanttChartSelection(project.getTaskManager(), workbenchFacade.getTaskSelectionManager());
  }

  private FXToolbarBuilder createScheduleToolbar() {
    return new FXToolbarBuilder().withApplicationFont(FontKt.getApplicationFont())
      .addButton(myCriticalPathAction).addButton(myBaselineAction)
      .withClasses("toolbar-common", "toolbar-small", "toolbar-chart", "align-right");
  }

  /**
   * "{0} tasks hidden" -- and at the same time THE RETURN PATH.
   *
   * It used to be a Label. The only way back that existed was the placeholder button in the middle
   * of the table, and that one appears solely when the table is COMPLETELY empty
   * (TaskTable.kt:512-518). The normal case with a named view is "half of it is gone", and there
   * the placeholder never shows up. As a Hyperlink the counter that is on display anyway becomes
   * the way back: it is visible exactly when something is hidden.
   */
  private final Hyperlink filterTaskLabel = new Hyperlink();

  private final Supplier<TaskFilterActionSet> filterActions = Suppliers.memoize(() ->
    new TaskFilterActionSet(taskTable.getFilterManager(), taskTable.getCustomPropertyManager(), getProject().getProjectDatabase())
  );

  private final Supplier<TaskViewActionSet> viewActions = Suppliers.memoize(() ->
    new TaskViewActionSet(getProject().getTaskViewManager())
  );

  private FXToolbarBuilder createToolbarBuilder() {
    Button tableFilterButton = ToolbarKt.createButton(new TableButtonAction("taskTable.tableMenuFilter"), true);
    tableFilterButton.setOnAction(event -> {
      var tableFilterMenu = new ContextMenu();
      tableFilterMenu.getItems().clear();
      filterActions.get().tableFilterActions(new MenuBuilderFx(tableFilterMenu, null));
      tableFilterMenu.show(tableFilterButton, Side.BOTTOM, 0.0, 0.0);
      event.consume();
    });

    Button tableViewButton = ToolbarKt.createButton(new TableButtonAction("taskTable.tableMenuViews"), true);
    tableViewButton.setOnAction(event -> {
      var tableViewMenu = new ContextMenu();
      tableViewMenu.getItems().clear();
      viewActions.get().tableViewActions(new MenuBuilderFx(tableViewMenu, null));
      tableViewMenu.show(tableViewButton, Side.BOTTOM, 0.0, 0.0);
      event.consume();
    });

    Button tableManageColumnButton = ToolbarKt.createButton(new TableButtonAction("taskTable.tableMenuToggle"), true);
    Objects.requireNonNull(tableManageColumnButton).setOnAction(event -> {
        myTaskActions.getManageColumnsAction().actionPerformed(null);
        event.consume();
    });

    HBox filterComponent = new HBox(0, filterTaskLabel, tableViewButton, tableFilterButton, tableManageColumnButton);
    return new FXToolbarBuilder()
        .addButton(myTaskActions.getUnindentAction().asToolbarAction())
        .addButton(myTaskActions.getIndentAction().asToolbarAction())
        .addButton(myTaskActions.getMoveUpAction().asToolbarAction())
        .addButton(myTaskActions.getMoveDownAction().asToolbarAction())
        .addButton(myTaskActions.getLinkTasksAction().asToolbarAction())
        .addButton(myTaskActions.getUnlinkTasksAction().asToolbarAction())
        .addTail(filterComponent)
      //      it.toolbar.stylesheets.add("/net/sourceforge/ganttproject/ChartTabContentPanel.css")
//      it.toolbar.styleClass.remove("toolbar-big")

      .withClasses("toolbar-common", "toolbar-small", "task-filter");
  }

  @NotNull
  @Override
  public Function0<Unit> getRefresh() {
    return () -> {
      SwingUtilities.invokeLater(() -> {
        getChart().reset();
        myViewComponents.getChartNode().autosize();
      });
      return null;
    };
  }

  static class TableButtonAction extends GPAction {
    TableButtonAction(String id) {
      super(id);
      setFontAwesomeLabel(UIUtil.getFontawesomeLabel(this));
    }
    @Override
    public void actionPerformed(ActionEvent e) {
    }
  }

  @Override
  @NotNull
  public JComponent getChartComponent() {
    return myGanttChart;
  }

  private TaskTable setupTaskTable() {
    var taskTable = myTaskTableSupplier.get();
    taskTable.getHeaderHeightProperty().addListener((observable, oldValue, newValue) -> updateTimelineHeight());
    filterTaskLabel.setVisible(false);
    filterTaskLabel.setManaged(false);
    filterTaskLabel.setOnAction(event -> {
      // Both, not just one of the two: the counter does not say which of them hid what, so a way
      // back that only clears one of them would leave the person pressing without an effect.
      taskTable.getFilterManager().setActiveFilter(TaskTableFiltersKt.getVOID_FILTER());
      getProject().getTaskViewManager().showAll();
      event.consume();
    });
    taskTable.getFilterManager().getHiddenTaskCount().addListener((obs,  oldValue,  newValue) -> Platform.runLater(() -> {
      if (newValue.intValue() != 0) {
        filterTaskLabel.setText(InternationalizationCoreKt.getRootLocalizer().formatText("taskView.tasksHidden", newValue.intValue()));
        filterTaskLabel.setVisible(true);
        filterTaskLabel.setManaged(true);
      } else {
        filterTaskLabel.setText("");
        filterTaskLabel.setVisible(false);
        filterTaskLabel.setManaged(false);
      }
    }));
    return taskTable;
  }


  @Override
  public @NotNull ChartSelection getSelection() {
    return mySelection;
  }

  @Override
  public Chart getChart() {
    return myWorkbenchFacade.getGanttChart();
  }

    @Override
  public Node getNode() {
    var image = myWorkbenchFacade.getLogo();
    var fxImage = (image instanceof BufferedImage bimg) ? SwingFXUtils.toFXImage(bimg, null) : null;
    myViewComponents = ViewPaneKt.createViewComponents(
      /*toolbarBuilder=*/      () -> {
        var toolbar = createToolbarBuilder().build().getToolbar$ganttproject();
        toolbar.getStylesheets().add("/net/sourceforge/ganttproject/ChartTabContentPanel.css");
        return toolbar;
      },
      /*tableBuilder=*/        () -> {
        taskTable = setupTaskTable();
        return taskTable.getTreeTable();
      },
      /*chartToolbarBuilder=*/ () -> {
        var chartToolbarBox = new HBox();
        var navigationBar = createNavigationToolbarBuilder().build().getToolbar$ganttproject();
        navigationBar.getStylesheets().add("/net/sourceforge/ganttproject/ChartTabContentPanel.css");
        chartToolbarBox.getChildren().add(navigationBar);
        HBox.setHgrow(navigationBar, Priority.ALWAYS);
        chartToolbarBox.getChildren().add(createScheduleToolbar().build().getToolbar$ganttproject());
        return chartToolbarBox;
      },
      /*chartBuilder=*/
      this::getChartComponent,
      myCursorProperty,
      this::buildContextMenu,
      fxImage,
      myWorkbenchFacade.getDpiOption()
    );

    setHeaderHeight(() -> taskTable.getHeaderHeightProperty().intValue());
    myViewComponents.getSplitPane().getDividers().get(0).positionProperty().addListener((observable, oldValue, newValue) ->
      myDividerOption.setValue(newValue.doubleValue(), GanttChartTabContentPanel.this)
    );
    taskTable.getColumnListWidthProperty().addListener((observable, oldValue, newValue) -> {
      myViewComponents.initializeDivider(taskTable.getColumnList().getTotalWidth());
    });
    taskTable.loadDefaultColumns();
    myInitializationCompleted.invoke();
    return myViewComponents.getSplitPane();
  }

  private @NotNull Unit buildContextMenu(@NotNull MenuBuilder menuBuilder) {
    myContextMenuBuilder.accept(menuBuilder);
    return Unit.INSTANCE;
  }

  @NotNull
  @Override
  public List<GPOption<?>> getOptions() {
    var options = new ArrayList<GPOption<?>>();
    options.addAll(getProject().getTaskFilterManager().getOptions());
    // ALL named views travel in this ONE option. Not one option per view: the reader matches option
    // ids against a list built before the file is read (ProxyDocument:224), so an id it does not
    // know is dropped without a word. See TaskViews.kt.
    options.add(getProject().getTaskViewManager().getOption());
    options.add(myDividerOption);
    return options;
  }

  @Override
  public String getId() {
    return String.valueOf(UIFacade.GANTT_INDEX);
  }

  @Override
  public @NotNull GPAction getCreateAction() {
    return myTaskActions.getCreateAction();
  }

  @Override
  public @NotNull GPAction getDeleteAction() {
    return myTaskActions.getDeleteAction();
  }

  @Override
  public @NotNull GPAction getPropertiesAction() {
    return myTaskActions.getPropertiesAction();
  }
}
