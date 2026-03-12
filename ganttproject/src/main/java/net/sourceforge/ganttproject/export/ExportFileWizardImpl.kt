package net.sourceforge.ganttproject.export

import biz.ganttproject.core.option.BooleanOption
import biz.ganttproject.core.option.DefaultBooleanOption
import net.sourceforge.ganttproject.gui.projectwizard.WizardModel
import java.io.File

var ourLastSelectedExporter: Exporter? = null

class ExportWizardModel : WizardModel() {
  private var myExporter: Exporter? = null

  val publishInWebOption: BooleanOption = DefaultBooleanOption("exporter.publishInWeb")

  var exporter: Exporter?
    get() = myExporter
    set(exporter) {
      myExporter = exporter
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
