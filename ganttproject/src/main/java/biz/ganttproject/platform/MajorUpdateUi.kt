/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
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
package biz.ganttproject.platform

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.lib.fx.vbox
import com.bardsoftware.eclipsito.update.UpdateMetadata
import com.sandec.mdfx.MarkdownView
import javafx.scene.control.Label

/**
 * User interface for the major update record.
 */
internal class MajorUpdateUi(update: UpdateMetadata) {
  val title = Label(ourLocalizer.formatText("majorUpdate.title", update.version)).apply {
    styleClass.add("title")
  }
  val subtitle = Label(ourLocalizer.formatText("majorUpdate.subtitle", update.version)).apply {
    styleClass.setAll("subtitle")
  }
  val text: MarkdownView = MarkdownView().also { l ->
    l.stylesheets.add("/biz/ganttproject/app/mdfx.css")
    l.styleClass.add("par")
    l.mdString = ourLocalizer.formatText("majorUpdate.description", update.description)
  }

  val component = vbox {
    addClasses("major-update")
    addStylesheets("/biz/ganttproject/platform/Update.css")
    add(title)
    add(subtitle)
    add(text)
  }
}
private val ourLocalizer = RootLocalizer.createWithRootKey("platform.update", baseLocalizer = RootLocalizer)
