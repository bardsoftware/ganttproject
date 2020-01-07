/*
Copyright 2019 BarD Software s.r.o

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
package biz.ganttproject.storage

import biz.ganttproject.FXUtil
import biz.ganttproject.lib.fx.ListItemBuilder
import biz.ganttproject.lib.fx.buildFontAwesomeButton
import biz.ganttproject.storage.cloud.GPCloudStorage
import biz.ganttproject.storage.cloud.GPCloudStorageOptions
import biz.ganttproject.storage.local.LocalStorage
import biz.ganttproject.storage.webdav.WebdavServerSetupPane
import biz.ganttproject.storage.webdav.WebdavStorage
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.collections.ListChangeListener
import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.document.ReadOnlyProxyDocument
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor
import net.sourceforge.ganttproject.language.GanttLanguage
import java.io.File
import java.util.function.Consumer

/**
 * @author dbarashev@bardsoftware.com
 */
sealed class StorageMode(val name: String) {

  class FileException(message: String, vararg val args: Any) : Exception(message)

  abstract fun tryFile(file: File)

  class Open : StorageMode("open") {
    override fun tryFile(file: File) {
      if (!file.exists()) {
        throw FileException("document.storage.error.read.notExists", file)
      }
      if (file.exists() && !file.canRead()) {
        throw FileException("document.storage.error.read.cantRead", file)
      }
    }

  }

  class Save : StorageMode("save") {
    override fun tryFile(file: File) {
      if (file.exists() && !file.canWrite()) {
        throw FileException("document.storage.error.write.cantOverwrite", file)
      }
      if (!file.exists() && !file.parentFile.exists()) {
        throw FileException("document.storage.error.write.parentNotExists", file, file.parentFile)
      }
      if (!file.exists() && file.parentFile.exists() && !file.parentFile.canWrite()) {
        throw FileException("document.storage.error.write.parentNotWritable", file, file.parentFile)
      }
    }

  }

}

/**
 *
 * @author dbarashev@bardsoftware.com
 */
class StoragePane internal constructor(
    private val cloudStorageOptions: GPCloudStorageOptions,
    private val documentManager: DocumentManager,
    private val currentDocument: ReadOnlyProxyDocument,
    private val documentReceiver: Consumer<Document>,
    private val documentUpdater: Consumer<Document>,
    private val dialogUi: StorageDialogBuilder.DialogUi) {

  private var activeStorageLabel: Node? = null
  private val storageUiMap = mutableMapOf<String, Supplier<Pane>>()
  private val storageUiList = mutableListOf<StorageDialogBuilder.Ui>()
  private val storageUiPane = BorderPane()

  fun buildStoragePane(mode: StorageDialogBuilder.Mode): BorderPane {
    val borderPane = BorderPane()

    val storageButtons = VBox().apply {
      styleClass.add("storage-list")
    }
    val storagePane = BorderPane().apply {
      styleClass.add("pane-service-buttons")
      center = storageButtons
      top = HBox(Button("New Storage", FontAwesomeIconView(FontAwesomeIcon.PLUS)).apply {
        styleClass.add("btn-create")
        addEventHandler(ActionEvent.ACTION) { onNewWebdavServer(storageUiPane) }
      })
    }


    storageUiPane.setPrefSize(400.0, 400.0)

    borderPane.center = storageUiPane
    reloadStorageLabels(storageButtons, mode)
    cloudStorageOptions.list.addListener(ListChangeListener {
      reloadStorageLabels(storageButtons, mode, activeStorageLabel?.id)
    })

    if (storageUiList.size > 1) {
      borderPane.left = storagePane
    } else {
      storageUiPane.center = storageUiMap[storageUiList[0].category]?.get()
    }
    return borderPane
  }

  private fun reloadStorageLabels(storageButtons: VBox, mode: StorageDialogBuilder.Mode, selectedId: String? = null) {
    storageButtons.children.clear()
    storageUiList.clear()
    storageUiMap.clear()
    val i18n = GanttLanguage.getInstance()

    val openDocument = { document: Document ->
      try {
        (if (mode == StorageDialogBuilder.Mode.OPEN) documentReceiver else documentUpdater).accept(document)
        dialogUi.close()
      } catch (e: Exception) {
        dialogUi.error(e)
      }
    }
    val localStorage = LocalStorage(dialogUi, mode, currentDocument, openDocument).also{ storageUiList.add(it) }
    val recentProjects = RecentProjects(
        mode,
        documentManager,
        currentDocument,
        openDocument).also{ storageUiList.add(it) }
    storageUiList.add(GPCloudStorage(mode, openDocument, dialogUi, documentManager))
    cloudStorageOptions.webdavServers.mapTo(storageUiList) {
      WebdavStorage(it, mode, openDocument, dialogUi, cloudStorageOptions)
    }

    val initialStorageId = selectedId ?: if (mode == StorageDialogBuilder.Mode.OPEN) recentProjects.id else localStorage.id
    storageUiList.forEach { storageUi ->
      storageUiMap[storageUi.id] = Suppliers.memoize { storageUi.createUi() }

      val itemLabel = i18n.formatText("storageView.service.${storageUi.category}.label", storageUi.name)
      val itemIcon = i18n.getText("storageView.service.${storageUi.category}.icon")

      val listItemContent = buildFontAwesomeButton(
          itemIcon,
          itemLabel,
          { onStorageChange(storageUiPane, storageUi.id) },
          "storage-name")
      val builder = ListItemBuilder(listItemContent)
      builder.onSelectionChange = { pane: Parent -> this.setSelected(pane) }

      storageUi.createSettingsUi().ifPresent { settingsPane ->
        val listItemOnHover = buildFontAwesomeButton(FontAwesomeIcon.COG.name, null,
            {
              FXUtil.transitionCenterPane(storageUiPane, settingsPane) { dialogUi.resize() }
              Unit
            },
            "settings"
        )
        builder.hoverNode = listItemOnHover
      }
      val btnPane = builder.build()
      btnPane.styleClass.add("btn-service")
      btnPane.id = storageUi.id
      storageButtons.children.addAll(btnPane)

      if (initialStorageId == storageUi.id) {
        setSelected(btnPane)
      }
    }
    onStorageChange(storageUiPane, initialStorageId)
  }

  private fun setSelected(pane: Parent) {
    pane.styleClass.add("active")
    if (activeStorageLabel != null) {
      activeStorageLabel!!.styleClass.remove("active")
    }
    activeStorageLabel = pane
  }

  private fun onStorageChange(borderPane: BorderPane, storageId: String) {
    val ui = storageUiMap[storageId]?.get() ?: return
    FXUtil.transitionCenterPane(borderPane, ui) { dialogUi.resize() }
  }

  private fun onNewWebdavServer(borderPane: BorderPane) {
    val newServer = WebDavServerDescriptor()
    val setupPane = WebdavServerSetupPane(newServer, Consumer<WebDavServerDescriptor> { cloudStorageOptions.addValue(it) }, false)
    FXUtil.transitionCenterPane(borderPane, setupPane.createUi()) { dialogUi.resize() }
  }
}
