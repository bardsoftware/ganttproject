/*
Copyright 2019-2020 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

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
package biz.ganttproject.storage

import biz.ganttproject.FXUtil
import biz.ganttproject.app.DialogController
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.createAlertBody
import biz.ganttproject.storage.cloud.GPCloudStorageOptions
import com.google.common.base.Preconditions
import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.document.ReadOnlyProxyDocument
import net.sourceforge.ganttproject.gui.ProjectUIFacade
import net.sourceforge.ganttproject.language.GanttLanguage
import org.controlsfx.control.NotificationPane
import java.io.IOException
import java.util.*
import java.util.function.Consumer

/**
 * @author dbarashev@bardsoftware.com
 */
class StorageDialogBuilder(
    private val myProject: IGanttProject,
    projectUi: ProjectUIFacade,
    documentManager: DocumentManager,
    cloudStorageOptions: GPCloudStorageOptions,
    private val dialogBuildApi: DialogController) {
  private val myCloudStorageOptions: GPCloudStorageOptions = Preconditions.checkNotNull(cloudStorageOptions)
  private val myDocumentReceiver: Consumer<Document>
  private val myDocumentUpdater: Consumer<Document>
  private var myNotificationPane: NotificationPane? = null
  private var myOpenStorage: Node? = null
  private var mySaveStorage: Pane? = null

  private val myDialogUi = DialogUi(dialogBuildApi) { myNotificationPane!!}

  init {
    // This will be called when user opens a project.
    myDocumentReceiver = Consumer { document: Document ->
      myDialogUi.toggleProgress(true)
      val onFinish = Channel<Boolean>()

      GlobalScope.launch(Dispatchers.IO) {
        try {
          projectUi.openProject(documentManager.getProxyDocument(document), myProject, onFinish)
          onFinish.receive()
          myDialogUi.toggleProgress(false)
          myDialogUi.close()
        } catch (e: IOException) {
          e.printStackTrace()
        } catch (e: Document.DocumentException) {
          e.printStackTrace()
        }
      }
    }
    // This will be called when user saves a project.
    myDocumentUpdater = Consumer { document ->
      myDialogUi.toggleProgress(true)
      val onFinish = Channel<Boolean>()
      GlobalScope.launch(Dispatchers.IO) {
        if (myProject.document == null) {
          myProject.document = documentManager.getProxyDocument(document)
        } else {
          myProject.document.setMirror(document)
        }
        if (document.isLocal) {
          document.asLocalDocument()?.create()
        }
        projectUi.saveProject(myProject, onFinish)
        onFinish.receive()
        myDialogUi.toggleProgress(false)
        myDialogUi.close()
      }
    }
  }

  fun build(mode: Mode) {
    dialogBuildApi.addStyleClass("dlg-storage")
    dialogBuildApi.addStyleSheet("/biz/ganttproject/storage/StorageDialog.css")
    dialogBuildApi.removeButtonBar()

    val borderPane = BorderPane()
    borderPane.styleClass.addAll("body", "pane-storage")
    borderPane.center = Pane()
    val btnSave = Button(GanttLanguage.getInstance().getText("myProjects.save"))
    val btnOpen = Button(GanttLanguage.getInstance().getText("myProjects.open"))
    btnSave.apply {
      addEventHandler(ActionEvent.ACTION) {
        showSaveStorageUi(borderPane)
        btnOpen.styleClass.removeAll("selected")
        btnSave.styleClass.add("selected")
      }
      maxWidth = Double.MAX_VALUE
      styleClass.add("selected")
    }
    btnOpen.apply {
      addEventHandler(ActionEvent.ACTION) {
        showOpenStorageUi(borderPane)
        btnSave.styleClass.removeAll("selected")
        btnOpen.styleClass.add("selected")
      }
      maxWidth = Double.MAX_VALUE

    }

    val titleBox = VBox()
    titleBox.styleClass.add("header")
    val projectName = Label(myProject.projectName)

    val buttonBar = GridPane().apply {
      maxWidth = Double.MAX_VALUE
      columnConstraints.addAll(
          ColumnConstraints().apply { percentWidth = 45.0 },
          ColumnConstraints().apply { percentWidth = 45.0 }
      )
      hgap = 5.0
      styleClass.add("open-save-buttons")
      add(btnSave, 0, 0)
      add(btnOpen, 1, 0)
    }

    titleBox.children.addAll(projectName, buttonBar)
    this.dialogBuildApi.setHeader(titleBox)
    this.dialogBuildApi.setContent(borderPane)
    this.dialogBuildApi.beforeShow = {
      if (mode == Mode.SAVE) {
        btnSave.fire()
        btnSave.requestFocus()
      } else {
        btnOpen.fire()
        btnOpen.requestFocus()
      }
    }
  }

  private fun showOpenStorageUi(container: BorderPane) {
    if (myOpenStorage == null) {
      val storagePane = buildStoragePane(Mode.OPEN)
      myNotificationPane = NotificationPane(storagePane)
      myNotificationPane!!.styleClass.addAll(
          NotificationPane.STYLE_CLASS_DARK)
      myOpenStorage = myNotificationPane
    }
    FXUtil.transitionCenterPane(container, myOpenStorage, {})
  }

  private fun showSaveStorageUi(container: BorderPane) {
    if (mySaveStorage == null) {
      mySaveStorage = buildStoragePane(Mode.SAVE)
    }
    FXUtil.transitionCenterPane(container, mySaveStorage, {})
  }

  private fun buildStoragePane(mode: Mode): Pane {
    if (myProject.document != null) {
      val storagePane = StoragePane(myCloudStorageOptions, myProject.documentManager, ReadOnlyProxyDocument(myProject.document), myDocumentReceiver, myDocumentUpdater, myDialogUi)
      return storagePane.buildStoragePane(mode)
    } else {
      return Pane(Label("No document!"))
    }
  }

  enum class Mode {
    OPEN, SAVE
  }

  class DialogUi(internal val dialogController: DialogController,
                 private val notificationPane: () -> NotificationPane) {
    fun close() {
      dialogController.hide()
    }

    fun resize() {
      this.dialogController.resize()
    }

    fun error(e: Throwable) {
      dialogController.showAlert(RootLocalizer.create("error.channel.itemTitle"), createAlertBody(e.message ?: ""))
    }

    fun error(message: String) {
      dialogController.showAlert(RootLocalizer.create("error.channel.itemTitle"), createAlertBody(message))
    }

    fun message(message: String) {
      val notificationText = TextArea(message)
      notificationText.isWrapText = true
      notificationText.prefRowCount = 3
      notificationText.styleClass.add("info")
      this.notificationPane().graphic = notificationText
      this.notificationPane().show()
    }

    fun toggleProgress(isShown: Boolean) {
      dialogController.toggleProgress(isShown)
    }
  }

  interface Ui {
    val category: String

    val id: String
      get() = category

    val name: String

    fun createUi(): Pane

    fun createSettingsUi(): Optional<Pane>
  }
}
