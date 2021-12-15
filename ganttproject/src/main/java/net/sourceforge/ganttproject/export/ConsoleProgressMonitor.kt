/*
Copyright 2021 BarD Software s.r.o

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
package net.sourceforge.ganttproject.export

import net.sourceforge.ganttproject.GPLogger
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.core.runtime.jobs.ProgressProvider

/**
 * @author dbarashev@bardsoftware.com
 */
private class ConsoleProgressMonitor : IProgressMonitor {
  private var myTaskName: String? = null
  private var myTotalWork = 0
  private var isCanceled = false
  private var myWorked = 0
  override fun beginTask(name: String?, totalWork: Int) {
    myTaskName = name
    myTotalWork = totalWork
  }

  override fun done() {
    LOGGER.debug("[$myTaskName] done")
  }

  override fun internalWorked(work: Double) {}
  override fun isCanceled(): Boolean {
    return isCanceled
  }

  override fun setCanceled(value: Boolean) {
    isCanceled = value
    LOGGER.debug("[$myTaskName] canceled")
  }

  override fun setTaskName(name: String?) {}
  override fun subTask(name: String?) {}
  override fun worked(work: Int) {
    myWorked += work
    LOGGER.debug("[" + myTaskName + "] " + myWorked * 100 / myTotalWork + "%")
  }
}

private val LOGGER = GPLogger.create("Export.Progress")

internal class ConsoleProgressProvider : ProgressProvider() {
  override fun createMonitor(p0: Job?): IProgressMonitor {
    TODO("Not yet implemented")
  }

  override fun createMonitor(p0: Job?, p1: IProgressMonitor?, p2: Int): IProgressMonitor {
    TODO("Not yet implemented")
  }

  override fun createProgressGroup(): IProgressMonitor = ConsoleProgressMonitor()

  override fun getDefaultMonitor(): IProgressMonitor = ConsoleProgressMonitor()

}
