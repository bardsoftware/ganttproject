/*
Copyright 2019-2020 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.app

import biz.ganttproject.lib.fx.VBoxBuilder
import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane

const val OPTION_PANE_STYLESHEET = "/biz/ganttproject/app/OptionPane.css"

/**
 * Input data of a single option: its key for i18n purposes, user data for the handler and flag indicating if option
 * is initially selected.
 */
data class OptionElementData<T>(val i18nKey: String, val userData: T, val isSelected: Boolean = false, val customContent: Node? = null)

/**
 * This is a helper class which builds a UI widget consisting of a few mutually exclusive options where user is supposed
 * to choose one.
 *
 * The widget can be used inside other widgets or shown in a dialog. It thus becomes slightly more advanced alternative
 * to JOptionPane class.
 */
class OptionPaneBuilder<T> {
  var i18n = RootLocalizer
  val titleString: LocalizedString by lazy { i18n.create("title") }
  val titleHelpString: LocalizedString? by lazy { i18n.create("titleHelp") }


  /**
   * Style class added to the widget
   */
  var styleClass = ""

  /**
   * Stylesheet which is associated with the widget
   */
  var styleSheets: MutableList<String> = mutableListOf(OPTION_PANE_STYLESHEET)

  /**
   * Graphic node shown in the left side of the widget
   */
  var graphic: Node? = null

  /**
   * The list of option elements.
   */
  var elements: List<OptionElementData<T>> = listOf()

  var toggleGroup: ToggleGroup = ToggleGroup()

  fun buildPane(): Pane {
    return buildPaneImpl(this.toggleGroup).also {
      it.styleClass.add(this@OptionPaneBuilder.styleClass)
    }
  }

  private fun buildPaneImpl(lockGroup: ToggleGroup): Pane {
    val vbox = VBoxBuilder()
    this.elements.forEach {
      val btn = RadioButton().also { btn ->
        btn.textProperty().bind(i18n.create(it.i18nKey))
        btn.styleClass.add("btn-option")
        btn.userData = it.userData
        btn.toggleGroup = lockGroup
        btn.isSelected = it.isSelected
      }
      vbox.add(btn)

      if (this.i18n.formatTextOrNull("${it.i18nKey}.help") != null) {
        vbox.add(Label().apply {
          this.textProperty().bind(i18n.create("${it.i18nKey}.help"))
          this.styleClass.add("option-help")
        })
      }
      it.customContent?.let(vbox::add)
    }
    return vbox.vbox
  }

  fun createHeader() = BorderPane().apply {
      styleClass.add("header")
      center = VBoxBuilder().apply {
        addTitle(this@OptionPaneBuilder.titleString.value)
        add(Label().apply {
          this.textProperty().bind(this@OptionPaneBuilder.titleHelpString)
          this.styleClass.add("help")
        })
      }.vbox
      this@OptionPaneBuilder.graphic?.let {graphic ->
        this.right = graphic
        graphic.styleClass.add("img")
      }
    }

  /**
   * Shows option dialog. When dialog is closed, calls optionHandler with the selected value.
   */
  fun showDialog(optionHandler: (T) -> Unit) {
    dialog {
      val lockGroup = ToggleGroup()

      this.styleSheets.forEach { styleSheet ->
        it.addStyleSheet(styleSheet)
      }
      it.addStyleClass(this.styleClass)
      it.addStyleClass("option-pane-padding")

      // Dialog .header includes .title and .help labels and .img graphic on the right side if specified
      it.setHeader(createHeader())
      // Dialog content is .content-pane
      it.setContent(buildPaneImpl(lockGroup).apply {
        styleClass.add("option-pane")
      })
      // The only available button is OK which is .btn-attention
      it.setupButton(ButtonType.OK) {btn ->
        btn.styleClass.add("btn-attention")
        btn.addEventHandler(ActionEvent.ACTION) {
          optionHandler(lockGroup.selectedToggle.userData as T)
        }
      }
    }
  }
}
