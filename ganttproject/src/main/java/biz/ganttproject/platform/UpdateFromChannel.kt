/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
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
package biz.ganttproject.platform

import biz.ganttproject.FXUtil
import biz.ganttproject.app.Localizer
import biz.ganttproject.lib.fx.createToggleSwitch
import biz.ganttproject.lib.fx.openInBrowser
import biz.ganttproject.lib.fx.vbox
import com.bardsoftware.eclipsito.update.UpdateMetadata
import com.sandec.mdfx.MDFXNode
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import org.controlsfx.control.HyperlinkLabel
import kotlin.collections.set

class UpdateFromChannel(private val model: UpdateDialogModel, private val localizer: Localizer) {
  // Maps version to the UI component that shows its state.
  internal val version2ui = mutableMapOf<String, UpdateComponentUi>()
  internal val resizer: ()->Unit = {}

  internal val progressText = localizer.create("installProgress")
  internal val progressLabel = Label().also {
    it.textProperty().bind(progressText)
    it.styleClass.add("progress")
    it.isVisible = false
  }

  private val toggleButtonText get() =
    when (model.state) {
      ApplyAction.DOWNLOAD_MAJOR -> localizer.formatText("platform.update.toggleMajorMinor.showMinor")
      ApplyAction.INSTALL_FROM_CHANNEL -> localizer.formatText("platform.update.toggleMajorMinor.showMajor")
      else -> error("Unexpected state")
    }

  internal val majorUpdatesUi by lazy {
    model.majorUpdate?.let { MajorUpdateUi(it) }
  }

  internal val minorUpdatesUi by lazy {
    val minorUpdates = model.minorUpdates.map {
      UpdateComponentUi(localizer, it).also { ui ->
        version2ui[it.version] = ui
      }
    }
    val updateList = vbox {
      addClasses("minor-update", "scroll-body")
      minorUpdates.forEach {
        add(it.title)
        add(it.subtitle)
        add(it.text)
      }
    }
    ScrollPane(updateList).also {
      it.isFitToWidth = true
      it.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
      it.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
    }
  }

  internal val node by lazy {
    vbox {
      addClasses("content-pane")

      // The upper part of the dialog which shows the running version number and a toggle to switch
      // the update checking on and off.
      val props = createGridPane(localizer, model)
      this@vbox.add(props)

      val wrapperPane = BorderPane().also {
        it.styleClass.addAll("major-update", "minor-update")
      }
      val showMinorUpdate = {
        FXUtil.transitionCenterPane(wrapperPane, minorUpdatesUi, resizer)
      }
      val showMajorUpdate = {
        FXUtil.transitionCenterPane(wrapperPane, majorUpdatesUi?.component, resizer)
      }
      if (model.hasUpdates) {
        wrapperPane.top = Label().apply {
          styleClass.add("title")
          textProperty().bind(model.stateTitleProperty)
        }
        model.stateProperty.subscribe { oldState, newState ->
          if (oldState == ApplyAction.DOWNLOAD_MAJOR && newState == ApplyAction.INSTALL_FROM_CHANNEL) {
            showMinorUpdate()
          } else if (oldState == ApplyAction.INSTALL_FROM_CHANNEL && newState == ApplyAction.DOWNLOAD_MAJOR) {
            showMajorUpdate()
          }
        }

        if (model.hasMinorUpdates && model.hasMajorUpdates) {
          wrapperPane.bottom = Button(toggleButtonText).also {btn ->
            btn.styleClass.addAll("btn", "btn-regular")
            btn.addEventFilter(ActionEvent.ACTION) {
              if (model.state == ApplyAction.DOWNLOAD_MAJOR) {
                model.state = ApplyAction.INSTALL_FROM_CHANNEL
                btn.text = toggleButtonText
              } else if (model.state == ApplyAction.INSTALL_FROM_CHANNEL) {
                model.state = ApplyAction.DOWNLOAD_MAJOR
                btn.text = toggleButtonText
              }
            }
          }
        }

        add(wrapperPane)
        if (model.state == ApplyAction.INSTALL_FROM_CHANNEL) {
          showMinorUpdate()
        }
        if (model.state == ApplyAction.DOWNLOAD_MAJOR) {
          showMajorUpdate()
        }
      }
    }
  }

  // This function creates a monitor that will show the progress of installing the given update.
  internal fun startProgressMonitor(version: String): InstallProgressMonitor? =
    this.version2ui[version]?.let { minorUpdateUi ->
      val progressMonitor: InstallProgressMonitor = { percents: Int ->
        FXUtil.runLater {
          if (minorUpdateUi.progressValue == -1) {
            this.progressLabel.isVisible = true
          }
          if (minorUpdateUi.progressValue != percents) {
            minorUpdateUi.progressValue = percents
            progressText.update(version, percents.toString())
            if (percents == 100) {
              this.progressLabel.isVisible = false
              this.progressText.clear()
            }
          }
        }
      }
      progressMonitor
    }
}

/**
 * Encapsulates UI controls which show information and status of individual updates.
 */
internal class UpdateComponentUi(ourLocalizer: Localizer, update: UpdateMetadata) {
  val title: Label = Label(ourLocalizer.formatText("bodyItem.title", update.version)).also { l ->
    l.styleClass.add("title")
  }
  val subtitle: Label = Label(ourLocalizer.formatText("bodyItem.subtitle", update.date, update.sizeAsString())).also { l ->
    l.styleClass.add("subtitle")
  }
  val text: MDFXNode = MDFXNode(ourLocalizer.formatText("bodyItem.description", update.description)).also { l ->
    l.styleClass.add("par")
  }
  var progressValue: Int = -1
    set(value) {
      field = value
      if (value == 100) {
        listOf(title, subtitle, text).forEach { it.opacity = 0.5 }
      }
    }
}

internal fun createGridPane(ourLocalizer: Localizer, model: UpdateDialogModel) = GridPane().apply {
  styleClass.add("props")
  add(Label(ourLocalizer.formatText("installedVersion")).also {
    GridPane.setMargin(it, Insets(5.0, 10.0, 3.0, 0.0))
  }, 0, 0)
  add(Label(model.installedVersion).also {
    GridPane.setMargin(it, Insets(5.0, 0.0, 3.0, 0.0))
  }, 1, 0)
  add(Label(ourLocalizer.formatText("checkUpdates")).also {
    GridPane.setMargin(it, Insets(5.0, 10.0, 3.0, 0.0))
  }, 0, 1)
  val toggleSwitch = createToggleSwitch().also {
    it.selectedProperty().value = UpdateOptions.isCheckEnabled.value
    it.selectedProperty().addListener { _, _, newValue -> UpdateOptions.isCheckEnabled.value = newValue }
  }
  add(toggleSwitch, 1, 1)
  add(HyperlinkLabel(ourLocalizer.formatText("checkUpdates.helpline")).also {
    it.styleClass.add("helpline")
    it.onAction = EventHandler { openInBrowser(PRIVACY_URL) }
  }, 1, 2)
}

private fun (UpdateMetadata).sizeAsString(): String {
  return when {
    this.sizeBytes < (1 shl 10) -> """${this.sizeBytes}b"""
    this.sizeBytes >= (1 shl 10) && this.sizeBytes < (1 shl 20) -> """${this.sizeBytes / (1 shl 10)}KiB"""
    else -> "%.2fMiB".format(this.sizeBytes.toFloat() / (1 shl 20))
  }
}


