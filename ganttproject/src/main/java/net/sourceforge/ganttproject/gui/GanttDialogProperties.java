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
package net.sourceforge.ganttproject.gui;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import javax.swing.*;

import biz.ganttproject.FXUtil;
import biz.ganttproject.FXUtilKt;
import biz.ganttproject.app.DialogKt;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.*;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.taskproperties.TaskPropertiesController;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

public class GanttDialogProperties {
  private final GanttTask[] myTasks;

  public GanttDialogProperties(GanttTask[] tasks) {
    myTasks = tasks;
  }

  public void show(final IGanttProject project, final UIFacade uiFacade) {
    final GanttLanguage language = GanttLanguage.getInstance();
    final GanttTaskPropertiesBean taskPropertiesBean = new GanttTaskPropertiesBean(myTasks, project, uiFacade);
    final var taskPropertiesController = new TaskPropertiesController(myTasks[0], uiFacade);

    final GPAction[] actions = new GPAction[] { new OkAction() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        uiFacade.getUndoManager().undoableEdit(language.getText("properties.changed"), () -> {
          var mutator = taskPropertiesController.save();
          taskPropertiesBean.save(mutator);
          try {
            project.getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
          } catch (TaskDependencyException e) {
            if (!GPLogger.log(e)) {
              e.printStackTrace();
            }
          }
          uiFacade.refresh();
          uiFacade.getActiveChart().focus();
        });
      }
    }, new CancelAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        uiFacade.getActiveChart().focus();
      }
    } };

    StringBuffer taskNames = new StringBuffer();
    for (int i = 0; i < myTasks.length; i++) {
      if (i > 0) {
        taskNames.append(language.getText(i + 1 == myTasks.length ? "list.separator.last" : "list.separator"));
      }
      taskNames.append(myTasks[i].getName());
    }

    final String title = MessageFormat.format(language.getText("properties.task.title"), taskNames);
    DialogKt.dialog(title, "taskProperties", dialogController -> {
      dialogController.addStyleSheet("/biz/ganttproject/app/TabPane.css");
      dialogController.addStyleSheet("/biz/ganttproject/app/Util.css");
      dialogController.addStyleClass("dlg-lock");
      var tabbedPane = new TabPane();
      tabbedPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
      tabbedPane.getSelectionModel().selectedItemProperty().subscribe(newValue -> {
        if (newValue == null) {
          return;
        }
        var tabContent = newValue.getContent();
        if (tabContent instanceof Parent) {
          FXUtilKt.walkTree((Parent)tabContent, node -> {
            if (node instanceof SwingNode) {
              var contentNode = ((SwingNode) node).getContent();
              SwingUtilities.invokeLater(() -> taskPropertiesBean.onActivate(contentNode));
            }
            return Unit.INSTANCE;
          });
        }
      });
      var insertPane = new Function2<JComponent, String, Unit>() {
        @Override
        public Unit invoke(JComponent node, String title) {
          var swingNode = new SwingNode();
          var swingNodeWrapper = new StackPane(swingNode);
          swingNodeWrapper.getStyleClass().add("tab-contents");
          SwingUtilities.invokeLater(() -> {
            swingNode.setContent(node);
            swingNodeWrapper.setPrefWidth(800.0);
          });
          tabbedPane.getTabs().add(new Tab(title, swingNodeWrapper));
          return Unit.INSTANCE;
        }
      };
      var mainPropertiesPanel = taskPropertiesController.getMainPropertiesPanel();

      try {
        tabbedPane.getTabs().add(new Tab(mainPropertiesPanel.getTitle(), mainPropertiesPanel.getFxNode()));
      } catch (Exception e) {
        e.printStackTrace();
      }
      //insertPane.invoke(taskPropertiesBean.generalPanel, language.getText("general"));
      insertPane.invoke(taskPropertiesBean.predecessorsPanel, language.getText("predecessors"));
      insertPane.invoke(taskPropertiesBean.resourcesPanel, language.getCorrectedLabel("human"));

      var customPropertyPanel = taskPropertiesBean.myCustomColumnPanel.getFxNode();
      customPropertyPanel.getStyleClass().add("tab-contents");
      var customPropertyTab = new Tab(language.getText("customColumns"), customPropertyPanel);
      tabbedPane.getTabs().add(customPropertyTab);

      dialogController.setContent(tabbedPane);
      dialogController.setupButton(actions[0], button -> null);
      dialogController.setupButton(actions[1], button -> null);

      dialogController.setOnShown(() -> { FXUtil.INSTANCE.runLater(() -> {
        dialogController.walkTree(node -> {
          if (node instanceof ButtonBar || node.getStyleClass().contains("tab-header-background") || node.getStyleClass().contains("tab-contents")) {
            ((Region)node).setBackground(new Background(new BackgroundFill(
              FXUtilKt.colorFromUiManager("Panel.background"), CornerRadii.EMPTY, Insets.EMPTY
            )));
          }
          return null;
        });
        dialogController.resize();
        return null;
      }); return null; });

      return null;
    });
    //uiFacade.createDialog(taskPropertiesBean, actions, title).show();
  }
}
