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

import biz.ganttproject.app.FXToolbarBuilder
import biz.ganttproject.app.ViewComponents
import biz.ganttproject.app.createButton
import biz.ganttproject.app.createViewComponents
import biz.ganttproject.core.option.DefaultDoubleOption
import biz.ganttproject.core.option.DoubleOption
import biz.ganttproject.core.option.GPOption
import biz.ganttproject.ganttview.ApplyExecutorType
import biz.ganttproject.ganttview.ResourceTable
import biz.ganttproject.ganttview.showResourceColumnManager
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ToolBar
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.action.resource.ResourceActionSet
import net.sourceforge.ganttproject.chart.Chart
import net.sourceforge.ganttproject.chart.ChartSelection
import net.sourceforge.ganttproject.chart.overview.ToolbarBuilder
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.UIUtil
import net.sourceforge.ganttproject.gui.view.ViewProvider
import java.awt.Component
import java.util.function.Supplier
import javax.swing.JComponent
import javax.swing.JLabel

internal class ResourceChartTabContentPanel(
  project: IGanttProject,
  workbenchFacade: UIFacade,
  private val tableSupplier: Supplier<ResourceTable>,
  override val chartComponent: JComponent
) : ChartTabContentPanel(project, workbenchFacade, workbenchFacade.resourceChart), ViewProvider {

  private lateinit var viewComponents: ViewComponents
  val component: JComponent? = null
  private val manageColumnsAction = GPAction.create("columns.manage.label") {
      showResourceColumnManager(resourceTable.columnList,
        project.resourceCustomPropertyManager, workbenchFacade.getUndoManager(), project.projectDatabase, ApplyExecutorType.DIRECT)
    }

  override fun buildDropdownActions(): List<GPAction> = listOf(
    resourceActions.resourceNewAction,
    resourceActions.cloudResourceList
  )

  override fun buildToolbarActions(): List<GPAction> = listOf(
    resourceActions.resourceMoveUpAction,
    resourceActions.resourceMoveDownAction
  )

  override fun createButtonPanel(): Component? {
    val builder = ToolbarBuilder()
      .withHeight(24)
      .withSquareButtons()
      .withDpiOption(getUiFacade().dpiOption)
      .withLafOption(getUiFacade().lafOption, null)
    builder.addButton(resourceActions.resourceMoveUpAction.asToolbarAction())
      .addButton(resourceActions.resourceMoveDownAction.asToolbarAction())

    val toolbar = builder.build()
    return toolbar.toolbar
  }

  override fun getTreeComponent(): Component {
    return JLabel("Unused")
  }

  private fun createToolbarBuilder(): FXToolbarBuilder {
    val manageColumnsButton: Button = createButton(GPAction.create("taskTable.tableMenuToggle") {
      manageColumnsAction.actionPerformed(null)
    }.also {
      it.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(it))
    })
    val rightComponent = HBox(0.0, manageColumnsButton)
    return FXToolbarBuilder()
      .addButton(resourceActions.resourceMoveUpAction.asToolbarAction())
      .addButton(resourceActions.resourceMoveDownAction.asToolbarAction())
      .addTail(rightComponent)
      .withClasses("toolbar-common", "toolbar-small", "task-filter")
  }

  override val options: List<GPOption<*>>
    get() = listOf(dividerOption)

  override val chart: Chart
    get() = getUiFacade().resourceChart


  private val resourceActions: ResourceActionSet = resourceTable.resourceActions
  private val resourceTable: ResourceTable get() = tableSupplier.get()

  private val dividerOption: DoubleOption = DefaultDoubleOption("divider", 0.5)
//  private val resourceTable = ResourceTable(
//    project, workbenchFacade.undoManager, workbenchFacade.resourceSelectionManager, resourceActions,
//    resourceTableChartConnector)

  override val node: Node
    get() {
      viewComponents = createViewComponents(
        tableToolbarBuilder = {
          val toolbar: ToolBar = createToolbarBuilder().build().toolbar
          toolbar.stylesheets.add("/net/sourceforge/ganttproject/ChartTabContentPanel.css")
          toolbar
        },
        tableBuilder = {
//          SwingNode().also {
//            SwingUtilities.invokeLater { it.content = getTreeComponent() as JComponent? }
//          }
          resourceTable.treeTable
        },
        chartToolbarBuilder = {
          val chartToolbarBox = HBox()
          val navigationBar = createNavigationToolbarBuilder().build().toolbar
          navigationBar.stylesheets.add("/net/sourceforge/ganttproject/ChartTabContentPanel.css")
          chartToolbarBox.children.add(navigationBar)
          HBox.setHgrow(navigationBar, Priority.ALWAYS)
          chartToolbarBox
        },
        chartBuilder = { chartComponent },
        getUiFacade().dpiOption
      )
      imageHeight = { viewComponents.image.height.toInt() }
      viewComponents.splitPane.dividers[0].positionProperty().addListener { _, _, newValue ->
        dividerOption.setValue(
          newValue.toDouble(),
          this@ResourceChartTabContentPanel
        )
      }
      resourceTable.columnListWidthProperty.addListener { _, _, _ ->
        viewComponents.initializeDivider(resourceTable.columnList.totalWidth)
      }

      return viewComponents.splitPane
    }
  override val id: String = UIFacade.RESOURCES_INDEX.toString()
  override val refresh: () -> Unit
    get() = {
      chart.reset()
    }
  override val createAction: GPAction = resourceActions.resourceNewAction
  override val deleteAction: GPAction = resourceActions.resourceDeleteAction

  override val selection: ChartSelection = ResourceChartSelection(project, workbenchFacade.resourceSelectionManager)

  override val propertiesAction: GPAction
    get() = resourceActions.resourcePropertiesAction

  init {
    //addTableResizeListeners(myTreeFacade.treeComponent, myTreeFacade.treeTable.scrollPane.viewport)
    resourceTable.headerHeightProperty.addListener { _, _, _ -> updateTimelineHeight() }
    resourceTable.loadDefaultColumns()
    headerHeight = { resourceTable.headerHeightProperty.intValue() }
  }
}
