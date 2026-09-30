/*
Copyright 2026 BarD Software s.r.o

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
package biz.ganttproject.ganttview

import biz.ganttproject.app.*
import biz.ganttproject.core.option.ObservableObject
import biz.ganttproject.core.option.ObservableString
import biz.ganttproject.core.option.voidValidator
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.layout.BorderPane
import net.sourceforge.ganttproject.GanttPreviousState
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.chart.GanttChart
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder
import javax.swing.SwingUtilities

/**
 * Dialog that provides interface for adding and removing baselines.
 *
 * @author dbarashev@ganttproject.biz (Dmitry Barashev)
 */
class BaselineDialog(private val myProject: IGanttProject, private val myUiFacade: UIFacade) {
  private val currentBaseline = myUiFacade.getGanttChart().getBaseline()
  private val listItems = FXCollections.observableArrayList<BaselineItem>().also {
    it.addAll(myProject.baselines.map { BaselineItem(it, isShown = it == currentBaseline) })
  }

  fun show(dlg: DialogController) {
    val selectedItem = ObservableObject<BaselineItem?>("", null)
    val dialogModel = ItemListDialogModel<BaselineItem>(
      listItems,
      {
        BaselineItem(
          GanttPreviousState("", GanttPreviousState.createTasks(myProject.taskManager)),
          false
        )
      },
      ourLocalizer
    )


    // Wire the show/hide behavior of the items which are already in the list and of those which
    // will be added later on. If the shown baseline is removed from the list, hide it in the chart.
    for (item in listItems) {
      wireItem(item, listItems, dialogModel)
    }
    listItems.addListener(ListChangeListener { change: ListChangeListener.Change<out BaselineItem>? ->
      while (change!!.next()) {
        for (removed in change.getRemoved()) {
          removed.isEnabledProperty.set(false)
        }
        for (added in change.getAddedSubList()) {
          wireItem(added, listItems, dialogModel)
        }
      }
    })
    dialogModel.btnApplyController.onAction = {
      SwingUtilities.invokeLater(Runnable {
        myProject.baselines.clear()
        for (item in listItems) {
          myProject.baselines.add(item.baseline)
        }
        myProject.setModified()
      })
      Unit
    }

    val editor = BaselineItemEditor(selectedItem, dialogModel, ourLocalizer)
    val dialogPane = ItemListDialogPane<BaselineItem>(
      listItems, selectedItem,
      { item: BaselineItem ->
        ShowHideListItem(
          { item.title },
          { item.isEnabledProperty.get() },
          {
            item.isEnabledProperty.set(!item.isEnabledProperty.get())
            Unit
          },
          "", true
        )
      },
      dialogModel, editor, ourLocalizer
    )


    // Build the baseline color options pane with the JavaFX property pane builder.
    val colorOptions = myUiFacade.getGanttChart().getBaselineColorOptions()
    val colorOptionsPane = properties(ourOptionLocalizer) {
      title(
        RootLocalizer.formatText(
          OptionsPageBuilder.I18N.getCanonicalOptionGroupLabelKey(colorOptions)
        )
      )
      for (option in colorOptions.options) {
        option.visitPropertyPaneBuilder(this)
      }
    }

    val contentPane = BorderPane()
    contentPane.center = dialogPane.contentNode
    contentPane.bottom = colorOptionsPane
    dialogPane.contentNode = contentPane
    dialogPane.build(dlg)


    listItems.find { it.baseline == currentBaseline }?.let { dialogPane.listView.getSelectionModel().select(it) }
  }

  /**
   * Adds listeners which keep the item show/hide flag in sync with the chart: at most one baseline
   * can be shown, so showing one item hides all the others, and hiding the shown item removes
   * the baseline from the chart.
   */
  private fun wireItem(
    item: BaselineItem, allItems: ObservableList<BaselineItem>,
    dialogModel: ItemListDialogModel<BaselineItem>
  ) {
    item.isEnabledProperty.addListener(ChangeListener { _, _, newValue: Boolean ->
      if (newValue) {
        // Only one baseline can be shown in the chart.
        for (other in allItems) {
          if (other !== item) {
            other.isEnabledProperty.set(false)
          }
        }
        dialogModel.requireRefresh.set(true)
      }
      item.toggle(myUiFacade.getGanttChart(), newValue)
    })
  }

}

/**
 * A baseline as an item of the list view. The item title is the baseline name, and the
 * "enabled" flag indicates if the baseline is shown in the chart.
 */
private class BaselineItem(val baseline: GanttPreviousState, isShown: Boolean) : Item<BaselineItem> {
  override val isEnabledProperty: BooleanProperty = SimpleBooleanProperty()

  override var title: String
    get() = baseline.name
    set(title) {
      baseline.setName(title)
    }

  init {
    isEnabledProperty.set(isShown)
  }

  fun toggle(ganttChart: GanttChart, newValue: Boolean) {
    SwingUtilities.invokeLater(Runnable {
      if (newValue) {
        ganttChart.setBaseline(this.baseline)
        ganttChart.reset()
      } else if (ganttChart.getBaseline() === this.baseline) {
        ganttChart.setBaseline(null)
        ganttChart.reset()
      }
    })
  }
}

/**
 * Editor pane for the baseline selected in the list view. Just the baseline name can be edited.
 */
private class BaselineItemEditor private constructor(
  private val myNameOption: ObservableString, editItem: ObservableObject<BaselineItem?>,
  dialogModel: ItemListDialogModel<BaselineItem>, localizer: Localizer
) : ItemEditorPaneImpl<BaselineItem>(listOf(myNameOption), editItem, dialogModel, localizer) {
  constructor(
    editItem: ObservableObject<BaselineItem?>, dialogModel: ItemListDialogModel<BaselineItem>,
    localizer: Localizer
  ) : this(ObservableString("name", "", voidValidator, false), editItem, dialogModel, localizer)

  override fun loadData(item: BaselineItem?) {
    if (item != null) {
      myNameOption.value = item.title
      visibilityToggle.isSelected = item.isEnabledProperty.get()
    }
    enableControls(item != null)
  }

  private fun enableControls(enable: Boolean) {
    myNameOption.isWritable.value = enable
    visibilityToggle.isDisable = !enable
  }

  override fun saveData(item: BaselineItem) {
    item.title = myNameOption.value.orEmpty()
    item.isEnabledProperty.set(visibilityToggle.isSelected)
  }
}

private val ourLocalizer: Localizer = RootLocalizer
  .createWithRootKey("baseline.dialog", RootLocalizer)

// Localizer which resolves option label keys, such as option.<optionID>.label
private val ourOptionLocalizer: Localizer = RootLocalizer
  .createWithRootKey("option", RootLocalizer)
