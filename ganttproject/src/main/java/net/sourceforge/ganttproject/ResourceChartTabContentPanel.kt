/*
 * Created on 22.10.2005
 */
package net.sourceforge.ganttproject

import biz.ganttproject.app.FXToolbarBuilder
import biz.ganttproject.app.ViewComponents
import biz.ganttproject.app.createViewComponents
import biz.ganttproject.core.option.GPOption
import biz.ganttproject.ganttview.ResourceTable
import biz.ganttproject.ganttview.ResourceTableChartConnector
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

internal class ResourceChartTabContentPanel(
  project: IGanttProject,
  workbenchFacade: UIFacade,
  private val myTreeFacade: GanttResourcePanel,
  override val chartComponent: JComponent,
  private val resourceTableChartConnector: ResourceTableChartConnector
) : ChartTabContentPanel(project, workbenchFacade, workbenchFacade.resourceChart), ViewProvider {

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

  private val resourceTable = ResourceTable(project, workbenchFacade.undoManager, resourceTableChartConnector)

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
      return viewComponents.splitPane
    }
  override val id: String = UIFacade.RESOURCES_INDEX.toString()
  override val refresh: () -> Unit
    get() = {
      myTreeFacade.resourceTreeTableModel.updateResources()
      (chart as? TimelineChart)?.let {
        myTreeFacade.resourceTreeTable.setRowHeight(it.getModel().calculateRowHeight())
      }
      chart.reset()
    }
  override val createAction: GPAction = myTreeFacade.resourceActionSet.resourceNewAction
  override val deleteAction: GPAction = myTreeFacade.resourceActionSet.resourceDeleteAction

  /*
  private final Supplier<GPAction> taskDeleteAction = Suppliers.memoize(myTaskActions::getDeleteAction);
  private final Supplier<GPAction> resourceDeleteAction = Suppliers.memoize(() -> getResourceTree().getDeleteAction());
  private final Supplier<ArtefactAction> deleteAction = Suppliers.memoize(() ->
  new ArtefactDeleteAction(
  () -> getViewManager().getActiveView().getDeleteAction(),
  new Action[]{taskDeleteAction.get(), resourceDeleteAction.get()}
  )
  );

   */
  override val propertiesAction: GPAction
    get() = myTreeFacade.propertiesAction

  init {
    //addTableResizeListeners(myTreeFacade.treeComponent, myTreeFacade.treeTable.scrollPane.viewport)
    resourceTable.headerHeightProperty.addListener { _, _, _ -> updateTimelineHeight() };
    resourceTable.loadDefaultColumns()
    headerHeight = { resourceTable.headerHeightProperty.intValue() }
  }
}
