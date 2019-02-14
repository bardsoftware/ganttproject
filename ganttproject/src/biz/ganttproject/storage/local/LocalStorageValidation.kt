package biz.ganttproject.storage.local

import biz.ganttproject.storage.StorageMode
import net.sourceforge.ganttproject.language.GanttLanguage
import org.controlsfx.validation.ValidationResult
import org.controlsfx.validation.Validator
import java.util.function.Supplier

/**
 * @author dbarashev@bardsoftware.com
 */
class ValidationHelper(
    private val isListEmpty: Supplier<Boolean>,
    val state: LocalStorageState) {
  val validator: Validator<String> = Validator { control, value ->
    if (value == null) {
      return@Validator ValidationResult()
    }
    try {
      if (value == "") {
        return@Validator ValidationResult.fromWarning(control, "Type file name")
      }
      state.trySetFile(value)
      return@Validator ValidationResult()
    } catch (e: StorageMode.FileException) {
      when {
        "document.storage.error.read.notExists" == e.message && !isListEmpty.get() ->
          return@Validator ValidationResult.fromWarning(control, GanttLanguage.getInstance().formatText(e.message, e.args))
        else -> return@Validator ValidationResult.fromError(control, GanttLanguage.getInstance().formatText(e.message, e.args))
      }
    }
  }
}

