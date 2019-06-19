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
import java.text.MessageFormat

class LocalizedString(
    private val key: String,
    private val i18n: Localizer,
    private val observable: SimpleStringProperty = SimpleStringProperty(),
    private var args: List<String> = emptyList()) : ObservableValue<String> by observable {
  init {
    observable.value = build()
  }

  fun update(vararg args: String): LocalizedString {
    this.args = args.toList()
    observable.value = build()
    return this
  }

  private fun build(): String = i18n.formatText(key, *args.toTypedArray())
}

interface Localizer {
  fun create(key: String): LocalizedString
  fun formatText(key: String, vararg args: Any): String
}

object DummyLocalizer : Localizer {
  override fun create(key: String): LocalizedString {
    return LocalizedString(key, this)
  }

  override fun formatText(key: String, vararg args: Any): String {
    return key
  }

}

/**
 * @author dbarashev@bardsoftware.com
 */
class DefaultLocalizer(var rootKey: String = "", private val fallbackLocalizer: Localizer = DummyLocalizer) : Localizer {
  override fun create(key: String): LocalizedString = LocalizedString(key, this)

  override fun formatText(key: String, vararg args: Any): String {
    val key1 = if (this.rootKey != "") "${this.rootKey}.$key" else key
    return if (hasKey(key1)) {
      val message = GanttLanguage.getInstance().getText(key1)
      return if (message == null) key1 else MessageFormat.format(message, *args)

    } else {
      this.fallbackLocalizer.formatText(key, args)
    }
  }

  fun hasKey(key: String): Boolean {
    return GanttLanguage.getInstance().getText(key) != null
  }
}

val RootLocalizer = DefaultLocalizer()
