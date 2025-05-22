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
import biz.ganttproject.app.applicationFont
import biz.ganttproject.app.getGlyphIcon
import com.google.common.base.Preconditions
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.scene.control.MenuItem
import javafx.scene.control.Skin
import javafx.scene.control.SplitMenuButton
import javafx.scene.layout.StackPane
import javafx.scene.text.Text
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.chart.TimelineChart
import net.sourceforge.ganttproject.chart.overview.NavigationPanel
import net.sourceforge.ganttproject.chart.overview.ZoomingPanel
import net.sourceforge.ganttproject.gui.GanttImagePanel
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.language.GanttLanguage
import java.awt.Component
import javax.swing.JComponent
import javax.swing.SwingUtilities

internal abstract class ChartTabContentPanel(
    protected val project: IGanttProject, workbenchFacade: UIFacade, chart: TimelineChart) {

  private var zoomingPanel: ZoomingPanel = ZoomingPanel(workbenchFacade, chart)
  private var navigationPanel: NavigationPanel = NavigationPanel(project, chart, workbenchFacade)
  private val myChart: TimelineChart
  private val myPanels: MutableList<Component> = ArrayList()
  private val myUiFacade: UIFacade
  private var myImagePanel: GanttImagePanel? = null
  protected var headerHeight: () -> Int = { 0 }
  protected var imageHeight: () -> Int = { myImagePanel?.preferredSize?.height ?: 0}


  protected open fun buildDropdownActions(): List<GPAction> = emptyList()
  protected open fun buildToolbarActions(): List<GPAction> = emptyList()

  abstract val chartComponent: JComponent?
    get

  protected abstract fun getTreeComponent(): Component?
  protected abstract fun createButtonPanel(): Component?

  fun addChartPanel(panel: Component) {
    myPanels.add(panel)
  }

  protected fun getUiFacade(): UIFacade {
    return myUiFacade
  }

  protected fun updateTimelineHeight() {
    //val timelineHeight = toolbar.component.height /* + myImageHeight*/
    SwingUtilities.invokeLater {
      val timelineHeight = headerHeight() + imageHeight()
      myChart.setTimelineHeight(timelineHeight)
      myChart.reset();
    }
  }

  init {

    addChartPanel(createNavigationToolbarBuilder().withScene().build().component)

    myUiFacade = workbenchFacade
    myChart = Preconditions.checkNotNull(chart)
  //++
  //    myUiFacade.mainFrame.addWindowListener(object : WindowAdapter() {
//      override fun windowOpened(windowEvent: WindowEvent) {
//        updateTimelineHeight()
//      }
//    })
  }

  fun createNavigationToolbarBuilder() = FXToolbarBuilder().also {
    it.withApplicationFont(applicationFont)
    zoomingPanel.buildToolbar(it)
    it.addWhitespace()
    navigationPanel.buildToolbar(it)
  }.withClasses("toolbar-common", "toolbar-small", "toolbar-chart")

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
