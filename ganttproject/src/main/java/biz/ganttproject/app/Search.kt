/*
Copyright 2018-2021 BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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

import com.sandec.mdfx.MDFXNode
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyCode
import javafx.stage.Popup
import javafx.stage.PopupWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.plugins.PluginManager
import net.sourceforge.ganttproject.search.SearchResult
import net.sourceforge.ganttproject.search.SearchService
import net.sourceforge.ganttproject.search.SearchUi
import org.controlsfx.control.textfield.CustomTextField
import java.awt.Rectangle
import javax.swing.JComponent

/**
 * This class implements SearchUi in JavaFX.
 *
 * @author dbarashev@bardsoftware.com
 */
class FXSearchUi(
  private val project: IGanttProject,
  private val uiFacade: UIFacade,
  searchAction: GPAction
) : SearchUi {
  private val textField: CustomTextField by lazy {
    CustomTextField().also {
      it.right = FontAwesomeIconView(FontAwesomeIcon.SEARCH).also { icon ->
        icon.onMouseClicked = EventHandler { this.runSearch() }
      }
      it.onKeyPressed = EventHandler { evt ->
        when (evt.code) {
          KeyCode.ENTER -> this.runSearch()
          KeyCode.KP_DOWN, KeyCode.DOWN -> {
            listView.selectionModel.select(0)
            listView.requestFocus()
          }
          KeyCode.ESCAPE -> {
            uiFacade.activeChart.focus()
          }
          else -> {}
        }
      }
      it.promptText = searchAction.keyStroke.toString().replace("pressed", "+").capitalize()
    }
  }

  val node: Node get() = textField
  lateinit var swingToolbar: () -> JComponent
  val results = FXCollections.observableArrayList<SearchResult<*>>()
  val listView: ListView<SearchResult<*>> by lazy {
    ListView(results).also {
      it.setCellFactory {
        SearchCell()
      }
    }
  }

  private fun runSearch() {
    results.clear()
    val textFieldBounds = this.textField.run {
      localToScreen(boundsInLocal).let {
        Rectangle(it.minX.toInt(), it.minY.toInt(), it.width.toInt(), it.height.toInt())
      }
    }
    val popOver = Popup()
    listView.onMouseClicked = EventHandler { evt ->
      if (evt.clickCount == 2) {
        onAction(listView)
        popOver.hide()
      }
    }
    listView.onKeyPressed = EventHandler { evt ->
      when (evt.code) {
        KeyCode.ENTER -> {
          onAction(listView)
          popOver.hide()
        }
        KeyCode.ESCAPE -> {
          popOver.hide()
        }
        KeyCode.DOWN, KeyCode.KP_DOWN -> {
          listView.requestFocus()
          if (listView.selectionModel.selectedIndices.isEmpty()) {
            listView.selectionModel.select(0)
          }
        }
        else -> {}
      }
    }

    val query = this.textField.text
    val resultChannel = Channel<SearchResult<*>>()
    GlobalScope.launch {
      runSearch(query, project, uiFacade).forEach { resultChannel.send(it) }
      resultChannel.close()
    }
    GlobalScope.launch(Dispatchers.JavaFx) {
      for (result in resultChannel) {
        results.add(result)
      }
      val scroll = ScrollPane(listView).also { scroll ->
        scroll.minWidth = textField.width
        scroll.isFitToWidth = true
      }
      if (results.isEmpty()) {
        results.add(SearchResult.EMPTY)
        scroll.maxHeight = 50.0
        scroll.vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
      } else {
        scroll.maxHeight = -1.0
        listView.maxHeight = -1.0
      }
      popOver.also {
        it.anchorLocation = PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT
        it.content.add(scroll)
        it.show(textField, textFieldBounds.maxX, textFieldBounds.minY + textFieldBounds.height)
        listView.requestFocus()
      }
    }

  }

  private fun onAction(listView: ListView<SearchResult<*>>) {
    listView.selectionModel.selectedItem?.also {
      it.searchService?.let { service ->
        (service as SearchService<SearchResult<*>, out Any>).select(mutableListOf(it))
      }
    }
  }

  override fun requestFocus() {
    swingToolbar().requestFocus()
    Platform.runLater {
      textField.requestFocus()
      textField.selectAll()
    }
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
      if (it == SearchResult.EMPTY) {
        graphic = MaterialIconView(MaterialIcon.CANCEL, "32")
        text = i18n.formatTextOrNull("noResults") ?: "No results"
        return@whenNotEmpty
      }
      val label = it.label
      val searchTerm = it.queryMatch
      val boldedLabel = label.replace(searchTerm.toRegex(RegexOption.IGNORE_CASE), "**$0**")
      val line2 = if (it.secondaryLabel.isNotEmpty()) {
        """**${it.secondaryLabel}** ${it.secondaryText.replace(searchTerm.toRegex(RegexOption.IGNORE_CASE), "**$0**")}"""
      } else ""
      val resultMd = """
        **${it.typeOfResult}** $boldedLabel
        
        $line2
        """.trimIndent()
      graphic = MDFXNode(resultMd).also {
        stylesheets.clear()
        stylesheets.add("/biz/ganttproject/app/mdfx-default.css")
      }
    }
  }
}

private val i18n = RootLocalizer.createWithRootKey("search")
