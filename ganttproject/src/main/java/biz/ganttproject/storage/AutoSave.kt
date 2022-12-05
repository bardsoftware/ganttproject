/*
Copyright 2022 BarD Software s.r.o.

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
package biz.ganttproject.storage

import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import java.io.File
import java.io.IOException
import java.util.*

class AutoSaveManager(private val documentManager: DocumentManager) {
  fun newAutoSaveDocument(): Document =
    cacheDir?.let { File.createTempFile("_ganttproject_autosave", ".gan", it) }?.let {
      documentManager.getDocument(it.absolutePath)
    } ?: throw IllegalStateException("Can't create auto-save document because temporary directory was not found")

  @Throws(IOException::class)
  fun getLastAutoSaveDocument(priorTo: Document?): Document? {
    val allAutoSaves = cacheDir
      ?.listFiles { file -> file.name.startsWith("_ganttproject_autosave") }
      ?.sortedBy { it.lastModified() }?.reversed()
      ?: return null
    if (allAutoSaves.isEmpty()) {
      return null
    }
    if (priorTo == null) {
      return documentManager.getDocument(allAutoSaves.first().absolutePath)
    }
    val idxPriorTo = allAutoSaves.indexOfFirst { it.name == priorTo.fileName }
    return if (idxPriorTo == - 1 || idxPriorTo == allAutoSaves.size - 1) {
      null
    } else documentManager.getDocument(allAutoSaves[idxPriorTo+1].absolutePath)
  }
}

fun createAutosaveCleanup() = Runnable {
  cacheDir
    ?.listFiles { file -> file.name.startsWith("_ganttproject_autosave") }
    ?.sortedBy { it.lastModified() }
    ?.reversed()?.drop(100)?.forEach {
      if (!it.delete()) {
        LOG.debug("Can't delete autosave file {}", it)
      }
    }
}


private val LOG = GPLogger.create("Document")