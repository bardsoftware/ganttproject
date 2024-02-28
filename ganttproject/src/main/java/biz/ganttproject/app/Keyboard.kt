package biz.ganttproject.app

import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import net.sourceforge.ganttproject.action.GPAction
import java.awt.event.InputEvent
import javax.swing.KeyStroke

fun KeyEvent.triggeredBy(keyStroke: KeyStroke): Boolean =
  this.code.code == keyStroke.keyCode && this.getModifiers() == (keyStroke.modifiers and 0b1111.inv())

fun KeyEvent.getModifiers(): Int =
  (if (this.isAltDown) InputEvent.ALT_DOWN_MASK else 0) or
  (if (this.isControlDown) InputEvent.CTRL_DOWN_MASK else 0) or
  (if (this.isMetaDown) InputEvent.META_DOWN_MASK else 0) or
  (if (this.isShiftDown) InputEvent.SHIFT_DOWN_MASK else 0)

fun GPAction.triggeredBy(event: KeyEvent): Boolean =
  GPAction.getAllKeyStrokes(this.id).firstOrNull { keyStroke ->
    event.triggeredBy(keyStroke)
  } != null

fun keyCombinations(actionId: String): List<KeyCombination> {
  val text = GPAction.getKeyStrokeText(actionId) ?: return emptyList()
  return text.split(",".toRegex()).dropLastWhile { it.isEmpty() }.map {
    KeyCombination.keyCombination(it.replace("pressed", " ").trim()
      .split("""\s+""".toRegex()).joinToString(separator = "+"))
  }
}

fun KeyEvent.whenMatches(actionId: String, code: () -> Unit) {
  if (keyCombinations(actionId).any { it.match(this) }) {
    this@whenMatches.consume()
    code()
  }
}