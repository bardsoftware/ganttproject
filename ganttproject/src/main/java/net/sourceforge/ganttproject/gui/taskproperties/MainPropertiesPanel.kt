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

import biz.ganttproject.app.LabelPosition
import biz.ganttproject.app.PropertySheetBuilder
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.colorFromUiManager
import biz.ganttproject.core.chart.render.Style
import biz.ganttproject.core.chart.render.TaskTexture
import biz.ganttproject.core.option.*
import biz.ganttproject.core.time.GanttCalendar
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Hyperlink
import javafx.scene.layout.*
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.shape.Rectangle
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.Task.Priority
import net.sourceforge.ganttproject.task.TaskMutator
import net.sourceforge.ganttproject.task.TaskView

private enum class CalculatedPart {
  START_DATE, END_DATE, DURATION
}
class MainPropertiesPanel(private val task: Task, private val taskView: TaskView) {
  val title: String = RootLocalizer.formatText("general")
  private val nameOption = ObservableString("name", task.name)
  private val milestoneOption = ObservableBoolean("milestone", task.isMilestone)
  private val startDateOption = ObservableDate("startDate", task.start.toLocalDate())
  private val endDateOption = ObservableDate("endDate", task.end.toLocalDate())
  private val durationOption = ObservableInt("duration", task.duration.value.toInt())
  private val hasEarliestStart = ObservableBoolean("hasEarliestStart", task.thirdDateConstraint == 1)
  private val earliestStartOption = ObservableDate("earliestStart", if (task.thirdDateConstraint == 1 ) task.third.toLocalDate() else null)
  private val priorityOption = ObservableEnum<Priority>("priority", task.priority, Priority.entries.toTypedArray())
  private val progressOption = ObservableInt("progress", task.completionPercentage)
  private val showInTimelineOption = ObservableBoolean("showInTimeline", taskView.timelineTasks.contains(task))
  private val colorOption = ObservableColor("color", Style.Color.parse(ColorOption.Util.getColor(task.color)))
  private val notesOption = ObservableString("notes", task.notes)
  private val webLinkOption = ObservableString("webLink", task.webLink)
  private val textureOption = ObservableEnum("texture", TaskTexture.find(task.shape) ?: TaskTexture.TRANSPARENT, TaskTexture.values())
  private val schedulingOptions = ObservableEnum("schedulingOptions", CalculatedPart.END_DATE,
    CalculatedPart.entries.toTypedArray()
  )

  private fun onSchedulingOptionChange(calculatedPart: CalculatedPart) {
    when (calculatedPart) {
      CalculatedPart.START_DATE -> {
        startDateOption.setWritable(false)
        endDateOption.setWritable(true)
        durationOption.setWritable(true)
      }
      CalculatedPart.END_DATE -> {
        endDateOption.setWritable(false)
        startDateOption.setWritable(true)
        durationOption.setWritable(true)
      }
      CalculatedPart.DURATION -> {
        durationOption.setWritable(false)
        startDateOption.setWritable(true)
        endDateOption.setWritable(true)
      }
    }
  }

  private fun onHasEarliestStartChange(hasEarliestStart: Boolean) = earliestStartOption.setWritable(hasEarliestStart)

  init {
    schedulingOptions.addWatcher { event -> onSchedulingOptionChange(event.newValue)}
    hasEarliestStart.addWatcher { event -> onHasEarliestStartChange(event.newValue) }

    onSchedulingOptionChange(schedulingOptions.value)
    onHasEarliestStartChange(hasEarliestStart.value)
  }

  fun getFxNode() = StackPane().apply {
    background = Background(BackgroundFill("Panel.background".colorFromUiManager(), CornerRadii.EMPTY, Insets.EMPTY))
    val leftPane = PropertySheetBuilder(RootLocalizer).pane {
      stylesheet("/biz/ganttproject/task/TaskPropertiesDialog.css")
      title(RootLocalizer.create("Main Properties"))
      text(nameOption)
      checkbox(milestoneOption)

      skip()
      dropdown(schedulingOptions)
      date(startDateOption)
      date(endDateOption)
      numeric(durationOption) {
        minValue = 1
      }

      skip()
      custom(earliestStartOption, run {
        HBox().apply {
          alignment = Pos.CENTER
          spacing = 5.0
          val dateEditor = this@pane.createDateOptionEditor(earliestStartOption)
          children.add(this@pane.createBooleanOptionEditor(hasEarliestStart))
          children.add(dateEditor)
          children.add(Hyperlink("Copy Start Date"))
          disableProperty().subscribe { oldValue, newValue ->
            if (!oldValue && newValue) {
              isDisable = false
              dateEditor.isDisable = true
            } else if (oldValue && !newValue) {
              dateEditor.isDisable = false
            }
          }
        }
      })
      dropdown(priorityOption)
      numeric(progressOption) {
        minValue = 0
        maxValue = 100
      }

      skip()
      title(RootLocalizer.create("View"))
      checkbox(showInTimelineOption)
      color(colorOption)
      dropdown(textureOption) {
        cellFactory = { _, p ->
          Rectangle(200.0, 20.0).also {
            it.fill = p.first.paint.asJavaFxPattern()
          }
        }
      }
    }
    val grid = GridPane()

    val rightPane = PropertySheetBuilder(RootLocalizer).pane {
      stylesheet("/biz/ganttproject/task/TaskPropertiesDialog.css")
      title(RootLocalizer.create("Documents"))
      text(notesOption) {
        isMultiline = true
        labelPosition = LabelPosition.ABOVE
      }
      custom(webLinkOption,
        HBox().apply {
          val editor = this@pane.createStringOptionEditor(webLinkOption)
          alignment = Pos.CENTER
          children.add(editor)
          children.add(Button("Open"))
          HBox.setHgrow(editor, ALWAYS)
        }
      )
    }
    grid.add(leftPane.node, 0, 0)
    grid.add(rightPane.node, 1, 0)
    children.add(grid)
  }

  fun save(taskMutator: TaskMutator) {
    nameOption.ifChanged(taskMutator::setName)
    milestoneOption.ifChanged(taskMutator::setMilestone)

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

}