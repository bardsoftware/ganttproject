/*
Copyright 2022-2026 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.core.option

import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.util.StringConverter
import java.io.File
import java.time.LocalDate

/**
 * Interface of a property pane builder. It adds UI elements for editing properties one by one, one
 * row per element.
 */
interface PropertyPaneBuilder {
  /**
   * Adds a checkbox editor for the given boolean property.
   */
  fun checkbox(property: ObservableBoolean)

  /**
   * Adds a dropdown editor for the given property with enum values.
   */
  fun <E: Enum<E>> dropdown(property: ObservableEnum<E>, optionValues: (DropdownDisplayOptions<E>.()->Unit)? = null)

  /**
   * Adds a dropdown editor for the given property with the list values.
   */
  fun <T> dropdown(property: ObservableChoice<T>, displayOptions: (DropdownDisplayOptions<T>.() -> Unit)? = null)

  /**
   * Adds an editor for the given property with the list of files.
   */
  fun files(property: ObservableFiles, optionValues: (FileDisplayOptions.() -> Unit)? = null)

  fun text(property: ObservableString, optionValues: (TextDisplayOptions.() -> Unit)? = null)

  /**
   * Adds a date editor for the given property.
   */
  fun date(property: ObservableDate, options: (DateDisplayOptions.() -> Unit)? = null)

  /**
   * Adds an integer editor for the given property.
   */
  fun numeric(property: ObservableInt, options: (IntDisplayOptions.() -> Unit)? = null)

  fun title(title: String)
}

enum class LabelPosition {
  LEFT, ABOVE
}

/**
 * A family of options for displaying properties in a property pane.
 */
sealed class PropertyDisplayOptions<P> {
  var labelPosition: LabelPosition = LabelPosition.LEFT
  val editorStyles = mutableListOf<String>()
}

/**
 * Options for displaying text fields in a property pane.
 */
data class TextDisplayOptions(
  var isMultiline: Boolean = false,
  var isScreened: Boolean = false,
  var columnCount: Int = 40,
  var rightNode: Node? = null,
) : PropertyDisplayOptions<String?>()

data class FileExtensionFilter(val description: String, val extensions: List<String>)

/**
 * Options for displaying file chooser fields in a property pane.
 */
data class FileDisplayOptions(
  var isSaveNotOpen: Boolean = false,
  var allowMultipleSelection: Boolean = false,
  var browseButtonText: String = "Browse...",
  var chooserTitle: String = "Choose a file",
  val extensionFilters: MutableList<FileExtensionFilter> = mutableListOf())
  : PropertyDisplayOptions<File>()

/**
 * Options for displaying integer fields in a property pane.
 */
data class IntDisplayOptions(
  var minValue: Int = 0,
  var maxValue: Int = Int.MAX_VALUE,
) : PropertyDisplayOptions<Int>()

/**
 * Options for displaying date chooser fields in a property pane.
 */
data class DateDisplayOptions(
  var stringConverter: StringConverter<LocalDate>// = createDateConverter()
) : PropertyDisplayOptions<LocalDate>()

/**
 * Options for displaying dropdown fields in a property pane.
 */
data class DropdownDisplayOptions<E>(
  var cellFactory: ((ListCell<Pair<E, String>>, Pair<E, String>) -> Node)? = null
) : PropertyDisplayOptions<E>()
