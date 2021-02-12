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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.plugins.PluginManager
import net.sourceforge.ganttproject.search.SearchResult
import net.sourceforge.ganttproject.search.SearchService
import net.sourceforge.ganttproject.search.SearchUi
import org.controlsfx.control.PopOver
import org.controlsfx.control.textfield.CustomTextField
import javax.swing.JComponent

/**
 * This class implements SearchUi in JavaFX.
 *
 * Currently it is just a TextField which invokes Swing results UI when user hits Enter.
 *
 * @author dbarashev@bardsoftware.com
 */
class FXSearchUi(private val project: IGanttProject, private val uiFacade: UIFacade) : SearchUi {
  private val textField: CustomTextField by lazy {
    CustomTextField().also {
      it.right = FontAwesomeIconView(FontAwesomeIcon.SEARCH).also { icon ->
        icon.onMouseClicked = EventHandler { this.runSearch() }
      }
      it.onKeyPressed = EventHandler { evt ->
        if (evt.code == KeyCode.ENTER) {
          this.runSearch()
        }
      }
      it.promptText = "Ctrl+F"
    }
  }

  val node: Node get() = textField
  lateinit var swingToolbar: () -> JComponent

  private fun runSearch() {
//    val textFieldBounds = this.textField.run {
//      val bounds = localToScene(boundsInLocal)
//      Rectangle(bounds.minX.toInt(), bounds.minY.toInt(), bounds.width.toInt(), bounds.height.toInt())
//    }
    val results = FXCollections.observableArrayList<SearchResult<*>>()
    val listView = ListView(results).also {
      it.setCellFactory {
        createSearchCell()
      }
    }
    val query = this.textField.text
    val popOver = PopOver().also {
      it.arrowLocation = PopOver.ArrowLocation.TOP_RIGHT
      it.contentNode = ScrollPane(listView)
      it.show(this.textField)
    }
    listView.onMouseClicked = EventHandler { evt ->
      if (evt.clickCount == 2) {
        onAction(listView)
        popOver.hide()
      }
    }
    listView.onKeyPressed = EventHandler { keyEvent ->
      if (keyEvent.code == KeyCode.ENTER) {
        onAction(listView)
        popOver.hide()
      }
    }

    val resultChannel = Channel<SearchResult<*>>()
    GlobalScope.launch {
      runSearch(query, project, uiFacade).forEach { resultChannel.send(it) }
      resultChannel.close()
    }
    GlobalScope.launch(Dispatchers.JavaFx) {
      for (result in resultChannel) {
        results.add(result)
      }
    }
//    SwingUtilities.invokeLater {
//      val searcher = PopupSearchCallback(this.project, this.uiFacade, this.swingToolbar(), textFieldBounds)
//      searcher.runSearch(query)
//    }

  }

  private fun onAction(listView: ListView<SearchResult<*>>) {
    //selectedValue.getSearchService().select(Collections.singletonList(selectedValue));
    listView.selectionModel.selectedItem?.also {
      val service: SearchService<SearchResult<*>, out Any> = it.searchService as SearchService<SearchResult<*>, out Any>
      service.select(mutableListOf(it))
    }
  }

  private fun createSearchCell(): ListCell<SearchResult<*>> = SearchCell()


  override fun requestFocus() {
    textField.requestFocus()
  }
}


private fun runSearch(text: String, project: IGanttProject, uiFacade: UIFacade): List<SearchResult<*>> {
  val services = PluginManager.getExtensions(
    SearchService.EXTENSION_POINT_ID,
    SearchService::class.java
  )
  val result = mutableListOf<SearchResult<*>>()
  for (service in services) {
    service.init(project, uiFacade)
    result.addAll(service.search(text))
  }
  return result
}

class SearchCell : ListCell<SearchResult<*>>() {
  private fun whenNotEmpty(item: SearchResult<*>?, empty: Boolean, code: (SearchResult<*>) -> Unit) {
    if (item == null) {
      text = ""
      graphic = null
      return
    }
    super.updateItem(item, empty)
    if (empty) {
      text = ""
      graphic = null
      return
    }
    code(item)
  }

  override fun updateItem(item: SearchResult<*>?, empty: Boolean) {
    whenNotEmpty(item, empty) {
      graphic = Label(it.label)
    }
  }
}
