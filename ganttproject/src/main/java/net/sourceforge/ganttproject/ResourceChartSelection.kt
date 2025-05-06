/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

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
package net.sourceforge.ganttproject

import net.sourceforge.ganttproject.AbstractChartImplementation.ChartSelectionImpl
import net.sourceforge.ganttproject.chart.gantt.ClipboardContents
import net.sourceforge.ganttproject.resource.ResourceSelectionManager
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.Transferable

internal class ResourceChartSelection(
  private val project: IGanttProject,
  private val selectionManager: ResourceSelectionManager
) : ChartSelectionImpl(),
    ClipboardOwner {
    @JvmField
    var myClipboardContents: ClipboardContents? = null

    override fun isEmpty(): Boolean {
        return selectionManager.resources.isEmpty()
    }

    override fun startCopyClipboardTransaction() {
        super.startCopyClipboardTransaction()
        myClipboardContents = ClipboardContents(project.taskManager).also { clipboard ->
          selectionManager.resources.forEach {
            clipboard.addResource(it)
          }
        }
        exportIntoSystemClipboard()
    }

    override fun startMoveClipboardTransaction() {
        super.startMoveClipboardTransaction()
        myClipboardContents = ClipboardContents(project.taskManager).also { clipboard ->
          selectionManager.resources.forEach {
            clipboard.addResource(it)
          }
        }
        exportIntoSystemClipboard()
    }

    private fun exportIntoSystemClipboard() {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(GPTransferable(myClipboardContents!!), this)
    }

    override fun cancelClipboardTransaction() {
        super.cancelClipboardTransaction()
        myClipboardContents = null
    }

    override fun commitClipboardTransaction() {
        myClipboardContents?.let { clipboard ->
          clipboard.resources.forEach {
            if (clipboard.isCut) {
              project.humanResourceManager.remove(it)
              project.humanResourceManager.add(it)
            } else {
              project.humanResourceManager.add(it.unpluggedClone())
            }
          }
        }
        super.commitClipboardTransaction()
        myClipboardContents = null
    }

    override fun lostOwnership(clipboard: Clipboard, transferable: Transferable) {
        // Do nothing.
    }
}
