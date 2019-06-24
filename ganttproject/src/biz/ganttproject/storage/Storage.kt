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
import com.google.common.collect.Lists
import com.google.common.collect.Maps
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
import java.util.*
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

class StorageUtil(private val myMode: StorageMode) {
  internal fun i18nKey(pattern: String): String {
    return String.format(pattern, myMode.name.toLowerCase())
  }

}

/**
 * @author dbarashev@bardsoftware.com
 */
class StoragePane internal constructor(
    private val myCloudStorageOptions: GPCloudStorageOptions,
    private val myDocumentManager: DocumentManager,
    private val myCurrentDocument: ReadOnlyProxyDocument,
    private val myDocumentReceiver: Consumer<Document>,
    private val myDocumentUpdater: Consumer<Document>,
    private val myDialogUi: StorageDialogBuilder.DialogUi) {
  private var myActiveStorageLabel: Node? = null
  private val myStorageUiMap = Maps.newHashMap<String, Supplier<Pane>>()
  private val myStorageUiList = Lists.newArrayList<StorageDialogBuilder.Ui>()
  private val storageUiPane = BorderPane()

  fun buildStoragePane(mode: StorageDialogBuilder.Mode): BorderPane {
    val borderPane = BorderPane()

    val storagePane = BorderPane()
    storagePane.styleClass.add("pane-service-buttons")
    val storageButtons = VBox().also {
      it.styleClass.add("storage-list")
    }
    storagePane.center = storageButtons
    Button("New Storage", FontAwesomeIconView(FontAwesomeIcon.PLUS)).also {
      it.styleClass.add("btn-create")
      it.addEventHandler(ActionEvent.ACTION) { onNewWebdavServer(storageUiPane) }
      storagePane.top = HBox(it)
    }


    storageUiPane.setPrefSize(400.0, 400.0)

    borderPane.center = storageUiPane
    reloadStorageLabels(storageButtons, mode, Optional.empty())
    myCloudStorageOptions.list.addListener(ListChangeListener {
      reloadStorageLabels(
          storageButtons, mode,
          if (myActiveStorageLabel == null) Optional.empty()
          else Optional.of(myActiveStorageLabel!!.id)
      )
    })

    if (myStorageUiList.size > 1) {
      borderPane.left = storagePane
    } else {
      storageUiPane.center = myStorageUiMap[myStorageUiList[0].category]?.get()
    }
    return borderPane
  }

  private fun reloadStorageLabels(storageButtons: VBox, mode: StorageDialogBuilder.Mode, selectedId: Optional<String>) {
    storageButtons.children.clear()
    myStorageUiList.clear()
    myStorageUiMap.clear()
    val i18n = GanttLanguage.getInstance()

    val doOpenDocument = if (mode == StorageDialogBuilder.Mode.OPEN) myDocumentReceiver else myDocumentUpdater
    val openDocument = Consumer { document: Document ->
      try {
        doOpenDocument.accept(document)
        myDialogUi.close()
      } catch (e: Exception) {
        myDialogUi.error(e)
      }
    }
    myStorageUiList.add(LocalStorage(
        myDialogUi,
        mode,
        myCurrentDocument,
        openDocument)
    )
    val recentProjects = RecentProjects(
        mode,
        myDocumentManager,
        myCurrentDocument,
        openDocument)
    myStorageUiList.add(recentProjects)
    myStorageUiList.add(GPCloudStorage(mode, openDocument, myDialogUi, myDocumentManager))
    myCloudStorageOptions.webdavServers.mapTo(myStorageUiList) {
      WebdavStorage(it, mode, openDocument, myDialogUi, myCloudStorageOptions)
    }

    val initialStorageId = selectedId.orElse(recentProjects.id)
    myStorageUiList.forEach { storageUi ->
      myStorageUiMap[storageUi.id] = Suppliers.memoize { storageUi.createUi() }

      val itemLabel = i18n.formatText(
          String.format("storageView.service.%s.label", storageUi.category), storageUi.name)
      val itemIcon = i18n.getText(
          String.format("storageView.service.%s.icon", storageUi.category))

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
              FXUtil.transitionCenterPane(storageUiPane, settingsPane) { myDialogUi.resize() }
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
    if (myActiveStorageLabel != null) {
      myActiveStorageLabel!!.styleClass.remove("active")
    }
    myActiveStorageLabel = pane
  }

  private fun onStorageChange(borderPane: BorderPane, storageId: String) {
    val ui = myStorageUiMap[storageId]?.get() ?: return
    FXUtil.transitionCenterPane(borderPane, ui) { myDialogUi.resize() }
  }

  private fun onNewWebdavServer(borderPane: BorderPane) {
    val newServer = WebDavServerDescriptor()
    val setupPane = WebdavServerSetupPane(newServer, Consumer<WebDavServerDescriptor> { myCloudStorageOptions.addValue(it) }, false)
    FXUtil.transitionCenterPane(borderPane, setupPane.createUi()) { myDialogUi.resize() }
  }
}
