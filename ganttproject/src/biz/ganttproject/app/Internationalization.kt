/*
Copyright 2019 BarD Software s.r.o

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

import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import net.sourceforge.ganttproject.language.GanttLanguage

class LocalizedString(
    private val key: String,
    private val i18n: DefaultStringSupplier,
    private val observable: SimpleStringProperty = SimpleStringProperty(),
    private var args: List<String> = emptyList()) : ObservableValue<String> by observable {
  init {
    observable.value = build()
  }

  fun update(vararg args: String): LocalizedString {
    this.args = listOf(*args)
    observable.value = build()
    return this
  }

  private fun build(): String =
      if (i18n.rootKey == "") i18n.formatText(key, this.args)
      else i18n.formatText("${i18n.rootKey}.$key", this.args)
}

/**
 * @author dbarashev@bardsoftware.com
 */
class DefaultStringSupplier(var rootKey: String = "") {
  fun create(key: String): LocalizedString = LocalizedString(key, this)

  fun formatText(key: String, vararg args: Any): String {
    return GanttLanguage.getInstance().formatText(key, *args)
  }

  fun hasKey(key: String): Boolean {
    return GanttLanguage.getInstance().getText(key) != null
  }

}
