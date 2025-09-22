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
package net.sourceforge.ganttproject.gui.taskproperties

import biz.ganttproject.app.*
import biz.ganttproject.colorFromUiManager
import biz.ganttproject.core.chart.render.Style
import biz.ganttproject.core.chart.render.TaskTexture
import biz.ganttproject.core.option.*
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.createButton
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.layout.*
import javafx.scene.shape.Rectangle
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.Task.Priority
import net.sourceforge.ganttproject.task.TaskMutator
import net.sourceforge.ganttproject.task.TaskView
import net.sourceforge.ganttproject.util.BrowserControl
import org.w3c.util.DateParser
import javax.swing.SwingUtilities

/**
 * This class is responsible for rendering the user interface components to display
 * and update properties such as the task name, milestone status, scheduling options,
 * priority, progress, colors, notes, web links, and task-specific configurations.
 */
class MainPropertiesPanel(private val task: Task, private val taskView: TaskView) {
  val title: String = RootLocalizer.formatText("general")
  val fxComponent by lazy { getFxNode() }
  val validationErrors = FXCollections.observableArrayList<String>()
  var defaultColor: ColorOption? = null

  private val nameOption = ObservableString("name", task.name)
  private val milestoneOption = ObservableBoolean("milestone", task.isMilestone)
  private val taskDatesController = TaskDatesController(task, milestoneOption)
  private val projectTaskOption = ObservableBoolean("projectTask", task.isProjectTask)
  private val hasEarliestStart = ObservableBoolean("hasEarliestStart", task.thirdDateConstraint == 1)
  private val earliestStartOption = ObservableDate("earliestBegin",
    if (task.thirdDateConstraint == 1 ) task.third.toLocalDate() else null,
    validator = { evt ->
      val result = DateValidators.aroundProjectStart(task.manager.projectStart).invoke(DateParser.toJavaDate(evt.newValue))
      if (result.first) {
        Ok(evt.newValue)
      } else {
        Err(result.second ?: "The value $result looks suspicious here")
      }
    }
  )
  private val priorityOption = ObservableEnum<Priority>("priority", task.priority, Priority.entries.toTypedArray())
  private val progressOption = ObservableInt("progress", task.completionPercentage)
  private val showInTimelineOption = ObservableBoolean("showInTimeline", taskView.timelineTasks.contains(task))
  private val colorOption = ObservableColor("color", Style.Color.parse(ColorOption.Util.getColor(task.color)))
  private val notesOption = ObservableString("notes", task.notes)
  private val webLinkOption = ObservableString("webLink", task.webLink)
  private val textureOption = ObservableEnum("texture", TaskTexture.find(task.shape) ?: TaskTexture.TRANSPARENT,
    TaskTexture.entries.toTypedArray()
  )
  private val copyStartDateAction = GPAction.create("option.taskProperties.main.earliestBegin.copyBeginDate") {
    earliestStartOption.set(taskDatesController.startDateOption.value)
  }.also {
    it.putValue(GPAction.TEXT_DISPLAY, ContentDisplay.TEXT_ONLY)
    it.isEnabled = earliestStartOption.isWritable.value
  }
  private val applyDefaultColorAction = GPAction.create("defaultColor") {
    defaultColor?.value?.let {
      colorOption.set(Style.Color.parse(ColorOption.Util.getColor(it)))
    }
  }.also {
    it.putValue(GPAction.TEXT_DISPLAY, ContentDisplay.TEXT_ONLY)
  }
  private var onRequestFocus = {}

  private fun onHasEarliestStartChange(hasEarliestStart: Boolean) {
    earliestStartOption.setWritable(hasEarliestStart)
    copyStartDateAction.isEnabled = hasEarliestStart
  }

  init {
    hasEarliestStart.addWatcher { event -> onHasEarliestStartChange(event.newValue) }

    onHasEarliestStartChange(hasEarliestStart.value)
  }

  private fun getFxNode() = StackPane().apply {
    background = Background(BackgroundFill("Panel.background".colorFromUiManager(), CornerRadii.EMPTY, Insets.EMPTY))
    val leftPane = PropertySheetBuilder(i18n).pane {
      stylesheet("/biz/ganttproject/task/TaskPropertiesDialog.css")
      title("section.main")
      text(nameOption)
      if (task.canBeProjectTask()) {
        checkbox(projectTaskOption)
      } else if (task.canBeMilestone()) {
        checkbox(milestoneOption)
      }

      skip()
      dropdown(taskDatesController.schedulingOptions)
      date(taskDatesController.startDateOption) {
        stringConverter = GanttLanguage.getInstance().shortDateConverter
      }
      date(taskDatesController.displayEndDateOption) {
        stringConverter = GanttLanguage.getInstance().shortDateConverter
      }
      numeric(taskDatesController.durationOption) {
        minValue = 1
      }

      skip()
      custom(earliestStartOption, 
        HBox().apply {
          alignment = Pos.CENTER
          spacing = 5.0
          val dateEditor = this@pane.createDateOptionEditor(earliestStartOption)
          children.add(this@pane.createBooleanOptionEditor(hasEarliestStart))
          children.add(dateEditor)
          children.add(createButton(copyStartDateAction, onlyIcon = false).also {
            it.styleClass.addAll("btn-regular", "small", "secondary")
          })
          disableProperty().subscribe { oldValue, newValue ->
            if (!oldValue && newValue) {
              isDisable = false
            }
          }
        }
      )
      dropdown(priorityOption)
      numeric(progressOption) {
        minValue = 0
        maxValue = 100
      }

      skip()
      title("section.view")
      checkbox(showInTimelineOption)
      custom(colorOption, HBox().apply {
        alignment = Pos.CENTER_LEFT
        spacing = 5.0
        children.add(createColorOptionEditor(colorOption).also { HBox.setHgrow(it, javafx.scene.layout.Priority.ALWAYS) })
        children.add(createButton(applyDefaultColorAction, onlyIcon = false).also {
          it.styleClass.addAll("btn-regular", "small", "secondary")
        })
      })
      dropdown(textureOption) {
        cellFactory = { _, p ->
          Rectangle(200.0, 20.0).also {
            it.fill = p.first.paint.asJavaFxPattern()
          }
        }
      }
    }
    onRequestFocus = leftPane::requestFocus

    val grid = GridPane()

    val rightPane = PropertySheetBuilder(i18n).pane {
      stylesheet("/biz/ganttproject/task/TaskPropertiesDialog.css")
      title("section.documents")
      text(notesOption) {
        isMultiline = true
        labelPosition = LabelPosition.ABOVE
      }
      text(webLinkOption) {
        editorStyles.add("weblink")
        rightNode = Button(i18n.formatTextOrNull("btn.open"), FontAwesomeIconView(FontAwesomeIcon.EXTERNAL_LINK)).also {
          it.contentDisplay = ContentDisplay.LEFT
          it.onAction = EventHandler { e ->
            webLinkOption.value?.let {
              if (it.isNotBlank()) {
                SwingUtilities.invokeLater {
                  BrowserControl.displayURL(webLinkOption.value)
                }
              }
            }
          }
        }
      }
    }
    leftPane.validationErrors.addListener(MapChangeListener {
      validationErrors.clear()
      validationErrors.addAll(leftPane.validationErrors.values)
    })
    grid.add(leftPane.node, 0, 0)
    grid.add(rightPane.node, 1, 0)
    children.add(grid)
  }

  fun save(taskMutator: TaskMutator) {
    nameOption.ifChanged(taskMutator::setName)
    milestoneOption.ifChanged(taskMutator::setMilestone)
    projectTaskOption.ifChanged(taskMutator::setProjectTask)
    taskDatesController.startDateOption.ifChanged { value ->
      taskMutator.setStart(GanttCalendar.fromLocalDate(value))
    }
    taskDatesController.endDateOption.ifChanged { value ->
      taskMutator.setEnd(GanttCalendar.fromLocalDate(value))
    }
    taskDatesController.durationOption.ifChanged { value ->
      taskMutator.setDuration(task.manager.createLength(value.toLong()))
    }
    earliestStartOption.ifChanged { value ->
      if (hasEarliestStart.value) {
        taskMutator.setThird(GanttCalendar.fromLocalDate(value), 1)
      } else {
        taskMutator.setThird(null, 0)
      }
    }

    priorityOption.ifChanged(taskMutator::setPriority)
    progressOption.ifChanged(taskMutator::setCompletionPercentage)
    colorOption.ifChanged { color ->
      color?.let { taskMutator.setColor(it.get()) }
    }
    showInTimelineOption.ifChanged { value ->
      if (value) {
        taskView.timelineTasks.add(task)
      } else {
        taskView.timelineTasks.remove(task)
      }
    }
    notesOption.ifChanged(taskMutator::setNotes)
    webLinkOption.ifChanged(taskMutator::setWebLink)
    textureOption.ifChanged { value ->
      taskMutator.setShape(value.paint)
    }
  }

  fun requestFocus() = onRequestFocus()
}

private fun Task.canBeMilestone() = this.nestedTasks.isEmpty()

private fun Task.canBeProjectTask(): Boolean {
  run {
    var parent = this.supertask
    while (parent != null) {
      if (parent.isProjectTask) {
        return false
      }
      parent = parent.supertask
    }
  }

  val nestedTasks = this.nestedTasks
  if (nestedTasks.isEmpty()) {
    return false
  }

  for (nestedTask in nestedTasks) {
    if (nestedTask.isProjectTaskOrContainsProjectTask()) {
      return false
    }
  }
  return true
}

private fun Task.isProjectTaskOrContainsProjectTask(): Boolean {
  if (this.isProjectTask) {
    return true
  }
  return this.nestedTasks.any { it.isProjectTaskOrContainsProjectTask() }
}


// In the new Properties dialog all labels are structured as 'option.taskProperties.main.<FIELD>.label",
// e.g. "option.taskProperties.main.progress.label". If we have translations for such keys, we're lucky,
// however, there are already translated strings for the previously used keys, e.g. for "advancement" that corresponds
// to "option.taskProperties.main.progress.label". We want to reuse them until we get the updated translations.
private val i18n = i18n {
  // We will search for the translation corresponding to a structured key in the current language only.
  default(withFallback = false)
  prefix("option.taskProperties.main") {
    // If there is no translation, we'll search for the translation corresponding to the previously used unstructured key,
    // again in the current language only.
    default(withFallback = false)
    transform { key ->
      val key1 = when {
        key.endsWith(".label") -> key.removeSuffix(".label")
        key.startsWith("priority.value.") -> "priority.${key.removePrefix("priority.value.")}"
        else -> key
      }
      val map = mapOf(
        "startDate" to "dateOfBegining",
        "endDate" to "dateOfEnd",
        "progress" to "advancement",
        "milestone" to "meetingPoint",
        "notes" to "notesTask",
        "color" to "colors",
        "texture" to "shape",
        "btn.open" to "storage.action.open",
      )
      map[key1] ?: key1
    }
    fallback {
      // Finally, we'll use the English translation of a structured key.
      default()
      prefix("option.taskProperties.main")
    }
  }
}