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

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.search.PopupSearchCallback
import net.sourceforge.ganttproject.search.SearchUi
import java.awt.Rectangle
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * This class implements SearchUi in JavaFX.
 *
 * Currently it is just a TextField which invokes Swing results UI when user hits Enter.
 *
 * @author dbarashev@bardsoftware.com
 */
class FXSearchUi(private val project: IGanttProject, private val uiFacade: UIFacade) : SearchUi {
  private val textField: TextField by lazy {
    TextField().also {
      it.onKeyPressed = EventHandler { evt ->
        if (evt.code == KeyCode.ENTER) {
          this.runSearch()
        }
      }
    }
  }

  val node: Node get() = textField
  lateinit var swingToolbar: () -> JComponent

  private fun runSearch() {
    val textFieldBounds = this.textField.run {
      val bounds = localToScene(boundsInLocal)
      Rectangle(bounds.minX.toInt(), bounds.minY.toInt(), bounds.width.toInt(), bounds.height.toInt())
    }
    val query = this.textField.text
    SwingUtilities.invokeLater {
      val searcher = PopupSearchCallback(this.project, this.uiFacade, this.swingToolbar(), textFieldBounds)
      searcher.runSearch(query)
    }
  }

  override fun requestFocus() {
    textField.requestFocus()
  }
}
