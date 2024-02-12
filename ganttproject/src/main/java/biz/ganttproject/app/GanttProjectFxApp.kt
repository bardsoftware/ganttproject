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
package biz.ganttproject.app

import biz.ganttproject.lib.fx.vbox
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import net.sourceforge.ganttproject.GanttProject

class GanttProjectFxApp(private val ganttProject: GanttProject) : Application() {
  override fun start(stage: Stage) {
    try {
      val vbox = vbox {
        add(convertMenu(ganttProject.menuBar))
        add(ganttProject.createToolbar().build().toolbar)
        add(ganttProject.viewManager.fxComponent)
      }
      stage.setScene(Scene(vbox))
      stage.show()
    } catch (ex: Exception) {
      ex.printStackTrace()
    }
  }
}
