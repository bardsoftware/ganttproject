// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.local

import biz.ganttproject.lib.fx.buildFontAwesomeButton
import biz.ganttproject.storage.StorageDialogBuilder
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
import net.sourceforge.ganttproject.language.GanttLanguage
import java.io.File
import java.util.*
import java.util.function.Consumer

/**
 * @author dbarashev@bardsoftware.com
 */
class LocalStorage(
    private val myMode: StorageDialogBuilder.Mode,
    private var currentDocument: Document,
    private val myDocumentReceiver: Consumer<Document>) : StorageDialogBuilder.Ui {
  private val i18n = GanttLanguage.getInstance()
  private var myResult: File? = null

  override fun getName(): String {
    return "This Computer"
  }

  override fun getCategory(): String {
    return "desktop"
  }

  private fun i18nKey(pattern: String): String {
    return String.format(pattern, myMode.name.toLowerCase())
  }

  override fun createUi(): Pane {
    val rootPane = VBox()
    rootPane.styleClass.addAll("pane-service-contents", "local-storage")
    rootPane.prefWidth = 400.0

    val titleBox = HBox()
    titleBox.styleClass.add("title")
    val title = Label(i18n.getText(i18nKey("storageService.local.%s.title")))
    titleBox.children.add(title)

    val docList = VBox()
    docList.styleClass.add("doclist")

    val currentDocLabel = Label(currentDocument.fileName)
    currentDocLabel.styleClass.add("doclist-doc")
    docList.children.addAll(currentDocLabel)

    fun onBrowse() {
      val fileChooser = FileChooser()
      fileChooser.title = i18nKey("storageService.local.%s.fileChooser.title")
      fileChooser.extensionFilters.addAll(
          FileChooser.ExtensionFilter("GanttProject Files", "*.gan"))
      myResult = fileChooser.showOpenDialog(null)
      if (myResult != null) {
        currentDocument = FileDocument(myResult)
        currentDocLabel.text = currentDocument.fileName
      }
    }

    val btnBrowse = buildFontAwesomeButton(FontAwesomeIcon.SEARCH.name, "Browse...", {onBrowse()}, "doclist-browse")
    val btnSave = buildFontAwesomeButton(
        iconName = i18n.getText(i18nKey("storageService.local.%s.icon")),
        label = i18n.getText(i18nKey("storageService.local.%s.actionLabel")),
        onClick = {myDocumentReceiver.accept(currentDocument)},
        styleClass = "doclist-save")
    val browseAndSave = HBox()
    browseAndSave.styleClass.add("doclist-browse")
    browseAndSave.children.addAll(btnBrowse, btnSave)

    rootPane.stylesheets.add("biz/ganttproject/storage/StorageDialog.css")
    rootPane.stylesheets.add("biz/ganttproject/storage/local/LocalStorage.css")
    rootPane.children.addAll(titleBox, docList, browseAndSave)
    return rootPane
  }

  override fun createSettingsUi(): Optional<Pane> {
    return Optional.empty<Pane>()
  }
}
