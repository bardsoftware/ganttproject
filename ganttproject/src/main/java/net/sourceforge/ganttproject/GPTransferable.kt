/*
Copyright 2018 BarD Software s.r.o

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
package net.sourceforge.ganttproject

import com.google.common.base.Charsets
import com.google.common.collect.ImmutableSet
import net.sourceforge.ganttproject.chart.gantt.ClipboardContents
import net.sourceforge.ganttproject.chart.gantt.ClipboardTaskProcessor
import net.sourceforge.ganttproject.chart.gantt.ExternalInternalFlavorMap
import net.sourceforge.ganttproject.io.GanttXMLSaver
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * @author dbarashev@bardsoftware.com
 */
class GPTransferable(private val myClipboardContents: ClipboardContents) : Transferable {
    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        if (INTERNAL_DATA_FLAVOR.equals(flavor)) {
            return myClipboardContents
        }
        if (EXTERNAL_TEXT_FLAVOR.equals(flavor)) {
            return createTextFlavor()
        }
        if (EXTERNAL_DOCUMENT_FLAVOR.equals(flavor)) {
            return createDocumentFlavor()?.let {
              ExternalInternalFlavorMap.put(it, myClipboardContents)
              ByteArrayInputStream(it)
            } ?: throw UnsupportedFlavorException(flavor)
        }
        throw UnsupportedFlavorException(flavor)
    }

    private fun createDocumentFlavor(): ByteArray? {
        val bufferProject: IGanttProject = GanttProjectImpl()
        val taskMgr = bufferProject.taskManager
        val processor = ClipboardTaskProcessor(taskMgr)
        // In intra-document copy+paste we do copy so-called external dependencies (those where one of the tasks is not in
        // the clipboard). However, we do not want to copy them into the system clipboard because in the target project
        // external task may not exist or may exist and be not the one we want.
        processor.setTruncateExternalDeps(true)
        // We also do not copy assignments into the system clipboard.
        processor.setTruncateAssignments(true)
        processor.pasteAsChild(taskMgr.rootTask, myClipboardContents)
        for (res in myClipboardContents.resources) {
            bufferProject.humanResourceManager.add(res.unpluggedClone())
        }
        try {
            ByteArrayOutputStream().use { out ->
                val saver = GanttXMLSaver(bufferProject)
                saver.save(out)
                out.flush()
                return out.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun createTextFlavor(): InputStream {
        val builder = StringBuilder()
        for (t in myClipboardContents.tasks) {
            builder.append(t.name).append(System.getProperty("line.separator"))
        }
        return ByteArrayInputStream(builder.toString().toByteArray(Charsets.UTF_8))
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return ourFlavors.toTypedArray()
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return ourFlavors.contains(flavor)
    }

    companion object {
        val mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + ClipboardContents::class.java.name + "\""
        @JvmField
        val EXTERNAL_DOCUMENT_FLAVOR = DataFlavor("application/x-ganttproject", "GanttProject XML File")
        val EXTERNAL_TEXT_FLAVOR = DataFlavor("text/plain", "GanttProject Task List")
        @JvmField
        var INTERNAL_DATA_FLAVOR = DataFlavor(mimeType)
        private var ourFlavors = ImmutableSet.of(INTERNAL_DATA_FLAVOR, EXTERNAL_DOCUMENT_FLAVOR, EXTERNAL_TEXT_FLAVOR)
    }
}