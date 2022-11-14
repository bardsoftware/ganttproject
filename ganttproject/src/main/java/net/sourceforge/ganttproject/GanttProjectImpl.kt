/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2021 Dmitry Barashev, GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject

import biz.ganttproject.app.SimpleBarrier
import biz.ganttproject.app.TimerBarrier
import biz.ganttproject.app.TwoPhaseBarrierImpl
import biz.ganttproject.core.calendar.GPCalendarCalc
import biz.ganttproject.core.calendar.ImportCalendarOption
import biz.ganttproject.core.calendar.WeekendCalendarImpl
import biz.ganttproject.core.option.BooleanOption
import biz.ganttproject.core.option.ColorOption
import biz.ganttproject.core.option.DefaultBooleanOption
import biz.ganttproject.core.option.DefaultColorOption
import biz.ganttproject.core.time.TimeUnitStack
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import biz.ganttproject.customproperty.CustomPropertyManager
import biz.ganttproject.ganttview.TaskFilterManager
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.gui.NotificationManager
import net.sourceforge.ganttproject.gui.UIConfiguration
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter
import net.sourceforge.ganttproject.importer.BufferProject
import net.sourceforge.ganttproject.importer.TaskMapping
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.resource.HumanResourceMerger
import net.sourceforge.ganttproject.resource.OverwritingMerger
import net.sourceforge.ganttproject.roles.RoleManager
import net.sourceforge.ganttproject.storage.LazyProjectDatabaseProxy
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.task.*
import net.sourceforge.ganttproject.task.event.createTaskListenerWithTimerBarrier
import java.awt.Color
import java.io.IOException
import java.net.URL
import javax.swing.SwingUtilities

fun interface ErrorUi {
  fun show(ex: Exception)
}

open class GanttProjectImpl(taskManager: TaskManagerImpl? = null,
                            override val projectDatabase: ProjectDatabase = LazyProjectDatabaseProxy(
                              {error("Not supposed to be called")},
                              {error("Not supposed to be called")},
                            ))
  : IGanttProject {
  val listeners: MutableList<ProjectEventListener> = mutableListOf()
  override val baselines: MutableList<GanttPreviousState> = ArrayList()

  override var projectName: String = ""
  override var description: String = ""
  override var organization: String = ""
  override var webLink: String = ""

  val language: GanttLanguage get() = GanttLanguage.getInstance()
  private val myCalendar = WeekendCalendarImpl()
  final override val humanResourceManager = HumanResourceManager(
    RoleManager.Access.getInstance().defaultRole,
    CustomColumnsManager()
  )
  override val resourceCustomPropertyManager: CustomPropertyManager get() = humanResourceManager.customPropertyManager
  private val myTaskManagerConfig = TaskManagerConfigImpl(humanResourceManager, myCalendar)
  final override val taskManager: TaskManagerImpl = taskManager ?: TaskManagerImpl(null, myTaskManagerConfig)
  override val uIConfiguration = UIConfiguration(Color.BLUE, true)
  override val taskCustomColumnManager: CustomPropertyManager get() = taskManager.customPropertyManager
  override val taskFilterManager = TaskFilterManager(this.taskManager)
  override val roleManager: RoleManager
    get() = RoleManager.Access.getInstance()

  override var isModified: Boolean = false
  override val activeCalendar: GPCalendarCalc get() = myTaskManagerConfig.calendar
  override val timeUnitStack: TimeUnitStack get() = myTaskManagerConfig.timeUnitStack
  override var document: Document get() { TODO() } set(_) {
    TODO()
  }
  override val documentManager: DocumentManager
    get() = TODO("Not yet implemented")

  init {
    myCalendar.addListener { setModified() }
  }

  override fun setModified() {
    isModified = true
  }

  override fun close() {
    // TODO Auto-generated method stub
  }

  override fun addProjectEventListener(listener: ProjectEventListener) {
    listeners.add(listener)
  }

  override fun removeProjectEventListener(listener: ProjectEventListener) {
    listeners.remove(listener)
  }

  fun fireProjectModified(isModified: Boolean, errorUi: ErrorUi) {
    for (modifiedStateChangeListener in listeners) {
      try {
        if (isModified) {
          modifiedStateChangeListener.projectModified()
        } else {
          modifiedStateChangeListener.projectSaved()
        }
      } catch (e: Exception) {
        errorUi.show(e)
      }
    }
  }

  protected open fun fireProjectCreated() {
    for (modifiedStateChangeListener in listeners) {
      modifiedStateChangeListener.projectCreated()
    }
    // A new project just got created, so it is not yet modified
    SwingUtilities.invokeLater { isModified = false }
  }

  protected open fun fireProjectClosed() {
    for (modifiedStateChangeListener in listeners) {
      modifiedStateChangeListener.projectClosed()
    }
  }

  protected open fun fireProjectOpened() {
    val barrier = TwoPhaseBarrierImpl<IGanttProject>(this)
    for (l in listeners) {
      l.projectOpened(barrier, barrier)
    }
  }

  @Throws(Document.DocumentException::class, IOException::class)
  override fun restore(fromDocument: Document) {
    restoreProject(fromDocument, this.listeners)
  }

  @Throws(IOException::class)
  override fun open(document: Document) {
    // TODO Auto-generated method stub
  }

  override fun importProject(
    bufferProject: BufferProject,
    mergeOption: HumanResourceMerger.MergeResourcesOption,
    importCalendarOption: ImportCalendarOption?,
    closeCurrentProject: Boolean
  ): TaskMapping {
      roleManager.importData(bufferProject.roleManager)
      if (importCalendarOption != null) {
        activeCalendar.importCalendar(bufferProject.activeCalendar, importCalendarOption)
      }
      val that2thisResourceCustomDefs =
        resourceCustomPropertyManager.importData(bufferProject.resourceCustomPropertyManager)
      val original2ImportedResource = humanResourceManager.importData(
        bufferProject.humanResourceManager, OverwritingMerger(mergeOption), that2thisResourceCustomDefs
      )
      val that2thisCustomDefs = taskCustomColumnManager.importData(bufferProject.taskCustomColumnManager)
      val origTaskManager = taskManager
      try {
        origTaskManager.setEventsEnabled(false)
        val result = origTaskManager.importData(bufferProject.taskManager, that2thisCustomDefs)
        origTaskManager.importAssignments(
          bufferProject.taskManager, humanResourceManager,
          result, original2ImportedResource
        )
        return result
      } finally {
        origTaskManager.setEventsEnabled(true)
      }
  }
}

private val DEFAULT_TASK_COLOR = Color(140, 182, 206)

private class TaskManagerConfigImpl(
  private val myResourceManager: HumanResourceManager,
  calendar: GPCalendarCalc
) : TaskManagerConfig {
  private val myTimeUnitStack: GPTimeUnitStack
  private val myCalendar: GPCalendarCalc
  private val myDefaultTaskColorOption: ColorOption
  private val mySchedulerDisabledOption: BooleanOption
  override fun getDefaultColor(): Color {
    return myDefaultTaskColorOption.value!!
  }

  override fun getDefaultColorOption(): ColorOption {
    return myDefaultTaskColorOption
  }

  override fun getSchedulerDisabledOption(): BooleanOption {
    return mySchedulerDisabledOption
  }

  override fun getCalendar(): GPCalendarCalc {
    return myCalendar
  }

  override fun getTimeUnitStack(): TimeUnitStack {
    return myTimeUnitStack
  }

  override fun getResourceManager(): HumanResourceManager {
    return myResourceManager
  }

  override fun getProjectDocumentURL(): URL {
    TODO()
  }

  override fun getNotificationManager(): NotificationManager {
    TODO()
  }

  init {
    myTimeUnitStack = GPTimeUnitStack()
    myCalendar = calendar
    myDefaultTaskColorOption = DefaultTaskColorOption(DEFAULT_TASK_COLOR)
    mySchedulerDisabledOption = DefaultBooleanOption("scheduler.disabled", false)
  }
}

internal class DefaultTaskColorOption internal constructor(defaultColor: Color) :
  DefaultColorOption("taskDefaultColor", defaultColor), GP1XOptionConverter {
  constructor() : this(DEFAULT_TASK_COLOR)

  override fun getTagName(): String {
    return "colors"
  }

  override fun getAttributeName(): String {
    return "tasks"
  }

  override fun loadValue(legacyValue: String) {
    loadPersistentValue(legacyValue)
    commit()
  }
}

internal fun (IGanttProject).restoreProject(fromDocument: Document, listeners: List<ProjectEventListener>) {
  restoreProject(listeners) {
    fromDocument.read()
  }
}

internal fun <T> (IGanttProject).restoreProject(listeners: List<ProjectEventListener>, closeCurrentProject: Boolean = true, restoreCode: ()->T): T {
  val completionPromise = SimpleBarrier<Document>()
  listeners.forEach { it.projectRestoring(completionPromise) }
  val projectDocument = document
  if (closeCurrentProject) {
    close()
  }
  val algs = taskManager.algorithmCollection
  return try {
    algs.scheduler.isEnabled = false
    algs.recalculateTaskScheduleAlgorithm.isEnabled = false
    algs.adjustTaskBoundsAlgorithm.isEnabled = false
    restoreCode()
  } finally {
    algs.recalculateTaskScheduleAlgorithm.isEnabled = true
    algs.adjustTaskBoundsAlgorithm.isEnabled = true
    algs.scheduler.isEnabled = true
    completionPromise.resolve(projectDocument)
    document = projectDocument
  }
}


internal fun createProjectModificationListener(project: IGanttProject, uiFacade: UIFacade) =
  createTaskListenerWithTimerBarrier(timerBarrier = TimerBarrier(1000).apply {
    await { project.setModified() }
  }).also {
      it.taskAddedHandler = {
        project.setModified()
        uiFacade.viewIndex = UIFacade.GANTT_INDEX
        uiFacade.refresh()
      }
    }
