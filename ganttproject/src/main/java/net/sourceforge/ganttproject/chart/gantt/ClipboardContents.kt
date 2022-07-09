/*
Copyright 2014 BarD Software s.r.o

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
package net.sourceforge.ganttproject.chart.gantt

import com.google.common.base.Predicate
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.google.common.hash.Hashing
import com.google.common.io.ByteStreams
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.GPTransferable
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.importer.BufferProject
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.ResourceAssignment
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import net.sourceforge.ganttproject.util.collect.Pair
import java.awt.Toolkit
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.*

/**
 * Represents all objects which are involved into a clipboard transaction on Gantt chart: tasks, dependencies
 * and resource assignments. It is not really what is placed in the system clipboard, it is rather a grouping of
 * the model objects
 *
 * @author dbarashev (Dmitry Barashev)
 */
class ClipboardContents(val taskManager: TaskManager) {
    private val myResources: MutableList<HumanResource> = Lists.newArrayList()
    private val myTasks: MutableList<Task> = Lists.newArrayList()
    private val myIntraDeps: MutableList<TaskDependency> = Lists.newArrayList()
    private val myIncomingDeps: MutableList<TaskDependency> = Lists.newArrayList()
    private val myOutgoingDeps: MutableList<TaskDependency> = Lists.newArrayList()
    private val myAssignments: MutableList<ResourceAssignment> = Lists.newArrayList()
    private val myNestedTasks = LinkedHashMultimap.create<Task?, Task>()
    var isCut = false
        private set

    /**
     * Adds tasks to the clipboard contents
     * @param tasks
     */
    fun addTasks(tasks: List<Task>) {
        myTasks.addAll(tasks)
    }

    /**
     * Adds appropriate objects (dependencies and assignments) to the clipboard depending on the already placed tasks.
     */
    private fun build() {
        val taskHierarchy = taskManager.taskHierarchy
        val subtree: MutableSet<Task> = Sets.newHashSet()
        val predicate = Predicate { parent_child: Pair<Task?, Task>? ->
            subtree.add(parent_child!!.second())
            if (parent_child.first() != null) {
                myNestedTasks.put(parent_child.first(), parent_child.second())
            }
            true
        }
        Collections.sort(myTasks, IN_DOCUMENT_ORDER)
        for (t in myTasks) {
            taskHierarchy.breadthFirstSearch(t, predicate)
        }
        val intraDeps: MutableSet<TaskDependency> = Sets.newLinkedHashSet()
        for (dependency in taskManager.dependencyCollection.dependencies) {
            val dependant = dependency.dependant
            val dependee = dependency.dependee
            if (subtree.contains(dependant) && subtree.contains(dependee)) {
                intraDeps.add(dependency)
            }
        }
        for (t in subtree) {
            for (dep in t.dependenciesAsDependant.toArray()) {
                if (intraDeps.contains(dep)) {
                    continue
                }
                myIncomingDeps.add(dep)
            }
            for (dep in t.dependenciesAsDependee.toArray()) {
                if (intraDeps.contains(dep)) {
                    continue
                }
                myOutgoingDeps.add(dep)
            }
        }
        myIntraDeps.addAll(intraDeps)
        for (t in tasks) {
            myAssignments.addAll(Arrays.asList(*t.assignments))
        }
        for (ra in myAssignments) {
            myResources.add(ra.resource)
        }
        GPLogger.getLogger("Clipboard").fine(String.format(
                "Clipboard task (only roots): %s\ninternal-dependencies: %s\nincoming dependencies:%s\noutgoing dependencies:%s",
                myTasks, myIntraDeps, myIncomingDeps, myOutgoingDeps))
    }

    /**
     * @return all clipboard tasks
     */
    val tasks: List<Task>
        get() = myTasks

    /**
     * @return a list of dependencies where both successor and predecessor are in clipboard for any dep
     */
    val intraDeps: List<TaskDependency>
        get() = myIntraDeps

    /**
     * @return a list of dependencies where only successor is in clipboard for any dep
     */
    val incomingDeps: List<TaskDependency>
        get() = myIncomingDeps

    /**
     * @return a list of dependencies where only predecessor is in clipboard for any dep
     */
    val outgoingDeps: List<TaskDependency>
        get() = myOutgoingDeps
    val assignments: List<ResourceAssignment>
        get() = myAssignments

    /**
     * Processes objects placed into the clipboard so that it was "cut" transaction      myAssignments.addAll(Arrays.asList(t.getAssignments()));
     *
     */
    fun cut() {
        isCut = true
        build()
    }

    /**
     * Processes objects placed into the clipboard so that it was "copy" transaction
     */
    fun copy() {
        build()
        isCut = false
        // Nothing needs to be done, actually, in addition to what build() already does
    }

    fun getNestedTasks(task: Task?) = myNestedTasks[task]


    fun addResource(res: HumanResource) {
        myResources.add(res)
    }

    val resources: List<HumanResource>
        get() = myResources

    companion object {
        private val IN_DOCUMENT_ORDER: Comparator<in Task> = Comparator { left, right -> left.manager.taskHierarchy.compareDocumentOrder(left, right) }
    }
}

/**
 * This object maps clipboard contents with the external flavor (XML serialized as a byte array)
 * to internal (ClipboardContents instance). This
 */
object ExternalInternalFlavorMap {
  private val mapping = mutableMapOf<String, ClipboardContents>()
  fun put(externalFlavor: ByteArray, internalFlavor: ClipboardContents) {
    mapping.clear()
    val key = Hashing.murmur3_128().hashBytes(externalFlavor).toString()
    mapping[key] = internalFlavor
  }

  fun get(externalFlavor: ByteArray): ClipboardContents? = mapping[Hashing.murmur3_128().hashBytes(externalFlavor).toString()]

}

fun getProjectFromClipboard(bufferProject: BufferProject): BufferProject? {
  val clipboard = Toolkit.getDefaultToolkit().systemClipboard
  if (clipboard.isDataFlavorAvailable(GPTransferable.EXTERNAL_DOCUMENT_FLAVOR)) {
    try {
      val data = clipboard.getData(GPTransferable.EXTERNAL_DOCUMENT_FLAVOR)
      if (data !is InputStream) {
        return null
      }
      val bytes = ByteStreams.toByteArray(data)
      val tmpFile = File.createTempFile("ganttPaste", "")
      Files.write(tmpFile.toPath(), bytes)

      val document: Document = bufferProject.documentManager.getDocument(tmpFile.absolutePath)
      document.read()
      tmpFile.delete()

      return bufferProject

    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
  return null
}