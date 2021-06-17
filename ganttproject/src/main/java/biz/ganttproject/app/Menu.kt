/*
Copyright 2020 BarD Software s.r.o

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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.text.Text
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.gui.UIUtil
import java.util.*
import javax.swing.Action
import javax.swing.SwingUtilities

/**
 * @author dbarashev@bardsoftware.com
 */
class MenuBarBuilder {
  val menuBar = MenuBar()

  fun addMenu(title: String, actions: List<GPAction?>) {
    Menu(title).also {menu ->
      menuBar.menus.add(menu)

      actions.forEach {
        val menuItem = it?.let {action ->
          action.asMenuItem()
        } ?: SeparatorMenuItem()
        menu.items.add(menuItem)
      }
    }
  }
  fun build(): MenuBar {
    return menuBar
  }
}

class MenuBuilder(private val contextMenu: ContextMenu) {
  private val stack = Stack<Function1<MenuItem, Unit>>()
  init {
    stack.push { contextMenu.items.add(it) }
  }
  private fun add(item: MenuItem) = stack.peek().invoke(item)

  fun items(vararg actions: GPAction) {
    actions.forEach { add(it.asMenuItem()) }
  }
  fun items(actions: Collection<GPAction>) {
    actions.forEach { add(it.asMenuItem()) }
  }
  fun separator() { add(SeparatorMenuItem()) }
  fun submenu(title: String, code: (MenuBuilder)->Unit) {
    Menu(title).also { menu ->
      add(menu)
      stack.push { menu.items.add(it) }
      code(this)
      stack.pop()
    }
  }
  fun build() {}
}

fun (GPAction).getGlyphIcon(): Text? =
    UIUtil.getFontawesomeLabel(this)?.let { iconLabel ->
      when (UIUtil.getFontawesomeIconset(this)) {
        "fontawesome" -> {
          val icon: FontAwesomeIcon? = FontAwesomeIcon.values().firstOrNull { icon -> icon.unicode() == iconLabel }
          icon?.let { FontAwesomeIconView(it) }
        }
        "material" -> {
          val icon: MaterialIcon? = MaterialIcon.values().firstOrNull { icon -> icon.unicode() == iconLabel }
          icon?.let { MaterialIconView(it) }
        }
        else -> null
      }
    }

fun GPAction.asMenuItem(): MenuItem =
  if (this == GPAction.SEPARATOR) {
    SeparatorMenuItem()
  } else {
    val menuItem = getValue(Action.SELECTED_KEY)?.let { selected ->
      CheckMenuItem(name).also {
        it.isSelected = selected as Boolean
        it.onAction = EventHandler { _ ->
          putValue(Action.SELECTED_KEY, it.isSelected)
          SwingUtilities.invokeLater {
            actionPerformed(null)
          }
        }
      }
    } ?: MenuItem(name).also {
      it.onAction = EventHandler {
        SwingUtilities.invokeLater {
          actionPerformed(null)
        }
      }
      getGlyphIcon()?.let { icon ->
        it.graphic = icon
      }
    }
    menuItem.also {
      it.isDisable = !isEnabled
    }
  }
