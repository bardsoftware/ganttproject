/*
Copyright 2026 Dmitry Barashev,  BarD Software s.r.o

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
package net.sourceforge.ganttproject.action.edit

import biz.ganttproject.app.OptionElementData
import biz.ganttproject.app.OptionPaneBuilder
import biz.ganttproject.app.RootLocalizer
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView

/**
 * Where to paste the tasks copied from another project.
 */
enum class PasteExternalChoice {
  /** Import the tasks into the top level of the task tree. */
  TOP_LEVEL,
  /** Paste the tasks after the selected task, as its siblings. */
  AFTER_SELECTED,
  CANCEL
}

/**
 * Asks the user where to paste the tasks copied from another project, and calls the handler with the user choice.
 * The handler is not called if the dialog is closed without choosing.
 */
fun showPasteExternalDialog(handleChoice: (PasteExternalChoice) -> Unit) {
  OptionPaneBuilder<PasteExternalChoice>().run {
    i18n = RootLocalizer.createWithRootKey(rootKey = "paste.external", baseLocalizer = RootLocalizer)
    styleClass = "dlg-lock"
    styleSheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
    styleSheets.add("/biz/ganttproject/storage/StorageDialog.css")
    graphic = FontAwesomeIconView(FontAwesomeIcon.PASTE, "64")
    elements = listOf(
      OptionElementData("afterSelected", PasteExternalChoice.AFTER_SELECTED, true),
      OptionElementData("topLevel", PasteExternalChoice.TOP_LEVEL),
      OptionElementData("cancel", PasteExternalChoice.CANCEL)
    )
    showDialog(handleChoice)
  }
}
