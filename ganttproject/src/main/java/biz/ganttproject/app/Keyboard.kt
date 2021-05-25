package biz.ganttproject.app

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
