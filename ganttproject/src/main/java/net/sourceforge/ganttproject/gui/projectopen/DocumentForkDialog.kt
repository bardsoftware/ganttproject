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
package net.sourceforge.ganttproject.gui.projectopen

import biz.ganttproject.app.OptionElementData
import biz.ganttproject.app.OptionPaneBuilder
import biz.ganttproject.app.RootLocalizer
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView

enum class OpenOnlineDocumentChoice { USE_OFFLINE, USE_ONLINE, CANCEL }

fun showOfflineIsAheadDialog(handleChoice: (OpenOnlineDocumentChoice) -> Unit) {
  OptionPaneBuilder<OpenOnlineDocumentChoice>().run {
    i18n = RootLocalizer.createWithRootKey(rootKey = "cloud.openWhenOfflineIsAhead")
    styleClass = "dlg-lock"
    styleSheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
    styleSheets.add("/biz/ganttproject/storage/StorageDialog.css")
    graphic = FontAwesomeIconView(FontAwesomeIcon.UNLOCK)
    elements = listOf(
      OptionElementData("useOffline", OpenOnlineDocumentChoice.USE_OFFLINE, true),
      OptionElementData("useOnline", OpenOnlineDocumentChoice.USE_ONLINE),
      OptionElementData("cancel", OpenOnlineDocumentChoice.CANCEL)
    )

    showDialog(handleChoice)
  }
}

fun showForkDialog(handleChoice: (OpenOnlineDocumentChoice) -> Unit) {
  OptionPaneBuilder<OpenOnlineDocumentChoice>().run {
    i18n = RootLocalizer.createWithRootKey(rootKey = "cloud.openWhenDiverged")
    styleClass = "dlg-lock"
    styleSheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
    styleSheets.add("/biz/ganttproject/storage/StorageDialog.css")
    graphic = FontAwesomeIconView(FontAwesomeIcon.UNLOCK)
    elements = listOf(
      OptionElementData("useOffline", OpenOnlineDocumentChoice.USE_OFFLINE, true),
      OptionElementData("useOnline", OpenOnlineDocumentChoice.USE_ONLINE),
      OptionElementData("cancel", OpenOnlineDocumentChoice.CANCEL)
    )

    showDialog(handleChoice)
  }
}
