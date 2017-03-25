// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.local

import biz.ganttproject.lib.fx.ListItemBuilder
import biz.ganttproject.lib.fx.buildFontAwesomeButton
import biz.ganttproject.storage.StorageDialogBuilder
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
import net.sourceforge.ganttproject.language.GanttLanguage
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
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
  private var myWorkingDir: File? = null

  override fun getName(): String {
    return "This Computer"
  }

  override fun getCategory(): String {
    return "desktop"
  }

  private fun i18nKey(pattern: String): String {
    return String.format(pattern, myMode.name.toLowerCase())
  }

  fun changeWorkingDir(dir: File) {
    println("Change workingdir=$dir")
    myWorkingDir = dir
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

    val currentDocDir = createDirPane(currentDocument.filePath, this::changeWorkingDir)
    val currentDocName = TextField(currentDocument.fileName)
//    val currentDocLabel = Label(currentDocument.fileName)
//    currentDocLabel.styleClass.add("doclist-doc")
    docList.children.addAll(currentDocDir, currentDocName)

    fun onBrowse() {
      val fileChooser = FileChooser()
      println("working dir=$myWorkingDir")
      fileChooser.initialDirectory = myWorkingDir
      fileChooser.title = i18nKey("storageService.local.%s.fileChooser.title")
      fileChooser.extensionFilters.addAll(
          FileChooser.ExtensionFilter("GanttProject Files", "*.gan"))
      myResult = fileChooser.showOpenDialog(null)
      if (myResult != null) {
        currentDocument = FileDocument(myResult)
        currentDocName.text = currentDocument.fileName
      }
    }

    val btnBrowse = buildFontAwesomeButton(FontAwesomeIcon.SEARCH.name, "Browse...", {onBrowse()}, "doclist-browse")
//    val btnSave = buildFontAwesomeButton(
//        iconName = i18n.getText(i18nKey("storageService.local.%s.icon")),
//        label = i18n.getText(i18nKey("storageService.local.%s.actionLabel")),
//        onClick = {myDocumentReceiver.accept(currentDocument)},
//        styleClass = "doclist-save")
    val btnSave = Button(i18n.getText(i18nKey("storageService.local.%s.actionLabel")))
    btnSave.addEventHandler(ActionEvent.ACTION, {myDocumentReceiver.accept(currentDocument)})
    btnSave.styleClass.add("doclist-save")

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

  fun createDirPane(filePath: String, onClick: ((File) -> Unit)): Pane {
    val result = VBox()
    result.styleClass.add("doclist-path")

    fun newPathNodeBuilder(path: Path): ListItemBuilder {
      val label = if(path.nameCount >= 1) path.getName(path.nameCount - 1).toString() else path.toString()
      val builder = ListItemBuilder(Button(label))
      builder.onSelectionChange = {listItem ->
        result.children.forEach { node -> node.styleClass.remove("active") }
        listItem.styleClass.add("active")
        onClick(path.toFile())
      }
      return builder
    }
    fun buildPathNode(path: Path): Node {
      val builder = newPathNodeBuilder(path)
      val result = builder.build()
      result.style = "-fx-padding: 0.5ex 0 0.5ex ${path.nameCount}em"
      return result
    }

    val path = Paths.get(filePath)
    val builder = newPathNodeBuilder(path.subpath(0, path.nameCount - 1))

    builder.hoverNode = buildFontAwesomeButton(
        iconName = "level_down",
        onClick = {_ ->
          for (i in path.nameCount - 3 downTo 0) {
            val dirPath = path.root.resolve(path.subpath(0, i + 1))
            result.children.add(0, buildPathNode(dirPath))
          }
          result.children.add(0, buildPathNode(path.root))
          builder.contentNode.style = "-fx-padding: 0.5ex 0 0.5ex ${path.nameCount}em"
          result.styleClass.add("expanded")
        }
    )

    result.children.addAll(builder.build())
    return result
  }
}
