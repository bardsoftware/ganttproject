/*
Copyright 2017 Dmitry Barashev, BarD Software s.r.o

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
package net.sourceforge.ganttproject.io

import biz.ganttproject.core.GanttProjectProtos
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.impex.ExportProtos
import com.google.protobuf.Message
import com.google.protobuf.TextFormat
import com.google.protobuf.util.JsonFormat
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import org.w3c.util.DateParser

/**
 * This class converts IGanttProject to protocol buffers suitable for serializing as text, json or in binary format.
 *
 * @author Dmitry Barashev
 */
open class ModelSerializer(val project: IGanttProject) {
    fun encodeDuration(duration: TimeDuration) = "${duration.length}${project.timeUnitStack.encode(duration.timeUnit)}"


    fun writeChildTasks(rootTask: Task, protoTask: GanttProjectProtos.Task.Builder) {
        for (task in project.taskManager.taskHierarchy.getNestedTasks(rootTask)) {

            val taskProtoBuilder = GanttProjectProtos.Task.newBuilder().apply {
                id = task.taskID
                name = task.name
                schedule = GanttProjectProtos.Task.Activity.newBuilder()
                        .setBeginDate(task.start.toXMLString())
                        .setDuration(encodeDuration(task.duration))
                        .build()
                task.third?.let { earliestBegin = task.third.toXMLString() }
                completion = task.completionPercentage.toFloat()
                priority = GanttProjectProtos.Task.Priority.valueOf(task.priority.name)
                milestone = task.isMilestone
                subproject = task.isProjectTask
                cost =
                        if (task.cost.isCalculated) {
                            GanttProjectProtos.Task.Cost.newBuilder().setCalculated(true).build()
                        } else {
                            GanttProjectProtos.Task.Cost.newBuilder()
                                    .setCalculated(false).setValue(task.cost.manualValue.toDouble()).build()
                        }
            }
            writeChildTasks(task, taskProtoBuilder)
            protoTask.addChildTask(taskProtoBuilder.build())
        }
    }

    fun writeWorker(hr: HumanResource): GanttProjectProtos.Worker {
        val builder = GanttProjectProtos.Worker.newBuilder().apply {
            id = hr.id
            name = hr.name
            email = hr.mail
            phone = hr.phone
        }
        return builder.build()
    }

    fun writeWorkers(): List<GanttProjectProtos.Worker> = project.humanResourceManager.resources.map { writeWorker(it) }.toList()
}

/**
 * This class converts IGanttProject to ExportProtos.Project protocol buffer suitable for serializing to JSON
 * for export purposes.
 */
class ExportSerializer(project: IGanttProject) : ModelSerializer(project) {
    fun write(): ExportProtos.Project {
        val projectProtoBuilder = ExportProtos.Project.newBuilder()
        val rootTaskProtoBuilder = GanttProjectProtos.Task.newBuilder().apply {
            id = 0
            name = project.projectName
            schedule = GanttProjectProtos.Task.Activity.newBuilder()
                    .setBeginDate(DateParser.getIsoDateNoHours(project.taskManager.projectStart))
                    .setDuration(encodeDuration(project.taskManager.projectLength))
                    .build()
            completion = project.taskManager.projectCompletion.toFloat()
        }
        writeChildTasks(project.taskManager.rootTask, rootTaskProtoBuilder)
        projectProtoBuilder.addRootTask(rootTaskProtoBuilder.build())

        projectProtoBuilder.putAllWorker(writeWorkers().map { it.id to it }.toMap())
        return projectProtoBuilder.build()
    }
}

fun asJson(msg: Message) = JsonFormat.printer().print(msg)
fun asText(msg: Message) = TextFormat.printToString(msg)
