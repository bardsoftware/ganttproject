/*
 * Created on 22.10.2005
 */
package net.sourceforge.ganttproject

import biz.ganttproject.app.FXToolbarBuilder
import biz.ganttproject.app.ViewComponents
import biz.ganttproject.app.createViewComponents
import biz.ganttproject.core.option.GPOption
import javafx.embed.swing.SwingNode
import javafx.scene.Node
import javafx.scene.control.ToolBar
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.chart.Chart
import net.sourceforge.ganttproject.chart.TimelineChart
import net.sourceforge.ganttproject.chart.overview.ToolbarBuilder
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.view.ViewProvider
import java.awt.Component
import javax.swing.JComponent
import javax.swing.SwingUtilities

internal class ResourceChartTabContentPanel(
  project: IGanttProject, workbenchFacade: UIFacade, private val myTreeFacade: GanttResourcePanel,
  override val chartComponent: JComponent
) : ChartTabContentPanel(project, workbenchFacade, workbenchFacade.resourceChart),
  ViewProvider {

  private lateinit var viewComponents: ViewComponents
  private var myTabContentPanel: JComponent? = null
  val component: JComponent
    get() {
      if (myTabContentPanel == null) {
        myTabContentPanel = createContentComponent()
      }
      return myTabContentPanel!!
    }

  override fun buildDropdownActions(): List<GPAction> = listOf(
    myTreeFacade.resourceActionSet.resourceNewAction,
    myTreeFacade.resourceActionSet.cloudResourceList
  )

  override fun buildToolbarActions(): List<GPAction> = listOf(
    myTreeFacade.resourceActionSet.resourceMoveUpAction,
    myTreeFacade.resourceActionSet.resourceMoveDownAction
  )

  override fun createButtonPanel(): Component? {
    val builder = ToolbarBuilder()
      .withHeight(24)
      .withSquareButtons()
      .withDpiOption(getUiFacade().dpiOption)
      .withLafOption(getUiFacade().lafOption, null)
    myTreeFacade.addToolbarActions(builder)
    val toolbar = builder.build()
    return toolbar.toolbar
  }

  override fun getTreeComponent(): Component {
    return myTreeFacade.treeComponent
  }

  private fun createToolbarBuilder(): FXToolbarBuilder {
    return FXToolbarBuilder()
      .addButton(myTreeFacade.resourceActionSet.resourceMoveUpAction.asToolbarAction())
      .addButton(myTreeFacade.resourceActionSet.resourceMoveDownAction.asToolbarAction())
      .withClasses("toolbar-common", "toolbar-small", "task-filter")
  }

  override val options: List<GPOption<*>>
    get() = emptyList()

  override val chart: Chart
    get() = getUiFacade().resourceChart
  override val node: Node
    get() {
      viewComponents = createViewComponents(
        tableToolbarBuilder = {
          val toolbar: ToolBar = createToolbarBuilder().build().toolbar
          toolbar.stylesheets.add("/net/sourceforge/ganttproject/ChartTabContentPanel.css")
          toolbar
        },
        tableBuilder = {
          SwingNode().also {
            SwingUtilities.invokeLater { it.content = getTreeComponent() as JComponent? }
          }
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
      return viewComponents.splitPane

    }
  override val id: String
    get() = "resourceChart"
  override val refresh: () -> Unit
    get() = {
      myTreeFacade.getResourceTreeTableModel().updateResources()
      (chart as? TimelineChart)?.let {
        myTreeFacade.getResourceTreeTable().setRowHeight(it.getModel().calculateRowHeight())
      }
      chart.reset()
    }


  init {
    addTableResizeListeners(myTreeFacade.treeComponent, myTreeFacade.treeTable.scrollPane.viewport)
  }
}
