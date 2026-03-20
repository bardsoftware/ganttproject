package biz.ganttproject.lib.fx

import biz.ganttproject.createButton
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableStringValue
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import net.sourceforge.ganttproject.action.GPAction

data class HBoxBuilder(
  var label: ObservableStringValue = SimpleStringProperty(""),
  val actions: MutableList<GPAction> = mutableListOf(),
  var isSelected: Boolean = false,
  val styleClasses: MutableList<String> = mutableListOf()
) {
  fun build(): Region {
    val btnBox = HBox().also {
      it.children.addAll(actions.map(::createButton))
      it.styleClass.add("action-buttons")
    }
    return HBox().also {
      it.children.addAll(listOf(Label().also { labelControl ->
        labelControl.textProperty().bind(label)
        labelControl.maxWidth = Double.MAX_VALUE
        HBox.setHgrow(labelControl, Priority.ALWAYS)
      }, btnBox))
      it.styleClass.addAll(styleClasses)
      it.styleClass.add("hbox")
      if (isSelected) {
        it.styleClass.add("selected")
      }
    }
  }

  fun String.asObservable() = SimpleStringProperty(this)
}

fun hbox(init: HBoxBuilder.() -> Unit) = HBoxBuilder().apply(init).build()