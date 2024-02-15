/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.app

import biz.ganttproject.core.option.IntegerOption
import biz.ganttproject.lib.fx.vbox
import javafx.embed.swing.SwingNode
import javafx.geometry.Orientation
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.SplitPane
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.gui.UIFacade
import javax.swing.JComponent
import javax.swing.SwingUtilities

interface View {
  var isVisible: Boolean
  var isActive: Boolean;
}

class ViewPane {
  private val tabPane = TabPane()
  private val localizer = RootLocalizer.createWithRootKey("view")
  var onViewCreated: ()->Unit = {}
  val selectedViewId get() = tabPane.selectionModel.selectedItem.id
  fun createComponent(): Parent = tabPane

  fun createView(viewComponent: Node, viewId: String): View {
    val tab = Tab().also {
      it.content = viewComponent
      it.text = localizer.formatText("$viewId.label")
      it.id = viewId
      onViewCreated()
    }
    tabPane.tabs.add(tab)
    tabPane.layout()
    return ViewImpl(tabPane, tab)
  }
}

private class ViewImpl(private val tabPane: TabPane, private val tab: Tab): View {
  override var isVisible: Boolean
    get() = true
    set(value) {}

  override var isActive: Boolean
    get() = tab.isSelected
    set(value) {
      if (value) {
        tabPane.selectionModel.select(tab)
      }
    }
}

data class ViewComponents(val image: Pane, val splitPane: SplitPane, val table: Node) {
  private var isDividerInitialized = false
  fun initializeDivider(columnsWidth: Double) {
    if (!isDividerInitialized) {
      splitPane.setDividerPosition(0, columnsWidth/splitPane.width)
      isDividerInitialized = true
    }
  }
}

fun createViewComponents(
  tableToolbarBuilder: ()->Node,
  tableBuilder: ()->Node,
  chartToolbarBuilder: ()->Node,
  chartBuilder: ()-> JComponent,
  dpiOption: IntegerOption): ViewComponents {

  val defaultScaledHeight =
    (UIFacade.DEFAULT_LOGO.iconHeight * dpiOption.value / (1f * UIFacade.DEFAULT_DPI)).toInt()
  val image = Image(ViewPane::class.java.getResourceAsStream("/icons/big.png"))
  val imageView = ImageView().apply {
    this.image = image
    fitHeight = defaultScaledHeight.toDouble()
    //isPreserveRatio = true
    viewport = Rectangle2D(0.0, 0.0, image.width, defaultScaledHeight.toDouble())

  }
  val imagePane = Pane(imageView).also { it.minHeight = defaultScaledHeight.toDouble() }

  val table = tableBuilder()
  val splitPane = SplitPane().also {split ->

    split.orientation = Orientation.HORIZONTAL

    val left = vbox {
      add(tableToolbarBuilder())
      add(imagePane)
      add(table, null, growth = Priority.ALWAYS)
    }
    split.items.add(left)

    val swingNode = SwingNode()
    val right = vbox {
      add(chartToolbarBuilder())
      add(swingNode, null, Priority.ALWAYS)
    }
    SwingUtilities.invokeLater { swingNode.content = chartBuilder() }
    split.items.add(right)
    split.setDividerPosition(0, 0.5)
  }
  return ViewComponents(image = imagePane, splitPane = splitPane, table = table)
}