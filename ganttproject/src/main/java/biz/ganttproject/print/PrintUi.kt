package biz.ganttproject.print

import biz.ganttproject.app.FXToolbarBuilder
import biz.ganttproject.app.dialog
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.control.ButtonType
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.chart.Chart
import net.sourceforge.ganttproject.gui.UIFacade
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * @author dbarashev@bardsoftware.com
 */
fun showPrintDialog(activeChart: Chart) {
  val channel = Channel<File>()
  createImages(activeChart, 2, 1, channel)
  dialog { dlg ->
    val contentPane = BorderPane().also {
      it.top = FXToolbarBuilder().addButton(GPAction.create("zoom.in") {
        Previews.zoomIn()
      }).addButton(GPAction.create("zoom.out") {
        Previews.zoomOut()
      }).build().toolbar
      //it.background = Background(BackgroundFill(Color.BLACK, null, null))
    }
    contentPane.center = Previews.gridPane
    dlg.setContent(contentPane)
    dlg.setupButton(ButtonType.APPLY) {
      it.text = "Print"
      it.onAction = EventHandler {
        printPages(Previews.imageFiles)
      }
    }
    Previews.readImages(channel)
  }
}

private data class PageFormat(val name: String, val width: Double, val height: Double)
private object Previews {
  private val readImageScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
  val gridPane = GridPane().also {
    it.vgap = 10.0
    it.hgap = 10.0
  }

  var pageFormat = PageFormat("A4", 270.0, 210.0)
  var zoomFactor = 1.0

  var imageFiles: List<File> = listOf()
  set(value) {
    field = value
    updatePreviews()
  }

  private fun updatePreviews() {
    Platform.runLater {
      gridPane.children.clear()
      imageFiles.forEachIndexed { index, file ->
        ImageView(
          Image(
            file.inputStream(),
            pageFormat.width * zoomFactor,
            pageFormat.height * zoomFactor,
            true,
            true
          )
        ).also {
          gridPane.add(it, index, 0)
        }
      }
    }
  }

  fun zoomIn() {
    zoomFactor *= 2
      updatePreviews()
  }
  fun zoomOut() {
    zoomFactor = max(1.0, zoomFactor/2.0)
      updatePreviews()
  }

  fun readImages(channel: Channel<File>) {
    readImageScope.launch {
      imageFiles = channel.receiveAsFlow().toList()
    }
  }
}

fun createPrintAction(uiFacade: UIFacade): GPAction {
  return GPAction.create("print") {
    showPrintDialog(uiFacade.activeChart)
  }
}
