/*
Copyright 2017 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.lib.fx

import biz.ganttproject.app.LocalizedString
import biz.ganttproject.app.RootLocalizer
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.event.ActionEvent
import javafx.event.Event
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Label
import javafx.scene.layout.*

class ListItemBuilder(val contentNode: Node) {
  private val result: HBox = HBox()
  var hoverNode: Node? = null
  var onSelectionChange: ((node: Parent) -> Unit)? = null

  fun build(): Pane {
    result.stylesheets.add("biz/ganttproject/lib/fx/GPList.css")
    result.styleClass.add("list-item")

    contentNode.styleClass.add("content-node")
    contentNode.addEventHandler(ActionEvent.ACTION, { _ -> onSelectionChange?.invoke(result) })
    if (contentNode is Region) {
      contentNode.maxWidth = Double.MAX_VALUE
    }
    result.children.addAll(this.contentNode)
    HBox.setHgrow(contentNode, Priority.ALWAYS)
    hoverNode?.let {
      it.styleClass.add("hover-node")
      result.children.addAll(it)
    }
    return result
  }
}

fun buildFontAwesomeButton(iconName: String, label: String? = null, onClick: ((Event) -> Unit)? = null, styleClass: String? = null): Button {
  val contentDisplay = if (label == null) ContentDisplay.GRAPHIC_ONLY else ContentDisplay.LEFT
  val size = if (label == null) "100%" else "1em"
  val result = FontAwesomeIconFactory.get().createIconButton(
      FontAwesomeIcon.valueOf(iconName.toUpperCase()), label ?: "", size, size, contentDisplay)
  if (onClick != null) {
    result.addEventHandler(ActionEvent.ACTION, onClick)
  }
  if (styleClass != null) {
    result.styleClass.add(styleClass)
  }
  return result
}

fun test(): Pane {
  val builder = ListItemBuilder(buildFontAwesomeButton(
      iconName = "cloud", label = "Foo",
      onClick = fun(_) {
        System.out.println("!!!")
      }))
  builder.onSelectionChange = { node: Node -> println(node) }
  builder.hoverNode = buildFontAwesomeButton(
      iconName = "cog",
      styleClass = "settings",
      onClick = { _ -> println("settings clicked") })

  return builder.build()
}

class VBoxBuilder(vararg classes: String) {
  var i18n = RootLocalizer
  val vbox = VBox()

  init {
    vbox.styleClass.addAll(classes)
  }

  fun addTitle(i18nKey: String, vararg args: String): Node {
    return addTitle(i18n.create(i18nKey).update(*args))
  }

  fun addTitleString(title: String): HBox {
    val titleBox = HBox()
    titleBox.styleClass.add("title")
    val title = Label().also { it.text = title }
    titleBox.children.add(title)
    add(titleBox)
    return titleBox
  }

  fun addTitle(title: LocalizedString): HBox {
    val titleBox = HBox()
    titleBox.styleClass.add("title")
    val title = Label().also { it.textProperty().bind(title) }
    titleBox.children.add(title)
    add(titleBox)
    return titleBox
  }

  fun add(node: Node) {
    add(node, alignment = null, growth = null)
  }

  fun add(node: Node, alignment: Pos?, growth: Priority?): Node {
    val child =
        if (alignment == null) {
          node
        } else {
          val wrapper = HBox()
          wrapper.alignment = alignment
          wrapper.children.add(node)
          wrapper.maxWidth = Double.MAX_VALUE

          HBox.setHgrow(node, Priority.ALWAYS)
          wrapper
        }
    vbox.children.add(child)
    growth?.let { VBox.setVgrow(child, it) }
    return child
  }

  fun addClasses(vararg classes: String) {
    this.vbox.styleClass.addAll(classes)
  }

  fun addStylesheets(vararg stylesheets: String) {
    this.vbox.stylesheets.addAll(stylesheets)
  }
}

fun vbox(code: VBoxBuilder.() -> Unit) = VBoxBuilder().apply(code).vbox
