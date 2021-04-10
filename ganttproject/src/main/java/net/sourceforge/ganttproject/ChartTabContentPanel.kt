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

import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.chart.TimelineChart
import net.sourceforge.ganttproject.gui.GanttImagePanel
import net.sourceforge.ganttproject.language.GanttLanguage
import biz.ganttproject.core.option.ChangeValueListener
import biz.ganttproject.core.option.ChangeValueEvent
import com.google.common.base.Preconditions
import com.google.common.base.Supplier
import java.lang.Runnable
import java.awt.event.ComponentEvent
import net.sourceforge.ganttproject.chart.overview.NavigationPanel
import net.sourceforge.ganttproject.chart.overview.ZoomingPanel
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.ArrayList
import javax.swing.*

internal abstract class ChartTabContentPanel(project: IGanttProject?, workbenchFacade: UIFacade, chart: TimelineChart) {
  private val myChart: TimelineChart
  private var mySplitPane: JSplitPane? = null
  private val myPanels: MutableList<Component> = ArrayList()
  protected val uiFacade: UIFacade
  private var myImageHeight = 0
  private var myHeaderHeight: Supplier<Int>? = null
  private var myImagePanel: GanttImagePanel? = null
  fun createContentComponent(): JComponent {
    val tabContentPanel = JPanel(BorderLayout())
    val left = JPanel(BorderLayout())
    val treeHeader = Box.createVerticalBox()
    val buttonPanel = createButtonPanel() as JComponent
    val buttonWrapper = JPanel(BorderLayout())
    buttonWrapper.add(buttonPanel, BorderLayout.WEST)
    //button.setAlignmentX(Component.LEFT_ALIGNMENT);
    treeHeader.add(buttonWrapper)
    val defaultScaledHeight =
      (UIFacade.DEFAULT_LOGO.iconHeight * uiFacade.dpiOption.value / (1f * UIFacade.DEFAULT_DPI)).toInt()
    myImagePanel = GanttImagePanel(uiFacade.logo, 300, defaultScaledHeight)
    myImageHeight = myImagePanel!!.preferredSize.height
    val imageWrapper = JPanel(BorderLayout())
    imageWrapper.add(myImagePanel, BorderLayout.WEST)
    //myImagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    treeHeader.add(imageWrapper)
    left.add(treeHeader, BorderLayout.NORTH)
    left.add(treeComponent, BorderLayout.CENTER)
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
      if (uiFacade.dpiOption.value < 96) {
        return@ChangeValueListener
      }
      SwingUtilities.invokeLater {
        alignTopPanelHeights(buttonPanel, chartPanels)
        myImagePanel!!.setScale(uiFacade.dpiOption.value / (1f * UIFacade.DEFAULT_DPI))
        myImageHeight = myImagePanel!!.height
        updateTimelineHeight()
      }
    }
    uiFacade.dpiOption.addChangeValueListener(changeValueListener, 2)
    return tabContentPanel
  }

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

  protected abstract val chartComponent: Component?
  protected abstract val treeComponent: Component
  protected abstract fun createButtonPanel(): Component
  var dividerLocation: Int
    get() = mySplitPane!!.dividerLocation
    set(location) {
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

  private fun updateTimelineHeight() {
    val timelineHeight = myHeaderHeight!!.get() + myImageHeight
    myChart.setTimelineHeight(timelineHeight)
  }

  fun addTableResizeListeners(tableContainer: Component, table: Component) {
    myHeaderHeight = Supplier {
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
      treeComponent.requestFocus()
      updateTimelineHeight()
    }
  }

  init {
    val navigationPanel = NavigationPanel(project, chart, workbenchFacade)
    val zoomingPanel = ZoomingPanel(workbenchFacade, chart)
    addChartPanel(zoomingPanel.component)
    addChartPanel(navigationPanel.component)
    uiFacade = workbenchFacade
    myChart = Preconditions.checkNotNull(chart)
    uiFacade.mainFrame.addWindowListener(object : WindowAdapter() {
      override fun windowOpened(windowEvent: WindowEvent) {
        updateTimelineHeight()
      }
    })
  }
}
