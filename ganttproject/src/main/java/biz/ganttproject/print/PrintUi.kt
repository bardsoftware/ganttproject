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

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.dialog
import biz.ganttproject.app.removeMnemonicsPlaceholder
import biz.ganttproject.lib.DateRangePicker
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.chart.Chart
import net.sourceforge.ganttproject.gui.UIFacade
import java.util.*
import java.util.concurrent.Executors
import javax.print.attribute.standard.MediaSize
import kotlin.reflect.KClass
import kotlin.reflect.full.staticProperties
import javafx.print.Paper as FxPaper

/**
 * @author dbarashev@bardsoftware.com
 */
fun showPrintDialog(activeChart: Chart) {
  val i18n = RootLocalizer
  Previews.chart = activeChart
  Previews.setMediaSize("A4")
  dialog { dlg ->
    dlg.addStyleClass("dlg", "dlg-print-preview")
    dlg.addStyleSheet(
      "/biz/ganttproject/app/Dialog.css",
      "/biz/ganttproject/print/Print.css"
    )
    dlg.setHeader(
      HBox().also { hbox ->
        hbox.alignment = Pos.CENTER_LEFT
        hbox.styleClass.add("header")
        hbox.children.addAll(
          // -- Preview zoom
          Label(i18n.formatText("zoom")).also {
            HBox.setMargin(it, Insets(0.0, 5.0, 0.0, 15.0))
          },
          Slider(0.0, 10.0, 0.0).also { slider ->
            //slider.isShowTickMarks = true
            slider.majorTickUnit = 1.0
            slider.blockIncrement = 1.0
            slider.isSnapToTicks = true
            slider.valueProperty().addListener { _, _, newValue ->
              Previews.zooming = newValue.toInt()
            }
          },

          // -- Page format
          Label(i18n.formatText("choosePaperFormat")).also {
            HBox.setMargin(it, Insets(0.0, 5.0, 0.0, 15.0))
          },
          ComboBox(FXCollections.observableList(
            //Previews.papers.keys.toList()
            Previews.mediaSizes.keys.toList()
          )).also { comboBox ->
            comboBox.setOnAction {
              Previews.setMediaSize(comboBox.selectionModel.selectedItem)
            }
            comboBox.selectionModel.select("A4")
          },

          // -- Page orientation
          Label(i18n.formatText("option.export.itext.landscape.label")).also {
            HBox.setMargin(it, Insets(0.0, 5.0, 0.0, 15.0))
          },
          ComboBox(FXCollections.observableList(
            Orientation.values().map { i18n.formatText(it.name.lowercase()) }.toList()
          )).also { comboBox ->
            comboBox.setOnAction {
              Previews.orientation = Orientation.values()[comboBox.selectionModel.selectedIndex]
            }
            comboBox.selectionModel.select(i18n.formatText(Previews.orientation.name.lowercase()))
          },

          // -- Date range
          Label("Date range").also {
            HBox.setMargin(it, Insets(0.0, 5.0, 0.0, 15.0))
          },
          DateRangePicker(activeChart).let {
            it.selectedRange.addListener { _, _, newValue ->
              Previews.onDateRangeChange(newValue.startDate, newValue.endDate)
            }
            it.component
          }
        )
      }
    )

    val contentPane = BorderPane().also {
      it.styleClass.add("content-pane")
      it.center = ScrollPane(Previews.gridPane)
    }
    dlg.setContent(contentPane)
    dlg.setupButton(ButtonType.APPLY) {
      it.text = i18n.formatText("project.print").removeMnemonicsPlaceholder()
      it.styleClass.addAll("btn-attention")
      it.onAction = EventHandler {
        //printPages(Previews.pages, Previews.paper)
        printPages(Previews.pages, Previews.mediaSize, Previews.orientation)
      }
    }
  }
}

private object Previews {
  lateinit var chart: Chart
  private val zoomFactors = listOf(1.0, 1.25, 1.5, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
  private val readImageScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

  val mediaSizes: Map<String, MediaSize> = mutableMapOf<String, MediaSize>().let {
    it.putAll(mediaSizes(MediaSize.ISO::class))
    it.putAll(mediaSizes(MediaSize.JIS::class))
    it.putAll(mediaSizes(MediaSize.NA::class))
    it.toMap()
  }

  val papers: Map<String, FxPaper> = mutableMapOf<String, FxPaper>().let {
    it.putAll(papers())
    it.toMap()
  }

  val gridPane = GridPane().also {
    it.vgap = 10.0
    it.hgap = 10.0
  }

  var mediaSize: MediaSize = MediaSize.ISO.A4
  set(value) {
    field = value
    updateTiles()
  }

  var paper: FxPaper = FxPaper.A4
  set(value) {
    field = value
    mediaSize = MediaSize((field.width/72.0).toFloat(), (field.height/72.0).toFloat(), MediaSize.INCH)
  }
  fun setMediaSize(name: String) {
    mediaSizes[name]?.let { mediaSize = it }
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
    updateTiles()
  }

  var pages: List<PrintPage> = listOf()
  set(value) {
    field = value
    updatePreviews()
  }

  var zooming: Int = 0
  set(value) {
    field = value
    zoomFactor = zoomFactors[value]
  }

  fun onDateRangeChange(start: Date, end: Date) {
    chart.startDate = start
    updateTiles()
  }

  private fun updateTiles() {
    val channel = Channel<PrintPage>()
    readImages(channel)
    createImages(chart, mediaSize, 144, orientation, channel)
  }

  private fun updatePreviews() {
    Platform.runLater {
      gridPane.children.clear()
      pages.forEach { page ->
        val previewWidth = mediaSize.previewWidth() * zoomFactor * page.widthFraction
        val previewHeight = mediaSize.previewHeight() * zoomFactor * page.heightFraction
        Pane(ImageView(
          Image(
            page.imageFile.inputStream(),
            previewWidth, previewHeight,
            true,
            true
          )
        )).also {
          it.prefWidth = zoomFactor * (if (orientation == Orientation.LANDSCAPE) mediaSize.previewWidth() else mediaSize.previewHeight())
          it.prefHeight = zoomFactor * (if (orientation == Orientation.LANDSCAPE) mediaSize.previewHeight() else mediaSize.previewWidth())
          gridPane.add(StackPane(it).also {
              border -> border.styleClass.add("page")
          }, page.column, page.row)
        }
      }
    }
  }

  fun readImages(channel: Channel<PrintPage>) {
    readImageScope.launch {
      pages = channel.receiveAsFlow().toList()
    }
  }

  private fun MediaSize.previewWidth() = BASE_PREVIEW_WIDTH * this.getX(MediaSize.MM) / MediaSize.ISO.A4.getX(MediaSize.MM)
  private fun MediaSize.previewHeight() = BASE_PREVIEW_HEIGHT * this.getY(MediaSize.MM) / MediaSize.ISO.A4.getY(MediaSize.MM)
}

fun createPrintAction(uiFacade: UIFacade): GPAction {
  return GPAction.create("project.print") {
    showPrintDialog(uiFacade.activeChart)
  }
}

private fun mediaSizes(clazz: KClass<*>): Map<String, MediaSize> =
  clazz.staticProperties.filter {
    it.get() is MediaSize
  }.associate {
    it.name to it.get() as MediaSize
  }

private fun papers(): Map<String, FxPaper> =
    FxPaper::class.staticProperties
      .filter { it.get() is FxPaper }
      .associate { it.name to it.get() as FxPaper }

private const val BASE_PREVIEW_WIDTH = 270.0
private const val BASE_PREVIEW_HEIGHT = 210.0
