/*
Copyright 2026 BarD Software s.r.o

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
package net.sourceforge.ganttproject

import javafx.scene.Node
import javafx.stage.Screen
import javafx.stage.Stage
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Reaches the two helpers that wrap the message of an alert into a scroll pane. They are private
 * because nothing outside the alert has any business calling them; the tests get at them by
 * reflection rather than widening them, so that the tested code is the shipped code.
 */
object UIFacadeTestAccess {
  private fun method(name: String, vararg parameterTypes: Class<*>): Method =
    try {
      UIFacadeImpl::class.java.getDeclaredMethod(name, *parameterTypes).also { it.isAccessible = true }
    } catch (e: NoSuchMethodException) {
      throw AssertionError(
        "UIFacadeImpl.$name(${parameterTypes.joinToString { it.simpleName }}) is gone. The tests of " +
          "the alert message pane are written against it; rename them along with it.", e)
    }

  private val makeScrollableMethod by lazy { method("makeScrollable", Node::class.java, Stage::class.java) }
  private val screenOfMethod by lazy { method("screenOf", Stage::class.java) }

  /** Unwraps the reflection layer so that a test sees the exception the code itself threw. */
  private fun <T> invoke(method: Method, vararg args: Any?): T {
    try {
      @Suppress("UNCHECKED_CAST")
      return method.invoke(null, *args) as T
    } catch (e: InvocationTargetException) {
      throw e.targetException
    }
  }

  fun makeScrollable(content: Node, owner: Stage): Node = invoke(makeScrollableMethod, content, owner)

  fun screenOf(window: Stage): Screen = invoke(screenOfMethod, window)
}
