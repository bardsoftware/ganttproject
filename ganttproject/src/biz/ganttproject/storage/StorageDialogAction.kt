// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage

import biz.ganttproject.storage.cloud.GPCloudStorageOptions
import javafx.application.Platform
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.gui.ProjectUIFacade
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.UIUtil
import java.awt.event.ActionEvent
import javax.swing.SwingUtilities

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
        //dialogBuilder
        SwingUtilities.invokeLater {
          val dlg = myUiFacade.createDialog(contentPane, arrayOfNulls(0), GPAction.getI18n("myProjects.title"))
          dialogBuilder.setDialog(dlg)
        }
        //dialog.show();
      }
    }
  }
}
