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
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.action.GPAction
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.SwingUtilities

class FXToolbar {
  val component = JFXPanel().also {
    it.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        // Otherwise key events will be propagated and may trigger unwanted actions,
        // e.g. hitting DELETE or INSERT in the search box will delete the selected
        // task or insert a new one.
        // See issue #1803: https://github.com/bardsoftware/ganttproject/issues/1803
        if (e.keyCode == KeyEvent.VK_INSERT || e.keyCode == KeyEvent.VK_DELETE) {
          e.consume()
        }
      }
    })
  }

  internal val toolbar: ToolBar by lazy {
    ToolBar()
  }

  internal val progressBar:ProgressBar by lazy {
    ProgressBar()
  }

  internal val toolbarPane: BorderPane by lazy {
    BorderPane().also {
      it.center = toolbar
      // it.bottom = progressBar
    }
  }


  internal fun init(initializer: (FXToolbar) -> Unit) {
    initializer(this)
  }
}

private typealias ToolbarVisitor = (toolbar: FXToolbar) -> Unit

fun createButton(action: GPAction, onlyIcon: Boolean = true): Button? {
  val icon = action.getGlyphIcon() ?: return null
  return Button("", icon).apply {
    if (onlyIcon) {
      this.contentDisplay = ContentDisplay.GRAPHIC_ONLY
    } else {
      this.contentDisplay = ContentDisplay.LEFT
      this.text = action.name
    }
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
}

private class ButtonVisitor(val action: GPAction) {
  fun visit(toolbar: FXToolbar) {
    createButton(action)?.let {
      toolbar.toolbar.items.add(it)
    }
  }
}

fun addSeparator(toolbar: FXToolbar) {
  toolbar.toolbar.items.add(Separator())
}

/**
 * @author dbarashev@bardsoftware.com
 */
class FXToolbarBuilder {

  private var classes: Array<out String> = arrayOf()
  private var deleteAction: GPAction? = null
  private var insertAction: GPAction? = null
  private val visitors = mutableListOf<ToolbarVisitor>()
  private var withScene = false

  fun withClasses(vararg classes: String): FXToolbarBuilder {
    this.classes = classes
    return this
  }

  fun withScene(): FXToolbarBuilder {
    this.withScene = true
    return this
  }
  fun addNode(node: Node): FXToolbarBuilder {
    visitors.add {
      it.toolbar.items.add(node)
    }
    return this
  }
  fun addButton(action: GPAction): FXToolbarBuilder {
    visitors.add(ButtonVisitor(action)::visit)
    return this
  }

  fun addWhitespace(): FXToolbarBuilder {
    visitors.add(::addSeparator)
    return this
  }

  fun addTail(action: GPAction): FXToolbarBuilder {
    createButton(action)?.let {
      addTail(it)
    }
    return this
  }

  fun addTail(tail: Node): FXToolbarBuilder {
    visitors.add(fun(toolbar: FXToolbar) {
      val spring = Region()
      HBox.setHgrow(spring, Priority.ALWAYS)
      toolbar.toolbar.items.addAll(spring, tail)
    })
    return this
  }

  fun build(): FXToolbar {
    val toolbar = FXToolbar()
    GlobalScope.launch(Dispatchers.JavaFx) {
      val scene = if (withScene) {
        Scene(toolbar.toolbar, Color.TRANSPARENT).also {
          it.stylesheets.add("biz/ganttproject/app/Toolbar.css")
        }
      } else null
      toolbar.init { toolbar ->
        visitors.forEach { it(toolbar) }
        toolbar.toolbar.let {
          it.styleClass.addAll(this@FXToolbarBuilder.classes)
          it.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED) {evt ->
            if (!evt.isConsumed) {
              when (evt.code.code) {
                insertAction?.keyStroke?.keyCode -> SwingUtilities.invokeLater { insertAction?.actionPerformed(null) }
                deleteAction?.keyStroke?.keyCode -> SwingUtilities.invokeLater { deleteAction?.actionPerformed(null) }
              }
            }
          }
        }
      }
      scene?.let {
        toolbar.component.scene = it
      }

    }
    return toolbar
  }

  fun addSearchBox(searchUi: FXSearchUi) {
    visitors.add { toolbar ->
      val grid = GridPane()
      grid.columnConstraints.addAll(
        ColumnConstraints().apply {
          percentWidth = 25.0
          hgrow = Priority.SOMETIMES
          minWidth = 20.0
        },
        ColumnConstraints().apply {
          percentWidth = 75.0
          hgrow = Priority.SOMETIMES
          minWidth = 100.0
        })
      GridPane.setValignment(searchUi.node, VPos.CENTER)
      HBox.setHgrow(grid, Priority.ALWAYS)
      grid.add(searchUi.node, 1, 0)
      toolbar.toolbar.items.addAll(grid)
      searchUi.swingToolbar = { toolbar.component }
    }
  }

  fun setArtefactActions(insertAction: GPAction, deleteAction: GPAction) {
    this.insertAction = insertAction
    this.deleteAction = deleteAction
  }
}
