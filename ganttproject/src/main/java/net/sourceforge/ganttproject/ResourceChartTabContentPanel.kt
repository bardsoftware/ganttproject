/*
 * Created on 22.10.2005
 */
package net.sourceforge.ganttproject

import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.chart.Chart
import net.sourceforge.ganttproject.gui.view.GPView
import javax.swing.JComponent
import net.sourceforge.ganttproject.chart.overview.ToolbarBuilder
import net.sourceforge.ganttproject.resource.HumanResource
import java.awt.Component

internal class ResourceChartTabContentPanel(
  project: IGanttProject, workbenchFacade: UIFacade, private val myTreeFacade: GanttResourcePanel,
  override val chartComponent: Component
) : ChartTabContentPanel(project, workbenchFacade, workbenchFacade.resourceChart), GPView {

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

  override fun getChart(): Chart {
    return getUiFacade().resourceChart
  }

  override fun getViewComponent(): Component {
    return component
  }

  init {
    addTableResizeListeners(myTreeFacade.treeComponent, myTreeFacade.treeTable.scrollPane.viewport)
  }
}
