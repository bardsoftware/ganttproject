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
package biz.ganttproject.storage.local

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.storage.BROWSE_PANE_LOCALIZER
import biz.ganttproject.storage.StorageMode
import org.controlsfx.validation.ValidationResult
import org.controlsfx.validation.Validator
import java.util.function.Supplier

/**
 * Creates a validator of the typed file names.
 *
 * @author dbarashev@bardsoftware.com
 */
fun createLocalStorageValidator(
    isListEmpty: Supplier<Boolean>,
    state: LocalStorageState): Validator<String> {
  return Validator { control, value ->
    if (value == null) {
      return@Validator ValidationResult()
    }
    if (value.isBlank()) {
      return@Validator ValidationResult.fromWarning(control, i18n.formatText("validation.emptyFileName"))
    }
    try {
      state.trySetFile(value)
      return@Validator ValidationResult()
    } catch (e: StorageMode.FileException) {
      when {
        "document.storage.error.read.notExists" == e.message && !isListEmpty.get() ->
          return@Validator ValidationResult.fromError(control, RootLocalizer.formatText(e.message!!, *e.args))
        else -> return@Validator ValidationResult.fromError(control, RootLocalizer.formatText(e.message!!, *e.args))
      }
    }
  }
}

private val i18n = RootLocalizer.createWithRootKey("storageService.local", BROWSE_PANE_LOCALIZER)
