/*
Copyright 2018-2026 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.lib.fx

import javafx.scene.image.Image
import javafx.stage.Stage
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.GanttProject
import java.awt.Desktop
import java.awt.Taskbar
import java.awt.Toolkit
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import javax.swing.SwingUtilities

/**
 * @author dbarashev@bardsoftware.com
 */
fun openInBrowser(url: String) {
  SwingUtilities.invokeLater {
    try {
      Desktop.getDesktop().browse(URI(url))
    } catch (e: IOException) {
      GPLogger.log(e)
    } catch (e: URISyntaxException) {
      GPLogger.log(e)
    }
  }
}

fun openFile(file: File) {
  SwingUtilities.invokeLater {
    try {
      Desktop.getDesktop().open(file)
    } catch (e: IOException) {}
  }
}

fun isBrowseSupported(): Boolean = try {
  Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
} catch (e: Exception) {
  LOGGER.error("Browse not supported.", exception = e)
  false
}

fun Stage.installDockIcon() {
  val iconStream = { GanttProject::class.java.getResource("/icons/ganttproject-logo-512.png") }
  if (Taskbar.isTaskbarSupported()) {
    val taskbar = Taskbar.getTaskbar();

    if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
      val defaultToolkit = Toolkit.getDefaultToolkit();
      var dockIcon = defaultToolkit.getImage(iconStream());
      taskbar.setIconImage(dockIcon);
    }
  }
  val iconImage = Image(iconStream()!!.openStream())
  this.icons.add(iconImage)
}

private val LOGGER = GPLogger.create("Desktop")