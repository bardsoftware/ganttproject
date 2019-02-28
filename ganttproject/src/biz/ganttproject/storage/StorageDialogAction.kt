// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage

import biz.ganttproject.storage.cloud.GPCloudStorageOptions
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.control.Dialog
import javafx.scene.input.KeyCombination
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.gui.ProjectUIFacade
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.UIUtil
import java.awt.event.ActionEvent

/**
 * @author dbarashev@bardsoftware.com
 */
class StorageDialogAction(private val myProject: IGanttProject, private val myUiFacade: UIFacade, private val myProjectUiFacade: ProjectUIFacade,
                          private val myDocumentManager: DocumentManager, private val myCloudStorageOptions: GPCloudStorageOptions) : GPAction("myProjects.action") {

  override fun actionPerformed(actionEvent: ActionEvent?) {

    UIUtil.initJavaFx {
      Platform.runLater {

        val dialogBuilder = StorageDialogBuilder(myProject, myProjectUiFacade, myDocumentManager, myCloudStorageOptions)
        val contentPane = dialogBuilder.build()
        Dialog<Unit>().apply {
          this.dialogPane.content = contentPane
          this.isResizable = true
          this.width = 600.0
          this.height = 600.0

          val window = this.dialogPane.scene.window
          window.onCloseRequest = EventHandler {
            window.hide()
          }
          this.dialogPane.scene.accelerators[KeyCombination.keyCombination("ESC")] = Runnable { window.hide() }

          this.show()
        }
        //dialogBuilder
//        SwingUtilities.invokeLater {
//          val dlg = myUiFacade.createDialog(contentPane, arrayOfNulls(0), GPAction.getI18n("myProjects.title"))
//          dialogBuilder.setDialog(dlg)
//        }
        //dialog.show();
      }
    }
  }
}
