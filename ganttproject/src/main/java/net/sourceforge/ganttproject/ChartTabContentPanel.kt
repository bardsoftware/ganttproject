/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

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

import biz.ganttproject.app.FXToolbarBuilder
import biz.ganttproject.app.getGlyphIcon
import biz.ganttproject.core.option.ChangeValueListener
import biz.ganttproject.lib.fx.applicationFont
import com.google.common.base.Preconditions
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.scene.control.MenuItem
import javafx.scene.control.Skin
import javafx.scene.control.SplitMenuButton
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.text.Text
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.chart.TimelineChart
import net.sourceforge.ganttproject.chart.overview.NavigationPanel
import net.sourceforge.ganttproject.chart.overview.ZoomingPanel
import net.sourceforge.ganttproject.gui.GanttImagePanel
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.language.GanttLanguage
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*
import javax.swing.*

internal abstract class ChartTabContentPanel(
    project: IGanttProject, workbenchFacade: UIFacade, chart: TimelineChart) {

  private val myChart: TimelineChart
  private var mySplitPane: JSplitPane? = null
  private val myPanels: MutableList<Component> = ArrayList()
  private val myUiFacade: UIFacade
  private var myImageHeight = 0
  private var myImagePanel: GanttImagePanel? = null
  protected var myHeaderHeight: () -> Int = { 0 }

  private val toolbar by lazy {
    FXToolbarBuilder().run {
      val dropdownActions = buildDropdownActions()
      addNode(buildDropdown(dropdownActions.firstOrNull(), dropdownActions))
      addTail(Pane())
      buildToolbarActions().forEach { addButton(it) }
      withScene()
      build()
    }.also {
      it.toolbar.stylesheets.add("/net/sourceforge/ganttproject/ChartTabContentPanel.css")
      it.toolbar.styleClass.remove("toolbar-big")
    }
  }

  protected open fun buildDropdownActions(): List<GPAction> = emptyList()
  protected open fun buildToolbarActions(): List<GPAction> = emptyList()

  fun createContentComponent(): JComponent {
    val tabContentPanel = JPanel(BorderLayout())
    val left = JPanel(BorderLayout())
    val treeHeader = Box.createVerticalBox()
    //treeHeader.add(toolbar.component)

    // /*
    val buttonPanel = createButtonPanel() as JComponent
    val buttonWrapper = JPanel(BorderLayout())
    buttonWrapper.add(buttonPanel, BorderLayout.CENTER)
    //button.setAlignmentX(Component.LEFT_ALIGNMENT);
    treeHeader.add(buttonWrapper)
    val defaultScaledHeight =
      (UIFacade.DEFAULT_LOGO.iconHeight * myUiFacade.dpiOption.value / (1f * UIFacade.DEFAULT_DPI)).toInt()
    myImagePanel = GanttImagePanel(myUiFacade.logo, 300, defaultScaledHeight)
    myImageHeight = myImagePanel!!.preferredSize.height
    val imageWrapper = JPanel(BorderLayout())
    imageWrapper.add(myImagePanel, BorderLayout.WEST)
    treeHeader.add(imageWrapper)
    // */

    left.add(treeHeader, BorderLayout.NORTH)
    left.add(getTreeComponent(), BorderLayout.CENTER)
    val minSize = Dimension(0, 0)
    left.minimumSize = minSize
    val right = JPanel(BorderLayout())
    val chartPanels = createChartPanels()
    right.add(chartPanels, BorderLayout.NORTH)
    right.background = Color(0.93f, 0.93f, 0.93f)
    right.add(chartComponent, BorderLayout.CENTER)
    right.minimumSize = minSize
    mySplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
    if (GanttLanguage.getInstance().componentOrientation == ComponentOrientation.LEFT_TO_RIGHT) {
      mySplitPane!!.leftComponent = left
      mySplitPane!!.rightComponent = right
      mySplitPane!!.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT)
      mySplitPane!!.dividerLocation = Math.min(300, left.preferredSize.width)
    } else {
      mySplitPane!!.rightComponent = left
      mySplitPane!!.leftComponent = right
      mySplitPane!!.dividerLocation =
        Toolkit.getDefaultToolkit().screenSize.width - Math.min(300, left.preferredSize.width)
      mySplitPane!!.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT)
    }
    mySplitPane!!.isOneTouchExpandable = true
    mySplitPane!!.resetToPreferredSizes()
    tabContentPanel.add(mySplitPane, BorderLayout.CENTER)
    val changeValueListener = ChangeValueListener {
      if (myUiFacade.dpiOption.value < 96) {
        return@ChangeValueListener
      }
      SwingUtilities.invokeLater {
        alignTopPanelHeights(buttonPanel, chartPanels)
        myImagePanel!!.setScale(myUiFacade.dpiOption.value / (1f * UIFacade.DEFAULT_DPI))
        myImageHeight = myImagePanel!!.height
        updateTimelineHeight()
      }
    }
    myUiFacade.dpiOption.addChangeValueListener(changeValueListener, 2)
    return tabContentPanel
  }

  // /*
  private fun alignTopPanelHeights(buttonPanel: JComponent, chartPanels: JComponent) {
    val maxHeight = Math.max(buttonPanel.size.height, chartPanels.size.height)
    if (buttonPanel.height < maxHeight) {
      //left.setBorder(BorderFactory.createEmptyBorder(maxHeight - buttonPanel.getHeight(), 0, 0, 0));
      val diff = maxHeight - buttonPanel.height
      val emptyBorder = BorderFactory.createEmptyBorder((diff + 1) / 2, 0, diff / 2, 0)
      buttonPanel.border = emptyBorder
    }
    if (chartPanels.height < maxHeight) {
      val diff = maxHeight - chartPanels.height
      //Border emptyBorder = BorderFactory.createEmptyBorder((diff+1)/2, 0, diff/2, 0);
      //chartPanels.setBorder(emptyBorder);
      chartPanels.remove(chartPanels.getComponent(chartPanels.componentCount - 1))
      chartPanels.add(Box.createRigidArea(Dimension(0, diff)))
    }
  }
  // */

  abstract val chartComponent: Component?
    get

  protected abstract fun getTreeComponent(): Component
  protected abstract fun createButtonPanel(): Component?
  fun getDividerLocation(): Int {
    return mySplitPane!!.dividerLocation
  }

  fun setDividerLocation(location: Int) {
    mySplitPane!!.dividerLocation = location
  }

  private fun createChartPanels(): JComponent {
    val panelsBox = Box.createHorizontalBox()
    for (panel in myPanels) {
      panelsBox.add(panel)
      panelsBox.add(Box.createHorizontalStrut(10))
    }
    return panelsBox
  }

  fun addChartPanel(panel: Component) {
    myPanels.add(panel)
  }

  protected fun getUiFacade(): UIFacade {
    return myUiFacade
  }

  protected fun updateTimelineHeight() {
    //val timelineHeight = toolbar.component.height /* + myImageHeight*/
    SwingUtilities.invokeLater {
      val timelineHeight = myHeaderHeight() + myImageHeight
      myChart.setTimelineHeight(timelineHeight)
      myChart.reset();
    }
  }

  protected fun setTableWidth(width: Double) {
    mySplitPane?.dividerLocation = width.toInt() + 1
  }
  fun addTableResizeListeners(tableContainer: Component, table: Component) {
    myHeaderHeight = {
      if (table.isShowing && tableContainer.isShowing) {
        val tableLocation = table.locationOnScreen
        val containerLocation = tableContainer.locationOnScreen
        tableLocation.y - containerLocation.y
      } else {
        0
      }
    }
    val componentListener: ComponentAdapter = object : ComponentAdapter() {
      override fun componentShown(componentEvent: ComponentEvent) {
        updateTimelineHeight()
      }

      override fun componentResized(componentEvent: ComponentEvent) {
        updateTimelineHeight()
      }

      override fun componentMoved(componentEvent: ComponentEvent) {
        updateTimelineHeight()
      }
    }
    tableContainer.addComponentListener(componentListener)
    table.addComponentListener(componentListener)
  }

  open fun setActive(active: Boolean) {
    if (active) {
      getTreeComponent().requestFocus()
      updateTimelineHeight()
    }
  }

  //  private GanttImagePanel myImagePanel;
  init {
    val navigationPanel = NavigationPanel(project, chart, workbenchFacade)
    val zoomingPanel = ZoomingPanel(workbenchFacade, chart)

    addChartPanel(FXToolbarBuilder().also {
      it.withApplicationFont(applicationFont)
      zoomingPanel.buildToolbar(it)
      it.addWhitespace()
      navigationPanel.buildToolbar(it)
    }.withClasses("toolbar-common", "toolbar-small", "toolbar-chart").withScene().build().component)

    myUiFacade = workbenchFacade
    myChart = Preconditions.checkNotNull(chart)
    myUiFacade.mainFrame.addWindowListener(object : WindowAdapter() {
      override fun windowOpened(windowEvent: WindowEvent) {
        updateTimelineHeight()
      }
    })
  }

  fun buildDropdown(titleAction: GPAction?, actions: List<GPAction>) =
    MyMenuButton().apply {
      setup(titleAction, actions)
    }
}

class MyMenuButton : SplitMenuButton() {
  private lateinit var arrowIcon: Text
  override fun createDefaultSkin(): Skin<*> {
    return super.createDefaultSkin().also {
      arrowIcon.styleClass.add("first")
      ((children[1] as StackPane).children[0] as StackPane).children.let {
        it.add(arrowIcon)
        it.add(FontAwesomeIconView(FontAwesomeIcon.ARROW_CIRCLE_DOWN).also {it.styleClass.add("second")})
      }
    }
  }

  fun setup(titleAction: GPAction?, actions: List<GPAction>) {
    styleClass.add("btn-create-item")
    text = "Add"
    arrowIcon = titleAction?.getGlyphIcon() ?: FontAwesomeIconView(FontAwesomeIcon.PLUS)
    items.addAll(actions.map { action ->
      MenuItem(GanttLanguage.correctLabel(action.localizedName)).also { item ->
        item.setOnAction { SwingUtilities.invokeLater { action.actionPerformed(null) }}
      }
    })
    setOnAction {
      titleAction?.let { SwingUtilities.invokeLater { it.actionPerformed(null) }}
    }
  }

}
