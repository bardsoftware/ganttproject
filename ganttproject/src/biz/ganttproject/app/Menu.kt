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

import de.jensd.fx.glyphs.GlyphIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.input.KeyCombination
import javafx.scene.text.Text
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.gui.UIUtil
import javax.swing.SwingUtilities

/**
 * @author dbarashev@bardsoftware.com
 */
class MenuBuilder {
  val menuBar = MenuBar()

  fun addMenu(title: String, actions: List<GPAction?>) {
    Menu(title).also {menu ->
      menuBar.menus.add(menu)

      actions.forEach {
        val menuItem = it?.let {action ->
          MenuItem(action.name).apply {
            onAction = EventHandler {
              SwingUtilities.invokeLater {
                action.actionPerformed(null)
              }
            }
            action.getGlyphIcon()?.let {icon ->
              graphic = icon
            }
          }
        } ?: SeparatorMenuItem()
        menu.items.add(menuItem)
      }
    }
  }
  fun build(): MenuBar {
    return menuBar
  }
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

