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

import biz.ganttproject.lib.fx.vbox
import biz.ganttproject.walkTree
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.text.Text
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.gui.UIUtil
import java.beans.PropertyChangeListener
import java.util.*
import javax.swing.*

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

interface MenuBuilder {
  fun items(vararg actions: GPAction)
  fun items(actions: Collection<GPAction>)
  fun separator()
  fun submenu(title: String, code: (MenuBuilder)->Unit)
}

class MenuBuilderFx(private val contextMenu: ContextMenu) : MenuBuilder {
  private val stack = Stack<Function1<MenuItem, Unit>>()
  init {
    stack.push {
      contextMenu.items.add(it)
    }
  }
  private fun add(item: MenuItem) = stack.peek().invoke(item)

  override fun items(vararg actions: GPAction) {
    actions.forEach { add(it.asMenuItem()) }
  }
  override fun items(actions: Collection<GPAction>) {
    actions.forEach { add(it.asMenuItem()) }
  }
  override fun separator() { add(SeparatorMenuItem()) }
  override fun submenu(title: String, code: (MenuBuilder)->Unit) {
    Menu(title).also { menu ->
      add(menu)
      stack.push {
        menu.items.add(it)
      }
      code(this)
      stack.pop()
    }
  }
  fun build() {}
}

private data class MenuWrapper(
  val separator: () -> Unit,
  val action: (action: GPAction) -> Unit,
  val item: (item: JMenuItem) -> Unit
)

private fun JMenu.wrapper(): MenuWrapper = MenuWrapper(
  separator = { this.addSeparator() },
  action = { this.add(it) },
  item = { this.add(it) }
)

private fun JPopupMenu.wrapper(): MenuWrapper = MenuWrapper(
  separator = { this.addSeparator() },
  action = { this.add(it) },
  item = { this.add(it) }
)

class MenuBuilderSwing() : MenuBuilder {
  private val stack = Stack<MenuWrapper>()

  constructor(rootMenu: JPopupMenu) : this() {
    stack.push(rootMenu.wrapper())
  }
  constructor(rootMenu: JMenu) : this() {
    stack.push(rootMenu.wrapper())
  }

  private fun add(action: GPAction) {
    val menu = stack.peek()
    if (action == GPAction.SEPARATOR) {
      menu.separator()
      return
    }
    if (true == action.getValue(GPAction.IS_SUBMENU)) {
      addSubmenu(menu, action.name)
      return
    }
    if (action == GPAction.SUBMENU_END) {
      stack.pop()
      return
    }
    val isSelected = action.getValue(Action.SELECTED_KEY) as Boolean?
    if (isSelected == null) {
      menu.action(action)
    } else {
      menu.item(JCheckBoxMenuItem(action))
    }
  }

  override fun items(vararg actions: GPAction) {
    actions.forEach { add(it) }
  }
  override fun items(actions: Collection<GPAction>) {
    actions.forEach { add(it) }
  }
  override fun separator() { add(GPAction.SEPARATOR) }

  override fun submenu(title: String, code: (MenuBuilder)->Unit) {
    val menu = stack.peek()
    addSubmenu(menu, title)
    code(this)
  }

  private fun addSubmenu(menu: MenuWrapper, title: String) {
    JMenu(title).also {
      menu.item(it)
      stack.push(it.wrapper())
    }
  }

  fun build() {}
}

class MenuBuilderAsList : MenuBuilder {
  private val actionList = mutableListOf<GPAction>()
  override fun items(vararg actions: GPAction) {
    actionList.addAll(actions)
  }

  override fun items(actions: Collection<GPAction>) {
    actionList.addAll(actions)
  }

  override fun separator() {
    actionList.add(GPAction.SEPARATOR)
  }

  override fun submenu(title: String, code: (MenuBuilder) -> Unit) {
    val submenuBuilder = MenuBuilderAsList()
    code(submenuBuilder)
    actionList.add(GPAction.createVoidAction(title).also { it.putValue(GPAction.IS_SUBMENU, true)})
    actionList.addAll(submenuBuilder.actionList)
    actionList.add(GPAction.SUBMENU_END)
  }

  fun actions() = actionList.toList()
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

fun GPAction.getHelpText(): String? = RootLocalizer.formatTextOrNull("${this.id}.help")

private val gpActionListener = WeakHashMap<GPAction, PropertyChangeListener>()
fun GPAction.asMenuItem(): MenuItem =
  if (this == GPAction.SEPARATOR) {
    SeparatorMenuItem()
  } else {
    val node: Node = getValue(Action.SELECTED_KEY)?.let { isSelected ->
      CheckBox(name).also { checkBox ->
        checkBox.graphic = vbox {
          val helpText = this@asMenuItem.getHelpText()
          addClasses("custom-checkbox-menu-item")
          add(Label(name).also {
            if (helpText != null) {
              it.styleClass.add("title")
            }
          })
          helpText?.let {
            add(Label(it).also {
              it.styleClass.add("help")
            })
          }
        }
        checkBox.contentDisplay = ContentDisplay.GRAPHIC_ONLY
        checkBox.isSelected = isSelected as Boolean
        checkBox.onAction = EventHandler { _ ->
          this.putValue(Action.SELECTED_KEY, checkBox.isSelected)
          SwingUtilities.invokeLater {
            this.actionPerformed(null)
          }
        }

        gpActionListener[this]?.let { this.removePropertyChangeListener(it) }
        PropertyChangeListener {
          checkBox.isSelected = (this.getValue(Action.SELECTED_KEY) as? java.lang.Boolean)?.booleanValue() ?: false
        }.also {
          this.addPropertyChangeListener(it)
          gpActionListener[this] = it
        }
      }
    } ?: Label(name).also {label ->
      getGlyphIcon()?.let { icon ->
        label.graphic = icon
      }
    }
    val menuItem = CustomMenuItem(node)
    menuItem.onAction = EventHandler { _ ->
      SwingUtilities.invokeLater {
        this.actionPerformed(null)
      }
    }
    menuItem.also {
      it.isDisable = !isEnabled
    }
  }
