/*
Copyright 2022 BarD Software s.r.o

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
package biz.ganttproject.app

import biz.ganttproject.core.chart.render.Style
import biz.ganttproject.core.option.*
import biz.ganttproject.lib.fx.AutoCompletionTextFieldBinding
import biz.ganttproject.lib.fx.buildFontAwesomeButton
import com.github.michaelbull.result.onFailure
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.event.EventHandler
import javafx.geometry.HPos
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.Spinner
import javafx.scene.effect.InnerShadow
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.util.StringConverter
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.gui.GPColorChooser
import net.sourceforge.ganttproject.util.BrowserControl
import org.controlsfx.control.textfield.CustomTextField
import java.awt.event.ActionEvent
import java.io.File
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.text.ParsePosition
import java.time.LocalDate

internal interface RowBuilder {
  fun build(grid: GridPane, rowNum: Int): Int
}

private val MIN_COLUMN_WIDTH = 100.0

class PropertySheet(
  val node: Node,
  val validationErrors: ObservableMap<ObservableProperty<*>, String>,
  val isEscCloseEnabled: BooleanProperty
) {
  fun requestFocus() {
    node.requestFocus()
  }

  var isDisable: Boolean
    get() = node.isDisable
    set(value) { node.isDisable = value }
}

class PropertyPaneBuilder(private val localizer: Localizer, private val gridPane: PropertyPane) {
  internal val validationErrors = FXCollections.observableMap(mutableMapOf<ObservableProperty<*>, String>())
  internal val isEscCloseEnabled = SimpleBooleanProperty(true)
  internal val rowBuilders = mutableListOf<RowBuilder>()
  internal var onRequestFocus: (()->Unit)? = null

  fun stylesheet(stylesheet: String) {
    gridPane.stylesheets.add(stylesheet)
  }

  fun title(title: LocalizedString) {
    rowBuilders.add(LabelRowBuilder(title.value, "title"))
  }

  fun title(title: String) = title(localizer.create(title))

  fun skip(rowNum: Int = 1) {
    rowBuilders.add(LabelRowBuilder("\n".repeat(rowNum), "skip"))
  }

  fun text(property: ObservableString, optionValues: (TextDisplayOptions.()->Unit)? = null) {
    rowBuilders.add(run {
      val options = optionValues?.let { TextDisplayOptions().apply(it) }
      createOptionItem(property, createStringOptionEditor(property, options), options)
    })
  }

  fun file(property: ObservableFile, optionValues: (FileDisplayOptions.()->Unit)? = null) {
    rowBuilders.add(run {
      val options = optionValues?.let { FileDisplayOptions().apply(it) }
      createOptionItem(property, createFileOptionEditor(property, options))
    })
  }

  fun checkbox(property: ObservableBoolean) {
    rowBuilders.add(run {
      createOptionItem(property, createBooleanOptionEditor(property))
    })
  }

  fun date(property: ObservableDate, options: (DateDisplayOptions.()->Unit)? = null) {
    rowBuilders.add(run {
      val optionValues = options?.let { DateDisplayOptions().apply(it) } ?: DateDisplayOptions()
      createOptionItem(property, createDateOptionEditor(property, optionValues))
    })
  }

  fun numeric(property: ObservableInt, optionValues: (IntDisplayOptions.()->Unit)? = null) {
    rowBuilders.add(run {
      val options = optionValues?.let { IntDisplayOptions().apply(it) } ?: IntDisplayOptions()
      createOptionItem(property, createIntOptionEditor(property, options))
    })
  }

  fun numeric(property: ObservableDouble) {
    rowBuilders.add(run {
      createOptionItem(property, createDoubleOptionEditor(property))
    })
  }
  fun money(property: ObservableMoney) {
    rowBuilders.add(run {
      createOptionItem(property, createMoneyOptionEditor(property))
    })
  }

  fun <E: Enum<E>> dropdown(property: ObservableEnum<E>, optionValues: (DropdownDisplayOptions<E>.()->Unit)? = null) {
    rowBuilders.add(run {
      val options = optionValues?.let { DropdownDisplayOptions<E>().apply(it) }
      createOptionItem(property, createEnumerationOptionEditor(property, options))
    })
  }

  fun <T> dropdown(property: ObservableChoice<T>, displayOptions: (DropdownDisplayOptions<T>.()->Unit)? = null) {
    rowBuilders.add(run {
      val options = displayOptions?.let { DropdownDisplayOptions<T>().apply(it) }
      createOptionItem(property, createChoiceOptionEditor(property, options))
    })
  }
  fun color(property: ObservableColor) {
    rowBuilders.add(createOptionItem(property, createColorOptionEditor(property)))
  }

  fun custom(property: ObservableProperty<*>, node: Node) {
    rowBuilders.add(createOptionItem(property, node))
  }

  private fun createOptionItem(property: ObservableProperty<*>, editor: Node, options: PropertyDisplayOptions<*>? = null): OptionRowBuilder {
    property.isWritable.addWatcher { evt -> editor.isDisable = !evt.newValue }
    if (onRequestFocus == null) {
      onRequestFocus = { editor.requestFocus() }
    }
    return OptionRowBuilder(property, editor, getOptionLabel(property), options)
  }

  fun <T> add(property: ObservableProperty<T>, displayOptions: PropertyDisplayOptions<T>? = null) {
    rowBuilders.add(run {
      createOptionEditorAndLabel(property)
    })
  }

  private fun createOptionEditorAndLabel(option: ObservableProperty<*>): OptionRowBuilder {
    val editor = when (option) {
      is ObservableBoolean -> createBooleanOptionEditor(option)
      is ObservableString -> createStringOptionEditor(option)
      is ObservableEnum -> createEnumerationOptionEditor(option)
      is ObservableChoice -> createChoiceOptionEditor(option)
      is ObservableFile -> createFileOptionEditor(option)
      is ObservableDate -> createDateOptionEditor(option)
      is ObservableInt -> createIntOptionEditor(option)
      is ObservableDouble -> createDoubleOptionEditor(option)
      is ObservableColor -> createColorOptionEditor(option)
      is ObservableMoney -> createMoneyOptionEditor(option)
      is ObservableNumeric<*> -> error("Can't create editor for ObservableNumeric=${option.id}")
      is ObservableObject<*> -> error("Can't create editor for ObservableObject=${option.id}")
    }
    option.isWritable.addWatcher { evt -> editor.isDisable = !evt.newValue }
    editor.isDisable = option.isWritable.value.not()

    return OptionRowBuilder(option, editor, getOptionLabel(option), null)
  }

  fun createBooleanOptionEditor(option: ObservableBoolean): Node {
    return CheckBox().also {checkBox ->
      checkBox.onAction = EventHandler {
        option.set(checkBox.isSelected, checkBox)
      }
      option.addWatcher { evt ->
        if (evt.trigger != checkBox) {
          checkBox.isSelected = option.value
        }
      }
    }

  }

  private fun <E: Enum<E>> createEnumerationOptionEditor(
    option: ObservableEnum<E>, displayOptions: DropdownDisplayOptions<E>? = null): Node {

    val key2i18n: List<Pair<E, String>> = option.allValues.map {
      it to localizer.formatText("${option.id}.value.${it.name.lowercase()}")
    }.toList()
    return createDropdownEditor(option, key2i18n, displayOptions)
  }


  fun createStringOptionEditor(property: ObservableString, displayOptions: TextDisplayOptions? = null): Node {
    val textField = when {
      property.isScreened -> PasswordField()
      displayOptions?.isMultiline == true -> TextArea().also {
        it.prefColumnCount = displayOptions.columnCount
        it.prefWidth = displayOptions.columnCount * 10.0
      }
      else -> CustomTextField().also {
        AutoCompletionTextFieldBinding(textField = it, suggestionProvider = { req ->
          property.completions(req.userText, it.caretPosition)
        }, converter = { it.text }).also {
          isEscCloseEnabled.bind(it.autoCompletionPopup.showingProperty().not())
        }
        displayOptions?.rightNode?.let { rightNode -> it.right = rightNode }
      }
    }

    val validatedText = textField.textProperty().validated(property.validator)
    property.isWritable.addWatcher {
      if (it.newValue) {
        validatedText.validate(textField.text, null)
      }
    }
    validatedText.addWatcher { evt ->
      property.set(evt.newValue, textField)
    }

    validatedText.validationMessage.addWatcher {
      if (it.newValue == null) {
        textField.markValid()
        validationErrors.remove(property)
      } else {
        textField.markInvalid()
        validationErrors[property] = it.newValue
      }
    }
    property.addWatcher {
      if (it.trigger != textField) {
        textField.text = property.value
      }
    }
    textField.text = property.value
    displayOptions?.editorStyles?.let(textField.styleClass::addAll)
    return textField
  }

  private fun createFileOptionEditor(option: ObservableFile, displayOptions: FileDisplayOptions? = null): Node {
    val textField = CustomTextField()
    val onBrowse = {
      val fileChooser = FileChooser();
      var initialFile: File?  = File(textField.text)
      while (initialFile?.exists() == false) {
        initialFile = initialFile.parentFile
      }
      initialFile?.let {
        if (it.isDirectory) {
          fileChooser.initialDirectory = it
        } else {
          fileChooser.initialDirectory = it.parentFile
        }
      }
      fileChooser.title = "Choose a file"
      displayOptions?.let {
        it.extensionFilters.forEach {filter ->
          fileChooser.extensionFilters.add(FileChooser.ExtensionFilter(filter.description, filter.extensions))
        }
      }
      val resultFile = fileChooser.showOpenDialog(null)
      option.value = resultFile
      resultFile?.let {
        textField.text = it.absolutePath
      }
    }
    textField.right = buildFontAwesomeButton(
      iconName = FontAwesomeIcon.SEARCH.name,
      label = "Browse...",
      onClick = { onBrowse() },
      styleClass = "btn"
    )
    return textField
//    return HBox().apply {
//      HBox.setHgrow(textField, Priority.ALWAYS)
//      children.add(textField)
//      children.add(Region().also {
//        it.padding = Insets(0.0, 5.0, 0.0, 0.0)
//      })
//      children.add(btn)
//    }

  }

  fun createDateOptionEditor(option: ObservableDate, displayOptions: DateDisplayOptions = DateDisplayOptions()): DatePicker {
    return DatePicker(option.value ?: LocalDate.now()).also { picker ->
      option.addWatcher { evt ->
        if (evt.trigger != picker) picker.value = evt.newValue
      }

      val textEditor = picker.editor
      val composedValidator = ValueValidator<LocalDate?> { unvalidatedValue ->
        try {
          val parsedDate = displayOptions.stringConverter.fromString(unvalidatedValue)
            ?: throw ValidationException("The date $unvalidatedValue can't be parsed using the current date format")
          option.validator.invoke(ObservableEvent(option.value, parsedDate, textEditor)).onFailure { msg ->
            throw ValidationException(msg)
          }
          parsedDate
        } catch (ex: ParseException) {
          throw ValidationException("The date $unvalidatedValue can't be parsed using the current date format", ex)
        }
      }
      val validatedText = textEditor.textProperty().validated(composedValidator)
      setupValidation(option, textEditor, validatedText)

      val converter = displayOptions.stringConverter
      option.addWatcher {
        if (it.trigger != textEditor) {
          textEditor.text = converter.toString(option.value)
        }
      }

      option.isWritable.addWatcher {
        if (it.newValue) {
          validatedText.validate(textEditor.text, null)
        }
      }

      picker.valueProperty().subscribe { oldValue, newValue ->
        try {
          option.set(newValue, picker)
        } catch (ex: ValidationException) {
          validatedText.validationMessage.value = ex.message
        }
      }
      setupDisabled(option, picker.disableProperty())
      picker.converter = converter
    }
  }

  private fun createIntOptionEditor(option: ObservableInt, displayOptions: IntDisplayOptions = IntDisplayOptions()): Node {
    return Spinner<Int>().also { spinner ->
      spinner.isEditable = true
      val valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(displayOptions.minValue, displayOptions.maxValue, option.value)
      spinner.valueFactory = valueFactory
      spinner.valueProperty().subscribe { oldValue, newValue ->
        option.set(newValue, spinner)
      }
      option.addWatcher {
        if (it.trigger != spinner) {
          valueFactory.value = it.newValue
        }
      }
    }
  }

  private fun <T> setupDisabled(property: ObservableProperty<T>, editorDisabled: BooleanProperty) {
    editorDisabled.set(property.isWritable.value.not())
    property.isWritable.addWatcher { evt -> editorDisabled.set(!evt.newValue) }
  }

  private fun <T> setupValidation(property: ObservableProperty<T>, textField: TextField, validatedText: ValidatedObservable<T>) {
    validatedText.validationMessage.addWatcher {
      if (it.newValue == null) {
        textField.markValid()
        validationErrors.remove(property)
      } else {
        textField.markInvalid()
        validationErrors[property] = it.newValue
      }
    }
    validatedText.addWatcher { evt ->
      evt.newValue?.let {
        property.set(it, textField)
      }
    }
  }

  private fun createDoubleOptionEditor(property: ObservableDouble): Node {
    return TextField().also { textField ->
      val validatedText = textField.textProperty().validated(DoubleValidator)
      setupValidation(property, textField, validatedText)
      property.addWatcher {
        if (it.trigger != textField) {
          textField.text = it.newValue.toString()
        }
      }

      setupDisabled(property, textField.disableProperty())
      textField.text = property.value.toString()
    }
  }

  private fun createMoneyOptionEditor(property: ObservableMoney): Node {
    return TextField().also { textField ->
      val validatedText = textField.textProperty().validated(MoneyValidator)
      setupValidation(property, textField, validatedText)
      property.addWatcher {
        if (it.trigger != textField) {
          textField.text = it.newValue.toString()
        }
      }
      setupDisabled(property, textField.disableProperty())
      textField.text = property.value.toString()
    }
  }

  private fun _createIntOptionEditor(option: ObservableInt): Node {
    return TextField().also { textField ->
      val validatedText = textField.textProperty().validated(option.validator)
      option.isWritable.addWatcher {
        if (it.newValue) {
          validatedText.validate(textField.text, null)
        }
      }
      validatedText.addWatcher { evt ->
        evt.newValue?.let {option.set(it, textField)}
      }
      validatedText.validationMessage.addWatcher {
        if (it.newValue == null) {
          textField.markValid()
        } else {
          textField.markInvalid()
        }
      }
      option.addWatcher {
        if (it.trigger != textField) {
          textField.text = option.value.toString()
        }
      }
      textField.text = option.value.toString()
    }
  }

  fun createColorOptionEditor(option: ObservableColor): Node {
    return ColorPicker(option.value?.javaFXColor ?: Color.WHITE).also { picker ->
      picker.customColors.addAll(GPColorChooser.getRecentColorsOption().values.map { ColorOption.Util.awtColorToFxColor(it) }.toList())
      option.addWatcher { evt ->
        if (evt.trigger != picker) picker.value = evt.newValue?.javaFXColor
      }
      picker.valueProperty().subscribe { _, newValue ->
        option.set(Style.Color.parse(ColorOption.Util.getColor(newValue)), picker)
        GPColorChooser.addRecentColor(ColorOption.Util.fxColorToAwtColor(newValue))
      }
    }
  }
  private fun createNoEditor(option: GPOption<*>) = Label(option.value?.toString())


  private fun getOptionLabel(option: ObservableProperty<*>) = localizer.formatText("${option.id}.label")

  inner class OptionRowBuilder(
    val option: ObservableProperty<*>,
    val editor: Node,
    val label: String?,
    val options: PropertyDisplayOptions<*>?
  ): RowBuilder {
    private fun createLabel(item: OptionRowBuilder): Label {
      return Label(item.label)
    }

    override fun build(grid: GridPane, idx: Int): Int {
      var resultRow = idx
      if (label != null) {
        val label = createLabel(this)
        when (options?.labelPosition ?: LabelPosition.LEFT) {
          LabelPosition.LEFT -> {
            grid.add(label, 0, idx)
            GridPane.setHgrow(label, Priority.NEVER)
            GridPane.setHalignment(label, HPos.RIGHT)
          }
          LabelPosition.ABOVE -> {
            grid.add(label, 0, idx, 2, 1)
            GridPane.setHgrow(label, Priority.NEVER)
            GridPane.setHalignment(label, HPos.LEFT)
            resultRow++
          }
        }

        if (editor is Region) {
          editor.minWidth = MIN_COLUMN_WIDTH
          editor.maxWidth = Double.MAX_VALUE
        }
        if (editor is TextArea) {
          GridPane.setValignment(label, VPos.TOP)
        }
        label.labelFor = editor
        HBox(editor).also {hbox ->
          HBox.setHgrow(editor, Priority.ALWAYS)

          getOptionHelpUrl(option)?.let { url ->
            hbox.children.add(createButton(OpenUrlAction(url), onlyIcon = true)?.also {
              it.styleClass.add("btn-help-url")
            })
          }
          when (options?.labelPosition ?: LabelPosition.LEFT) {
            LabelPosition.LEFT -> {
              grid.add(hbox, 1, idx)
              GridPane.setHgrow(hbox, Priority.SOMETIMES)
            }
            LabelPosition.ABOVE -> {
              grid.add(hbox, 0, resultRow, 2, 1)
              GridPane.setHgrow(hbox, Priority.ALWAYS)
            }
          }
        }

        if (idx == 0) {
          grid.focusedProperty().addListener { _, oldValue, newValue ->
            if (!oldValue && newValue) {
              editor.requestFocus()
            }
          }
        }
      }
      return resultRow
    }
  }

  inner class LabelRowBuilder(val text: String, val styleClass: String): RowBuilder {
    override fun build(grid: GridPane, rowNum: Int): Int {
      gridPane.add(Label(text).also { it.styleClass.add(styleClass) }, 0, rowNum, 2, 1)
      return rowNum
    }
  }

  private fun getOptionHelpUrl(option: ObservableProperty<*>) = localizer.formatTextOrNull("${option.id}.helpUrl")

}

/**
 * Provides a small DSL for building a property sheet.
 */
class PropertySheetBuilder(private val localizer: Localizer) {

  fun pane(code: PropertyPaneBuilder.()->Unit): PropertySheet {
    val gridPane = PropertyPane().also {
      it.styleClass.add("property-pane")
      it.stylesheets.add("/biz/ganttproject/app/PropertySheet.css")
    }
    gridPane.columnConstraints.add(ColumnConstraints().also {
      it.isFillWidth = true
    })
    val paneBuilder = PropertyPaneBuilder(localizer, gridPane).apply(code)
    var rowNum = 1
    paneBuilder.rowBuilders.forEachIndexed { _, builder ->
      rowNum = builder.build(gridPane, rowNum) + 1
    }
    gridPane.focusedProperty().addListener { _, _, newValue ->
      if (newValue) {
        paneBuilder.onRequestFocus?.invoke()
      }
    }
    return PropertySheet(gridPane, paneBuilder.validationErrors, paneBuilder.isEscCloseEnabled)
  }


  fun createPropertySheet(options: List<ObservableProperty<*>>): PropertySheet {
    return pane {
      options.forEach {
        add(it)
      }
    }
  }
}


class PropertyPane : GridPane() {
  init {
    vgap = 5.0
    hgap = 10.0
    //padding = Insets(5.0, 15.0, 5.0, 15.0)
    styleClass.add("property-pane")
  }
}

private fun Node.markValid() {
  this.styleClass.remove("validation-error")
  this.effect = null
}

private fun Node.markInvalid() {
  if (!this.styleClass.contains("validation-error")) {
    this.styleClass.add("validation-error")
    this.effect = InnerShadow(10.0, Color.RED)
  }
}

private class OpenUrlAction(private val url: String): GPAction("help.openUrl") {
  override fun actionPerformed(e: ActionEvent?) {
    BrowserControl.displayURL(url)
  }
}

enum class LabelPosition {
  LEFT, ABOVE
}
sealed class PropertyDisplayOptions<P> {
  var labelPosition: LabelPosition = LabelPosition.LEFT
  val editorStyles = mutableListOf<String>()
}
data class TextDisplayOptions(
  var isMultiline: Boolean = false,
  var isScreened: Boolean = false,
  var columnCount: Int = 40,
  var rightNode: Node? = null,
): PropertyDisplayOptions<String?>()
data class FileExtensionFilter(val description: String, val extensions: List<String>)
data class FileDisplayOptions(val extensionFilters: MutableList<FileExtensionFilter> = mutableListOf<FileExtensionFilter>()): PropertyDisplayOptions<File>()
data class IntDisplayOptions(
  var minValue: Int = 0,
  var maxValue: Int = Int.MAX_VALUE,
) : PropertyDisplayOptions<Int>()

data class DateDisplayOptions(
  var stringConverter: StringConverter<LocalDate> = createDateConverter()
): PropertyDisplayOptions<LocalDate>()

data class DropdownDisplayOptions<E>(
  var cellFactory: ((ListCell<Pair<E, String>>, Pair<E, String>) -> Node)? = null
): PropertyDisplayOptions<E>()

/**
 * Validator that checks if the text input can be parsed as a monetary value, using the currently chosen locale settings.
 */
object MoneyValidator: ValueValidator<BigDecimal> {
  private val format: NumberFormat = NumberFormat.getNumberInstance(getCurrentLocale()).also {
    (it as DecimalFormat).isParseBigDecimal = true
  }

  override fun parse(text: String): BigDecimal =
    try {
      if (text.isBlank()) BigDecimal.ZERO
      else {
        val trimmed = text.trim()
        val pos = ParsePosition(0)
        val num = format.parse(trimmed, pos)
        if (pos.index != trimmed.length || pos.errorIndex != -1) {
          throw ValidationException("Failed to parse $text using current currency format")
        }
        num as BigDecimal
      }
    } catch (ex: ParseException) {
      ex.printStackTrace()
      throw ValidationException(ex)
    }
}

/**
 * Validator that checks if the text input can be parsed as a decimal value, using the currently chosen locale settings.
 */
object DoubleValidator: ValueValidator<Double> {
  private val format: NumberFormat = NumberFormat.getNumberInstance(getCurrentLocale())

  override fun parse(text: String): Double =
    try {
      if (text.isBlank()) 0.0 else format.parse(text).toDouble()
    } catch (ex: ParseException) {
      ex.printStackTrace()
      throw ValidationException(ex)
    }
}
