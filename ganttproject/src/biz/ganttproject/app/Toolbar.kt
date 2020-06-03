/*
Copyright 2018 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

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
package biz.ganttproject.app

import javafx.embed.swing.JFXPanel
import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Separator
import javafx.scene.control.ToolBar
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.action.GPAction
import javax.swing.SwingUtilities

class FXToolbar {
  val component = JFXPanel()

  internal val toolbar: ToolBar by lazy {
    ToolBar().also {
      it.styleClass.addAll("toolbar-main")
    }
  }

  internal fun init(initializer: (FXToolbar) -> Unit) {
    val scene = Scene(toolbar, Color.TRANSPARENT)
    scene.stylesheets.add("biz/ganttproject/app/Toolbar.css")
    initializer(this)
    component.scene = scene
  }
}

private typealias ToolbarVisitor = (toolbar: FXToolbar) -> Unit

private class ButtonVisitor(val action: GPAction) {
  fun visit(toolbar: FXToolbar) {
    val icon = action.getGlyphIcon() ?: return
    val btn = Button("", icon).apply {
      this.contentDisplay = ContentDisplay.GRAPHIC_ONLY
      this.addEventHandler(ActionEvent.ACTION) {
        SwingUtilities.invokeLater {
          action.actionPerformed(null)
        }
      }
      this.isDisable = !action.isEnabled
      action.addPropertyChangeListener {
        this.isDisable = !action.isEnabled
      }
    }

    toolbar.toolbar.items.add(btn)
  }
}

fun addSeparator(toolbar: FXToolbar) {
  toolbar.toolbar.items.add(Separator())
}

/**
 * @author dbarashev@bardsoftware.com
 */
class FXToolbarBuilder {

  private val visitors = mutableListOf<ToolbarVisitor>()

  fun addButton(action: GPAction): FXToolbarBuilder {
    visitors.add(ButtonVisitor(action)::visit)
    return this
  }

  fun addWhitespace(): FXToolbarBuilder {
    visitors.add(::addSeparator)
    return this
  }

  fun addTail(tail: Node) {
    visitors.add(fun(toolbar: FXToolbar) {
      val spring = Region()
      HBox.setHgrow(spring, Priority.ALWAYS)
      toolbar.toolbar.items.addAll(spring, tail)
    })
  }

  fun build(): FXToolbar {
    val toolbar = FXToolbar()
    GlobalScope.launch(Dispatchers.Main) {
      toolbar.init { toolbar ->
        visitors.forEach { it(toolbar) }
      }
    }
    return toolbar
  }

  fun addSearchBox(searchUi: FXSearchUi) {
    visitors.add { toolbar ->
      toolbar.toolbar.items.add(searchUi.node)
      searchUi.swingToolbar = { toolbar.component }
    }
  }
}
