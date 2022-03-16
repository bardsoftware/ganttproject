/*
 * Copyright (c) 2021 Dmitry Barashev, BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */

package biz.ganttproject.print

import biz.ganttproject.app.*
import biz.ganttproject.lib.DateRangePicker
import biz.ganttproject.lib.DateRangePickerModel
import biz.ganttproject.lib.fx.vbox
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.stage.FileChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.chart.Chart
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.util.FileUtil
import org.osgi.service.prefs.Preferences
import java.io.IOException
import java.util.concurrent.Executors
import javax.print.attribute.standard.MediaSize
import javax.swing.SwingUtilities
import kotlin.reflect.KClass
import kotlin.reflect.full.staticProperties
//import javafx.print.Paper as FxPaper


/**
 * @author dbarashev@bardsoftware.com
 */
fun showPrintDialog(activeChart: Chart, preferences: Preferences) {
  val i18n = RootLocalizer
  val prefixedLocalizer = RootLocalizer.createWithRootKey("print.preview")

  dialog { dlg ->
    val previews = Previews(activeChart, preferences, onError = { ex ->
      dlg.showAlert(prefixedLocalizer.create("alert.title"), createAlertBody(ex))
    })
    previews.setMediaSize(previews.mediaSizeKey)
    previews.autoUpdate = true

    dlg.addStyleClass("dlg", "dlg-print-preview")
    dlg.addStyleSheet(
      "/biz/ganttproject/app/Dialog.css",
      "/biz/ganttproject/print/Print.css"
    )

    val controls = vbox {
      // -- Page format
      add(
        Label(i18n.formatText("choosePaperFormat")).also {
          VBox.setMargin(it, Insets(0.0, 0.0, 3.0, 0.0))
        }
      )
      add(
        ComboBox(FXCollections.observableList(
          //Previews.papers.keys.toList()
          mediaSizes.keys.toList()
        )).also { comboBox ->
          comboBox.setOnAction {
            previews.setMediaSize(comboBox.selectionModel.selectedItem)
          }
          comboBox.selectionModel.select(previews.mediaSizeKey)
        }
      )

      // -- Page orientation
      add(
        Label(i18n.formatText("option.export.itext.landscape.label")).also {
          VBox.setMargin(it, Insets(5.0, 0.0, 3.0, 0.0))
        }
      )
      add(
        ComboBox(FXCollections.observableList(
          Orientation.values().map { i18n.formatText(it.name.lowercase()) }.toList()
        )).also { comboBox ->
          comboBox.setOnAction {
            previews.orientation = Orientation.values()[comboBox.selectionModel.selectedIndex]
          }
          comboBox.selectionModel.select(i18n.formatText(previews.orientation.name.lowercase()))
        }
      )

      // -- Date range
      add(
        Label(i18n.formatText("print.preview.dateRange")).also {
          VBox.setMargin(it, Insets(5.0, 0.0, 3.0, 0.0))
        }
      )
      add(
        DateRangePicker(previews.dateRangeModel, MappingLocalizer(mapOf(
        "custom" to { prefixedLocalizer.formatText("dateRange.custom") },
        "view" to { prefixedLocalizer.formatText("dateRange.currentView") },
        "project" to { i18n.formatText("wholeProject") }
      )) { key ->
        i18n.formatText(key)
      }).component
      )
    }

    val contentPane = BorderPane().also {
      it.styleClass.add("content-pane")
      it.right = BorderPane().apply {
        top = controls
        styleClass.add("controls")
      }
      it.center = ScrollPane(Pane(previews.gridPane).also {p -> p.styleClass.add("all-pages")})
    }
    dlg.setContent(contentPane)
    dlg.setButtonPaneNode(
      HBox().also { hbox ->
        hbox.alignment = Pos.CENTER_LEFT
        hbox.children.addAll(
          // -- Preview zoom

          Label(i18n.formatText("print.preview.scale")).also {
            HBox.setMargin(it, Insets(0.0, 5.0, 0.0, 15.0))
          },
          //FontAwesomeIconView(FontAwesomeIcon.BAR_CHART),
          Slider(0.0, 10.0, 0.0).also { slider ->
            slider.majorTickUnit = 1.0
            slider.blockIncrement = 1.0
            slider.isSnapToTicks = true
            slider.valueProperty().addListener { _, _, newValue ->
              previews.zooming = newValue.toInt()
            }
            slider.value = 4.0
          },
        )
      }
    )
    dlg.setupButton(ButtonType.YES) {
      it.text = i18n.formatText("print.export.button.exportAsZip").removeMnemonicsPlaceholder()
      it.styleClass.addAll("btn-attention", "secondary")
      it.onAction = EventHandler {
        exportPages(previews.pages, activeChart.project, dlg)
      }
    }
    val btnApply = dlg.setupButton(ButtonType.APPLY) {
      it.text = i18n.formatText("project.print").removeMnemonicsPlaceholder()
      it.styleClass.addAll("btn-attention")
      it.onAction = EventHandler {
        //printPages(Previews.pages, Previews.paper)
        SwingUtilities.invokeLater {
          try {
            printPages(previews.pages, previews.mediaSize, previews.orientation)
          } catch (ex: Exception) {
            ourLogger.error("Print job failed", ex)
            Platform.runLater {
              dlg.showAlert(i18n.create("print.job.alert.title"), createAlertBody(ex))
            }
          }
        }
      }
    }
    dlg.onShown = {
      btnApply?.requestFocus()
    }
  }
}

private val zoomFactors = listOf(1.0, 1.25, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
private val readImageScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
val mediaSizes: Map<String, MediaSize> = mutableMapOf<String, MediaSize>().let {
  it.putAll(mediaSizes(MediaSize.ISO::class))
  it.putAll(mediaSizes(MediaSize.JIS::class))
  it.putAll(mediaSizes(MediaSize.NA::class))
  it.toMap()
}

private typealias ErrorUi = (Exception) -> Unit
private class Previews(val chart: Chart, private val preferences: Preferences, private val onError: ErrorUi) {

  val dateRangeModel = DateRangePickerModel(chart).also {
    it.selectedRange.addListener { _, _, newValue ->
      this.preferences.put("date-range", newValue.persistentValue)
      updateTiles()
    }
  }
  /*
  val papers: Map<String, FxPaper> = mutableMapOf<String, FxPaper>().let {
    it.putAll(papers())
    it.toMap()
  }
  */

  val gridPane = GridPane().also {
    it.vgap = 10.0
    it.hgap = 10.0
    it.padding = Insets(10.0, 10.0, 10.0, 10.0)
  }

  var mediaSize: MediaSize = MediaSize.ISO.A4
  set(value) {
    field = value
    updateTiles()
  }

  val mediaSizeKey: String get() =
    mediaSizes.filter { it.value == this.mediaSize }.firstNotNullOfOrNull { it.key } ?: MediaSize.ISO.A4.name

  /*
  var paper: FxPaper = FxPaper.A4
  set(value) {
    field = value
    mediaSize = MediaSize((field.width/72.0).toFloat(), (field.height/72.0).toFloat(), MediaSize.INCH)
  }
  */
  fun setMediaSize(name: String) {
    mediaSizes[name]?.let { mediaSize = it }
    preferences.put("page-size", name)
    //papers[name]?.let { paper = it }
  }

  var zoomFactor = 1.0
  set(value) {
    field = value
    updatePreviews()
  }

  var orientation: Orientation = Orientation.LANDSCAPE
  set(value) {
    field = value
    preferences.put("page-orientation", value.name.lowercase())
    updateTiles()
  }

  var pages: List<PrintPage> = listOf()
  set(value) {
    field = value
    updatePreviews()
  }

  var zooming: Int = 4
  set(value) {
    field = value
    zoomFactor = zoomFactors[value]
  }

  var autoUpdate: Boolean = false
  set(value) {
    field = value
    if (value) {
      updateTiles()
    }
  }
  init {
    mediaSizes[this.preferences.get("page-size", MediaSize.ISO.A4.name)]?.let {
      mediaSize = it
    }
    orientation = Orientation.valueOf(
      this.preferences.get("page-orientation", Orientation.LANDSCAPE.name).uppercase()
    )
    dateRangeModel.init(this.preferences.get("date-range", "view"))
  }

  /**
   * This function re-generates image tiles.
   */
  private fun updateTiles() {
    if (autoUpdate) {
      val channel = Channel<PrintPage>()
      readImages(channel)
      createImages(chart, mediaSize, 144, orientation, dateRangeModel.selectedRange.get().asClosedRange(), channel)
    }
  }

  /**
   * This function updates the preview nodes, without re-generating the tiles themselves.
   */
  private fun updatePreviews() {
    Platform.runLater {
      gridPane.children.clear()
      pages.forEach { page ->
        val pageWidth = zoomFactor *
            (if (orientation == Orientation.LANDSCAPE) mediaSize.previewWidth()
            else mediaSize.previewHeight())
        val pageHeight = zoomFactor *
            (if (orientation == Orientation.LANDSCAPE) mediaSize.previewHeight()
            else mediaSize.previewWidth())
        val previewWidth = pageWidth * page.widthFraction
        val previewHeight = pageHeight * page.heightFraction
        Pane(ImageView(
          Image(
            page.imageFile.inputStream(),
            previewWidth, previewHeight,
            true,
            true
          )
        )).also {
          it.prefWidth = pageWidth
          it.prefHeight = pageHeight
          gridPane.add(StackPane(it).also { border ->
            border.styleClass.add("page")
          }, page.column, page.row)
        }
      }
    }
  }

  private fun readImages(channel: Channel<PrintPage>) {
    readImageScope.launch {
      try {
        pages = channel.receiveAsFlow().toList()
      } catch (ex: Exception) {
        ourLogger.error("Images generation failed", ex)
        onError(ex)
      }
    }
  }

  private fun MediaSize.previewWidth() = BASE_PREVIEW_WIDTH *
      this.getX(MediaSize.MM) / MediaSize.ISO.A4.getX(MediaSize.MM)

  private fun MediaSize.previewHeight() = BASE_PREVIEW_HEIGHT *
      this.getY(MediaSize.MM) / MediaSize.ISO.A4.getY(MediaSize.MM)
}

private fun exportPages(pages: List<PrintPage>, project: IGanttProject, dlg: DialogController) {
  val fileChooser = FileChooser()
  fileChooser.title = RootLocalizer.formatText("storageService.local.save.fileChooser.title")
  fileChooser.extensionFilters.add(
    FileChooser.ExtensionFilter(RootLocalizer.formatText("filterzip"), "zip")
  )
  fileChooser.initialFileName = FileUtil.replaceExtension(project.document.fileName, "zip")
  val file = fileChooser.showSaveDialog(null)
  if (file != null) {
    try {
      val zipBytes = FileUtil.zip(pages.mapIndexed { index, page ->
        "${project.document.fileName}_page$index.png" to { page.imageFile.inputStream() }
      }.toList())
      file.writeBytes(zipBytes)
    } catch (ex: IOException) {
      ourLogger.error("Failed to write an archive with the exported pages to {}", file.absolutePath, ex)
      dlg.showAlert(RootLocalizer.create("print.export.alert.title"), createAlertBody(ex.message ?: ""))
    }
  }
}

fun createPrintAction(uiFacade: UIFacade, preferences: Preferences): GPAction {
  return GPAction.create("project.print") {
    showPrintDialog(uiFacade.activeChart, preferences.node("/configuration/print"))
  }
}

private fun mediaSizes(clazz: KClass<*>): Map<String, MediaSize> =
  clazz.staticProperties.filter {
    it.get() is MediaSize
  }.associate {
    it.name to it.get() as MediaSize
  }
/*
private fun papers(): Map<String, FxPaper> =
    FxPaper::class.staticProperties
      .filter { it.get() is FxPaper }
      .associate { it.name to it.get() as FxPaper }
*/
private const val BASE_PREVIEW_WIDTH = 297.0
private const val BASE_PREVIEW_HEIGHT = 210.0
private val ourLogger = GPLogger.create("Print")
