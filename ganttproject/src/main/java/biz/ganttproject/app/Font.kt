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

import biz.ganttproject.core.option.FontSpec
import javafx.beans.property.SimpleObjectProperty
import net.sourceforge.ganttproject.GPLogger
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.io.File
import java.nio.file.Files
import javafx.scene.text.Font as FxFont

val applicationFont = SimpleObjectProperty(javafx.scene.text.Font.getDefault())
val applicationFontSpec = SimpleObjectProperty<FontSpec>(null)

/**
 * @author dbarashev@bardsoftware.com
 */
object FontManager {
  val fontFamilies by lazy {
    val swingFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames
    val javafxFamilies = FxFont.getFamilies()
    swingFamilies.toSet().intersect(javafxFamilies.toSet()).sorted().toList()
  }
  val fallbackFontFile by lazy {
    val cachedFile = File(fontCacheDir, FALLBACK_FONT_FILENAME)
    val fontFile = if (cachedFile.canWrite() || !cachedFile.exists() && cachedFile.parentFile.canWrite()) {
      cachedFile
    } else {
      Files.createTempFile("ganttproject_fallback_font", ".ttc").toFile().also {
        it.deleteOnExit()
      }
    }
    fontFile.also { file ->
      if (!file.exists() || file.length() == 0L) {
        FontManager::class.java.getResourceAsStream(FALLBACK_FONT_PATH)?.readAllBytes()?.also { file.writeBytes(it) }
          ?: run {
            println("Fallback font not found!")
          }
      }
    }
  }
  val fontCacheDir by lazy {
    val userHomeDir = File(System.getProperty("user.home").replace("\\", "/"))
    if (userHomeDir.exists() && userHomeDir.isDirectory && userHomeDir.canWrite()) {
      Files.createDirectories(userHomeDir.toPath().resolve(".ganttproject.d/font-cache")).toFile()
    } else {
      Files.createTempDirectory(".gp-font-cache").toFile()
    }
  }

  init {
    try {
      val fallbackAwtFonts = Font.createFonts(fallbackFontFile)
      if (fallbackAwtFonts.isNotEmpty()) {
        LOGGER.debug("Registering fallback font {} in AWT", fallbackAwtFonts[0])
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(fallbackAwtFonts[0])
      }
      FxFont.loadFonts(fallbackFontFile.inputStream(), -1.0)
    } catch (ex: Exception) {
      LOGGER.error("Failed to register fallback font", ex)
    }
  }
}

/**
 * FontSpec does not define the absolute font size, so this function resolves the FontSpec size relative to the default
 * font size.
 */
fun FontSpec.getSizePt() = this.size.factor * javafx.scene.text.Font.getDefault().size

private val LOGGER = GPLogger.create("App.Fonts").delegate()
private const val FALLBACK_FONT_FILENAME = "DroidSansFallbackFull.ttc"
private const val FALLBACK_FONT_PATH = "/fonts/$FALLBACK_FONT_FILENAME"
