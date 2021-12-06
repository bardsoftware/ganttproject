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

import biz.ganttproject.core.calendar.GPCalendarCalc
import biz.ganttproject.core.calendar.WeekendCalendarImpl
import biz.ganttproject.core.option.BooleanOption
import biz.ganttproject.core.option.ColorOption
import biz.ganttproject.core.option.DefaultBooleanOption
import biz.ganttproject.core.option.DefaultColorOption
import biz.ganttproject.core.time.TimeUnitStack
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import biz.ganttproject.ganttview.TaskFilterManager
import com.google.common.base.Strings
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.gui.NotificationManager
import net.sourceforge.ganttproject.gui.UIConfiguration
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.roles.RoleManager
import net.sourceforge.ganttproject.task.CustomColumnsManager
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskManagerConfig
import java.awt.Color
import java.io.IOException
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities

fun interface ErrorUi {
  fun show(ex: Exception)
}

open class GanttProjectImpl() : IGanttProject {
  private var myProjectName: String? = null
  private var myDescription: String? = null
  private var myOrganization: String? = null
  private var myWebLink: String? = null
  private val myTaskManager: TaskManager
  private val myResourceManager: HumanResourceManager
  private val myTaskManagerConfig: TaskManagerConfigImpl
  private var myDocument: Document? = null
  val listeners: MutableList<ProjectEventListener> = mutableListOf()
  private val myUIConfiguration: UIConfiguration
  private val myTaskCustomColumnManager: CustomColumnsManager
  private val myBaselines: List<GanttPreviousState> = ArrayList()
  private val myCalendar = WeekendCalendarImpl()
  private val taskFilterManager: TaskFilterManager

  override fun getProjectName(): String {
    return myProjectName!!
  }

  override fun setProjectName(projectName: String) {
    myProjectName = projectName
  }

  override fun getDescription(): String {
    return Strings.nullToEmpty(myDescription)
  }

  override fun setDescription(description: String) {
    myDescription = description
  }

  override fun getOrganization(): String {
    return myOrganization!!
  }

  override fun setOrganization(organization: String) {
    myOrganization = organization
  }

  override fun getWebLink(): String {
    return myWebLink!!
  }

  override fun setWebLink(webLink: String) {
    myWebLink = webLink
  }

  fun newTask(): Task {
    val result: Task = taskManager.createTask()
    taskManager.taskHierarchy.move(result, taskManager.rootTask)
    return result
  }

  val language: GanttLanguage
    get() = Companion.language

  override fun getUIConfiguration(): UIConfiguration {
    return myUIConfiguration
  }

  override fun getHumanResourceManager(): HumanResourceManager {
    return myResourceManager
  }

  override fun getRoleManager(): RoleManager {
    return RoleManager.Access.getInstance()
  }

  override fun getTaskManager(): TaskManager {
    return myTaskManager
  }

  override fun getActiveCalendar(): GPCalendarCalc {
    return myTaskManagerConfig.calendar
  }

  override fun getTimeUnitStack(): TimeUnitStack {
    return myTaskManagerConfig.timeUnitStack
  }

  override fun setModified() {
    // TODO Auto-generated method stub
  }

  override fun setModified(modified: Boolean) {
    // TODO Auto-generated method stub
  }

  override fun close() {
    // TODO Auto-generated method stub
  }

  override fun getDocument(): Document {
    return myDocument!!
  }

  override fun setDocument(document: Document) {
    myDocument = document
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
    val barrier = CountDownCompletionPromise<IGanttProject>(this)
    for (l in listeners) {
      l.projectOpened(barrier, barrier)
    }
  }

  override fun isModified(): Boolean {
    // TODO Auto-generated method stub
    return false
  }

  @Throws(Document.DocumentException::class, IOException::class)
  override fun restore(fromDocument: Document) {
  }

  @Throws(IOException::class)
  override fun open(document: Document) {
    // TODO Auto-generated method stub
  }

  override fun getDocumentManager(): DocumentManager {
    // TODO Auto-generated method stub
    TODO()
  }

  override fun getResourceCustomPropertyManager(): CustomPropertyManager {
    return myResourceManager.customPropertyManager
  }

  private class TaskManagerConfigImpl internal constructor(
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

  override fun getTaskCustomColumnManager(): CustomPropertyManager {
    return myTaskCustomColumnManager
  }

  override fun getBaselines(): List<GanttPreviousState> {
    return myBaselines
  }

  override fun getTaskFilterManager() = taskFilterManager

  fun repaintResourcePanel() {
    // TODO Auto-generated method stub
  }

  internal class DefaultTaskColorOption internal constructor(defaultColor: Color) :
    DefaultColorOption("taskDefaultColor", defaultColor), GP1XOptionConverter {
    constructor() : this(DEFAULT_TASK_COLOR) {}

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

  companion object {
    private val language = GanttLanguage.getInstance()
    val DEFAULT_TASK_COLOR = Color(140, 182, 206)
  }

  init {
    myResourceManager = HumanResourceManager(
      RoleManager.Access.getInstance().defaultRole,
      CustomColumnsManager()
    )
    myTaskManagerConfig = TaskManagerConfigImpl(myResourceManager, myCalendar)
    myTaskManager = TaskManager.Access.newInstance(null, myTaskManagerConfig)
    myUIConfiguration = UIConfiguration(Color.BLUE, true)
    myTaskCustomColumnManager = CustomColumnsManager()
    myCalendar.addListener { setModified() }
    taskFilterManager = TaskFilterManager(myTaskManager)
  }
}

internal fun (IGanttProject).restoreProject(fromDocument: Document, listeners: List<ProjectEventListener>) {
  val completionPromise = CompletionPromiseImpl<Document>()
  listeners.forEach { it.projectRestoring(completionPromise) }
  val projectDocument = document
  close()
  val algs = taskManager.algorithmCollection
  try {
    algs.scheduler.isEnabled = false
    algs.recalculateTaskScheduleAlgorithm.setEnabled(false)
    algs.adjustTaskBoundsAlgorithm.setEnabled(false)
    fromDocument.read()
  } finally {
    algs.recalculateTaskScheduleAlgorithm.setEnabled(true)
    algs.adjustTaskBoundsAlgorithm.setEnabled(true)
    algs.scheduler.isEnabled = true
  }
  completionPromise.resolve(projectDocument)
  document = projectDocument
}

typealias Subscriber<T> = (T)->Unit
typealias ActivityTermination = ()->Unit

interface CompletionActivityRegistry {
  fun add(activity: String): ActivityTermination
}
fun interface CompletionPromise<T> {
  fun await(code: Subscriber<T>)
}

class CompletionPromiseImpl<T> : CompletionPromise<T> {
  private val subscribers = mutableListOf<Subscriber<T>>()
  override fun await(code: Subscriber<T>) { subscribers.add(code) }
  internal fun resolve(value: T) = subscribers.forEach { it(value) }
}

class CountDownCompletionPromise<T>(private val value: T) : CompletionPromise<T>, CompletionActivityRegistry {
  private val counter = AtomicInteger(0)
  private val subscribers = mutableListOf<Subscriber<T>>()
  private val activities = mutableMapOf<String, ActivityTermination>()
  override fun await(code: Subscriber<T>) {
    if (counter.get() == 0) {
      code(value)
    } else {
      subscribers.add(code)
    }
  }

  override fun add(activity: String): ActivityTermination {
    return {
      if (counter.get() > 0) {
        BARRIER_LOGGER.debug("Barrier reached: $activity")
        activities.remove(activity)
        tick()
      }
    }.also {
      BARRIER_LOGGER.debug("Barrier waiting: $activity")
      activities[activity] = it
      counter.incrementAndGet()
    }
  }
  private fun tick() {
    if (counter.decrementAndGet() == 0) {
      subscribers.forEach { it(value) }
    }
  }

  fun isResolved() = this.counter.get() == 0
}

private val BARRIER_LOGGER = GPLogger.create("App.Barrier")
