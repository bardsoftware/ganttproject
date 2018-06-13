// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud

import biz.ganttproject.FXUtil
import biz.ganttproject.storage.StorageDialogBuilder
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.document.Document
import org.controlsfx.control.HyperlinkLabel
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudStorage(private val myMode: StorageDialogBuilder.Mode, private val myOptions: GPCloudStorageOptions, private val myOpenDocument: Consumer<Document>, private val myDialogUi: StorageDialogBuilder.DialogUi) : StorageDialogBuilder.Ui {
  private val myPane: BorderPane
  private val myButtonPane: HBox
  private val myNextButton: Button


  internal interface PageUi {
    fun createPane(): CompletableFuture<Pane>
  }

  init {
    myPane = BorderPane()
    myButtonPane = HBox()
    myButtonPane.styleClass.add("button-pane")
    myButtonPane.alignment = Pos.CENTER
    myNextButton = Button("Continue")
    myButtonPane.children.add(myNextButton)
    myNextButton.visibleProperty().value = false
    myPane.bottom = myButtonPane
  }

  override fun getName(): String {
    return "GanttProject Cloud"
  }

  override fun createSettingsUi(): Optional<Pane> {
    return Optional.empty()
  }

  override fun getCategory(): String {
    return "cloud"
  }

  override fun createUi(): Pane {
    return doCreateUi()
  }

  private fun doCreateUi(): Pane {
    val signupPane = GPCloudSignupPane(Consumer { this.nextPage(it) })
    val cloudServer = myOptions.cloudServer
    if (cloudServer.isPresent) {
    } else {
      signupPane.createPane().thenApply { pane -> nextPage(pane) }
    }
    return myPane
  }

  private fun nextPage(newPage: Pane): Pane {
    FXUtil.transitionCenterPane(myPane, newPage) { myDialogUi.resize() }
    return newPage
  }

  companion object {

    internal fun newLabel(key: String, vararg classes: String): Label {
      val label = Label(key)
      label.styleClass.addAll(*classes)
      return label
    }

    internal fun newHyperlink(eventHandler: EventHandler<ActionEvent>, text: String, vararg classes: String): HyperlinkLabel {
      val result = HyperlinkLabel(text)
      result.addEventHandler(ActionEvent.ACTION, eventHandler)
      result.styleClass.addAll(*classes)
      return result
    }
  }
}
