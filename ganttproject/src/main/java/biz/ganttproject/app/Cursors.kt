package biz.ganttproject.app

import javafx.scene.Cursor
import javafx.scene.ImageCursor
import javafx.scene.image.Image
import net.sourceforge.ganttproject.GanttGraphicArea

enum class GPCursor(val fxCursor: Cursor) {
  Default(Cursor.DEFAULT),
  DragTaskStart(Cursor.E_RESIZE),
  DragTaskEnd(Cursor.W_RESIZE),
  DragTaskProgress(PERCENT_CURSOR),
  DragView(DRAG_CURSOR)
}

private val PERCENT_CURSOR: Cursor = createCursor("icons/cursorpercent.gif")
private val DRAG_CURSOR: Cursor = createCursor("icons/16x16/chart-drag.png")
private fun createCursor(imageResource: String) = run {
  val url = GanttGraphicArea::class.java.classLoader.getResource(imageResource)
  if (url != null) {
    val image = Image(url.toExternalForm())
    ImageCursor(image, 10.0, 5.0)  // hotspot matches AWT (10, 5)
  } else Cursor.DEFAULT
}
