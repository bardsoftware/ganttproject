/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

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
package net.sourceforge.ganttproject.gui.resourceproperties

import biz.ganttproject.app.PropertySheetBuilder
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.i18n
import biz.ganttproject.colorFromUiManager
import biz.ganttproject.core.option.ObservableChoice
import biz.ganttproject.core.option.ObservableDouble
import biz.ganttproject.core.option.ObservableMoney
import biz.ganttproject.core.option.ObservableString
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.StackPane
import javafx.util.StringConverter
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.roles.Role
import net.sourceforge.ganttproject.roles.RoleManager

class MainPropertiesPanel(private val resource: HumanResource) {
  val title: String = RootLocalizer.formatText("general")
  val fxComponent by lazy { getFxNode() }
  val validationErrors = FXCollections.observableArrayList<String>()

  private val nameOption = ObservableString("name", resource.name)
  private val phoneOption = ObservableString("phone", resource.phone)
  private val emailOption = ObservableString("email", resource.mail)
  private val roleOption = ObservableChoice<Role>("role", resource.role,
    RoleManager.Access.getInstance().getEnabledRoles().toList(), roleStringConverter)
  private val rateOption = ObservableMoney("standardRate", resource.standardPayRate)
  private val totalCostOption = ObservableMoney("totalCost", resource.totalCost).also {
    it.setWritable(false)
  }
  private val totalLoadOption = ObservableDouble("totalLoad", resource.totalLoad).also {
    it.setWritable(false)
  }
  private var onRequestFocus = {}

  private fun getFxNode() = StackPane().apply {
    background = Background(BackgroundFill("Panel.background".colorFromUiManager(), CornerRadii.EMPTY, Insets.EMPTY))
    val pane = PropertySheetBuilder(i18n).pane {
      stylesheet("/biz/ganttproject/task/TaskPropertiesDialog.css")
      title("section.main")
      text(nameOption)
      text(phoneOption)
      text(emailOption)
      dropdown(roleOption)

      skip()
      title("section.rate")
      money(rateOption)
      money(totalCostOption)
      numeric(totalLoadOption)
    }
    onRequestFocus = pane::requestFocus
    children.add(pane.node)
  }

  fun requestFocus() = onRequestFocus()

  fun save() {
    nameOption.ifChanged(resource::setName)
    phoneOption.ifChanged(resource::setPhone)
    emailOption.ifChanged(resource::setMail)
    roleOption.ifChanged(resource::setRole)
    rateOption.ifChanged(resource::setStandardPayRate)
  }

}

private val roleStringConverter = object : StringConverter<Role>() {
  override fun toString(role: Role): String  = role.name
  override fun fromString(string: String): Role? = RoleManager.Access.getInstance().getRole(string)
}

private val i18n = i18n {
  // We will search for the translation corresponding to a structured key in the current language only.
  default(withFallback = false)
  prefix("option.personProperties.main") {
    // If there is no translation, we'll search for the translation corresponding to the previously used unstructured key,
    // again in the current language only.
    default(withFallback = false)
    transform { key ->
      val key1 = when {
        key.endsWith(".label") -> key.removeSuffix(".label")
        else -> key
      }
      val map = mapOf(
        "phone" to "colPhone",
        "email" to "colMail",
        "role" to "colRole",
        "standardRate" to "colStandardRate",
        "totalCost" to "colTotalCost",
        "totalLoad" to "colTotalLoad",
        "section.rate" to "optionGroup.resourceRate.label"
      )
      map[key1] ?: key1
    }
    fallback {
      // Finally, we'll use the English translation of a structured key.
      default()
      prefix("option.personProperties.main")
    }
  }
}
