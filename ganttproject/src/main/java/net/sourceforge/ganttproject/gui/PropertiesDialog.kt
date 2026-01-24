/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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
package net.sourceforge.ganttproject.gui

import biz.ganttproject.app.DialogController
import biz.ganttproject.app.ErrorPane
import biz.ganttproject.app.dialog
import biz.ganttproject.app.setSwingBackground
import javafx.collections.ObservableList
import javafx.embed.swing.SwingNode
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.StackPane
import net.sourceforge.ganttproject.action.GPAction
import javax.swing.JComponent
import javax.swing.SwingUtilities

data class PropertiesDialogTabProvider(
  val insertTab: (TabPane)->Unit,
  val requestFocus: ()->Unit
)

fun swingTab(title: String, createContents: ()->JComponent): PropertiesDialogTabProvider {
  return PropertiesDialogTabProvider(
    insertTab = { tabbedPane ->
      val swingNode = SwingNode()
      var swingNodeWrapper = StackPane(swingNode)
      swingNodeWrapper.styleClass.add("tab-contents")
      SwingUtilities.invokeLater {
        swingNode.setContent(createContents())
        swingNodeWrapper.prefWidth = 800.0
      }
      tabbedPane.tabs.add(Tab(title, swingNodeWrapper))
    },
    requestFocus = {
    }
  )
}

/**
 * Builds a properties dialog using the provided components:
 * - a list of dialog actions.
 * - a list of tabs to be inserted into the tab pane.
 * - an observable list of validation errors to be shown in the dialog UI.
 */
fun propertiesDialog(title: String, id: String, actions: List<GPAction>, validationErrors: ObservableList<String>,
                     tabProviders: List<PropertiesDialogTabProvider>, setupCode: (DialogController)->Unit={}) {
  dialog(title, id) { dialogController ->
    dialogController.addStyleSheet("/biz/ganttproject/app/ErrorPane.css");
    dialogController.addStyleSheet("/biz/ganttproject/app/TabPane.css");
    dialogController.addStyleSheet("/biz/ganttproject/app/Util.css");
    dialogController.addStyleClass("dlg-lock");
    var tabbedPane = TabPane();
    tabbedPane.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE;

    var errorPane = ErrorPane();

    validationErrors.subscribe {
      val errors = validationErrors
      if (errors.isEmpty()) {
        errorPane.onError(null)
      } else {
        dialogController.setButtonPaneNode(errorPane.fxNode)
        errorPane.onError(errors.first())

      }
    }

    tabProviders.forEach {
      it.insertTab(tabbedPane)
    }

    dialogController.setContent(tabbedPane);
    dialogController.setupButton(actions[0]);
    dialogController.setupButton(actions[1]);
    //dialogController.setButtonPaneNode(errorPane.fxNode);
    dialogController.setEscCloseEnabled(true)

    // This is hack for the dialogs showing Swing components. We set the background color of the dialog panes
    // to be the same as the Panel background in the current Swing LAF.
    dialogController.onShown = {
      dialogController.setSwingBackground()
      tabProviders.first().requestFocus()
      dialogController.resize()
    }
  }
}
