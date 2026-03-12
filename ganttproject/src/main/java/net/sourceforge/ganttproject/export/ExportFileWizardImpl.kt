/*
 * Copyright (c) 2003-2026 Dmitry Barashev, BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
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
package net.sourceforge.ganttproject.export

import biz.ganttproject.core.option.BooleanOption
import biz.ganttproject.core.option.DefaultBooleanOption
import biz.ganttproject.app.WizardModel
import java.io.File

/**
 * The model of the export wizard.
 */
class ExportWizardModel(id: String, title: String) : WizardModel(id, title) {
  val publishInWebOption: BooleanOption = DefaultBooleanOption("exporter.publishInWeb")

  var exporter: Exporter? = null
    set(exporter) {
      field = exporter
      ourLastSelectedExporter = exporter
    }

  var file: File? = null
    set(value) {
      field = value
      needsRefresh.set(true, this)
    }

  init {
    canFinish = {
      exporter != null && file != null && errorMessage.value.isNullOrBlank()
    }
  }
}

// The last exporter that was selected by the user. Used to recover the first page state when the wizard is reopened.
var ourLastSelectedExporter: Exporter? = null