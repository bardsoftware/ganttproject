package biz.ganttproject.lib.fx

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.event.ActionEvent
import javafx.event.Event
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region

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
      onClick = fun(event: Event) {
        System.out.println("!!!")
      }))
  with(builder) {
    onSelectionChange = { node: Node -> println(node) }
    hoverNode = buildFontAwesomeButton(
        iconName = "cog",
        styleClass = "settings",
        onClick = { event -> println("settings clicked") })
  }
  return builder.build()
}
