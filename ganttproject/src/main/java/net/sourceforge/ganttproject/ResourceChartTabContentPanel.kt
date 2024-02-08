/*
 * Created on 22.10.2005
 */
package net.sourceforge.ganttproject

import javafx.scene.Node
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.chart.Chart
import net.sourceforge.ganttproject.gui.view.ViewProvider
import javax.swing.JComponent
import net.sourceforge.ganttproject.chart.overview.ToolbarBuilder
import java.awt.Component

internal class ResourceChartTabContentPanel(
  project: IGanttProject, workbenchFacade: UIFacade, private val myTreeFacade: GanttResourcePanel,
  override val chartComponent: JComponent
) : ChartTabContentPanel(project, workbenchFacade, workbenchFacade.resourceChart),
  ViewProvider {

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

  override val chart: Chart
    get() = getUiFacade().resourceChart
  override val viewComponent: Component
    get() = component
  override val node: Node
    get() = Pane()
  override val id: String
    get() = "resourceChart"
  override var persistentAttributes: Map<String, String>
    get() = mapOf()
    set(value) {}


  init {
    addTableResizeListeners(myTreeFacade.treeComponent, myTreeFacade.treeTable.scrollPane.viewport)
  }
}
