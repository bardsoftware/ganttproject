/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
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
package biz.ganttproject.app

import biz.ganttproject.core.option.ChangeValueEvent
import biz.ganttproject.core.option.DefaultFontOption
import biz.ganttproject.core.option.FontSpec
import javafx.collections.ListChangeListener
import javafx.css.PseudoClass
import javafx.scene.Parent
import javafx.stage.Window
import java.util.*

/**
 * This is an utility class that applies custom CSS rules and declarations at the window level.
 * It is based on ThemeManager code from AtlantaFX project.
 */
class AppearanceManager(private val appFontOption: DefaultFontOption) {
  private val customCSSDeclarations = mutableMapOf<String, String>() // -fx-property | value;
  private val customCSSRules = mutableMapOf<String, String>() // .foo | -fx-property: value;

  init {
    Window.getWindows().addListener { c: ListChangeListener.Change<out Window?> ->
      reloadCustomCss(c.list)
    }
    appFontOption.addChangeValueListener { _: ChangeValueEvent ->
      setFont(appFontOption.getValue())
      reloadCustomCss(Window.getWindows())
    }
    setFont(appFontOption.getValue())
  }

  fun setFont(font: FontSpec) {
    setCustomDeclaration("-fx-font-family", """"${font.family}"""")
    setCustomDeclaration("-fx-font-size", "${font.getSizePt()}")
  }

  fun reloadCustomCss(windows: List<Window>) {
    windows.forEach { w ->
      w.scene?.let {
        it.rootProperty().addListener { observable, oldValue, newValue ->
          if (newValue != null && newValue != oldValue) {
            reloadCustomCss(newValue)
          }
        }
        reloadCustomCss(it.root)
      }
    }
  }

  // --------------------------------------------------------------------------
  private fun setCustomDeclaration(property: String, value: String) {
    customCSSDeclarations[property] = value
  }

  private fun removeCustomDeclaration(property: String) {
    customCSSDeclarations.remove(property)
  }

  private fun setCustomRule(selector: String, rule: String) {
    customCSSRules[selector] = rule
  }

  @Suppress("unused")
  private fun removeCustomRule(selector: String) {
    customCSSRules.remove(selector)
  }

  private fun reloadCustomCss(node: Parent) {
    val cssUri = "data:text/css;base64," + Base64.getEncoder().encodeToString(buildStylesheet().toString().encodeToByteArray())
    if (node.stylesheets.indexOf(cssUri) >= 0) {
      node.pseudoClassStateChanged(USER_CUSTOM, true)
      return
    }
    node.stylesheets.removeIf { uri: String -> uri.startsWith("data:text/css") }
    node.stylesheets.add(cssUri)
    node.pseudoClassStateChanged(USER_CUSTOM, true)
  }

  private fun buildStylesheet() = StringBuilder().let { css ->
    css.append(""".root:${USER_CUSTOM.pseudoClassName}  {
      |${customCSSDeclarations.map { "\t ${it.key}: ${it.value};" }.joinToString(separator = "\n")}
      |}
    """.trimMargin())

    css.append(customCSSRules.map { (k, v) ->
      """
        .body:${USER_CUSTOM.pseudoClassName} $k {$v}
        
      """.trimIndent()
    }.joinToString(separator = "\n"))
  }
}

private val USER_CUSTOM = PseudoClass.getPseudoClass("user-custom")
