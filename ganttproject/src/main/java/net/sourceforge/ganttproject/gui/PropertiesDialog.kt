package net.sourceforge.ganttproject.gui

import biz.ganttproject.app.ErrorPane
import biz.ganttproject.app.dialog
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.scene.control.ButtonBar
import javafx.scene.control.TabPane
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Region
import net.sourceforge.ganttproject.action.GPAction
import biz.ganttproject.colorFromUiManager
import javafx.embed.swing.SwingNode
import javafx.scene.control.Tab
import javafx.scene.layout.StackPane
import javax.swing.JComponent
import javax.swing.SwingUtilities

data class PropertiedDialogTabProvider(
  val insertTab: (TabPane)->Unit,
  val requestFocus: ()->Unit
)

fun swingTab(title: String, createContents: ()->JComponent): PropertiedDialogTabProvider {
  return PropertiedDialogTabProvider(
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

fun propertiesDialog(title: String, id: String, actions: List<GPAction>, validationErrors: ObservableList<String>, tabProviders: List<PropertiedDialogTabProvider>) {
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
        errorPane.onError(errors.first())
      }
    }

    tabProviders.forEach {
      it.insertTab(tabbedPane)
    }

    dialogController.setContent(tabbedPane);
    dialogController.setButtonPaneNode(errorPane.fxNode);
    dialogController.setupButton(actions[0]);
    dialogController.setupButton(actions[1]);

    dialogController.onShown = {
      dialogController.walkTree { node ->
        if (node is ButtonBar
          || node.styleClass.intersect(listOf("tab-header-background", "tab-contents", "swing-background")).isNotEmpty()) {

          (node as Region).background =
            Background(BackgroundFill(
              "Panel.background".colorFromUiManager(), CornerRadii.EMPTY, Insets.EMPTY
            ))
        }
      }
      tabProviders.first().requestFocus()
      dialogController.resize()
    }
  }
}
