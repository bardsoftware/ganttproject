/*
Copyright 2005-2026 Dmitry Barashev, GanttProject Team, BarD Software s.r.o.

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
package net.sourceforge.ganttproject.export

import biz.ganttproject.app.JobMonitorModel
import biz.ganttproject.core.option.GPOptionGroup
import javafx.scene.Parent
import kotlinx.coroutines.CoroutineScope
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.gui.UIFacade
import org.osgi.service.prefs.Preferences
import java.awt.Component
import java.io.File

/**
 * @author bard
 */
interface Exporter {
    val fileTypeDescription: String

    val options: GPOptionGroup

    val secondaryOptions: MutableList<GPOptionGroup>?

    val fileNamePattern: String

    @Throws(Exception::class)
    fun run(coroutineScope: CoroutineScope, outputFile: File, finalizationJob: ExportFinalizationJob?, jobMonitor: JobMonitorModel)

    // File proposeOutputFile(IGanttProject project);
    fun proposeFileExtension(): String

    val fileExtensions: Array<String>

    fun withFormat(format: String): Exporter?

    //String[] getCommandLineKeys();
    fun setContext(project: IGanttProject, uiFacade: UIFacade, prefs: Preferences)

    val customOptionsUI: Component?

  fun createCustomOptionsUiFx(): Parent? { return null }
}
