/*
 * Copyright 2026 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.lib.fx.treetable

import com.sun.javafx.scene.control.skin.Utils
import javafx.scene.control.OverrunStyle
import javafx.scene.text.Font
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reflection-based helper to call Utils.computeClippedText() that works with both Java 21 and Java 25.
 * Java 21 version takes 5 arguments: (Font, String, double, OverrunStyle, String)
 * Java 25 version takes 6 arguments: (Font, String, double, OverrunStyle, String, AtomicBoolean)
 *
 * @author Claude Code.
 */
object UtilsReflectionHelper {

    private val computeClippedTextMethod: Method by lazy {
        findComputeClippedTextMethod()
    }

    private val usesSixArguments: Boolean by lazy {
        computeClippedTextMethod.parameterCount == 6
    }

    private fun findComputeClippedTextMethod(): Method {
        // Try to find the 6-argument version (Java 25)
        try {
            return Utils::class.java.getMethod(
                "computeClippedText",
                Font::class.java,
                String::class.java,
                Double::class.javaPrimitiveType,
                OverrunStyle::class.java,
                String::class.java,
                AtomicBoolean::class.java
            )
        } catch (e: NoSuchMethodException) {
            // Fall back to 5-argument version (Java 21)
            return Utils::class.java.getMethod(
                "computeClippedText",
                Font::class.java,
                String::class.java,
                Double::class.javaPrimitiveType,
                OverrunStyle::class.java,
                String::class.java
            )
        }
    }

    @JvmStatic
    fun computeClippedText(
        font: Font,
        text: String,
        width: Double,
        truncationStyle: OverrunStyle,
        ellipsisString: String
    ): String {
        return if (usesSixArguments) {
            // Java 25: call with AtomicBoolean
            computeClippedTextMethod.invoke(
                null,
                font,
                text,
                width,
                truncationStyle,
                ellipsisString,
                AtomicBoolean(false)
            ) as String
        } else {
            // Java 21: call without AtomicBoolean
            computeClippedTextMethod.invoke(
                null,
                font,
                text,
                width,
                truncationStyle,
                ellipsisString
            ) as String
        }
    }
}
