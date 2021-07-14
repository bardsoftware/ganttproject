package biz.ganttproject.app

import net.sourceforge.ganttproject.GPLogger
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.nio.file.Files
import javafx.scene.text.Font as FxFont
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
    Files.createTempFile("ganttproject_fallback_font", ".ttc").let { path ->
      FontManager::class.java.getResourceAsStream(FALLBACK_FONT_PATH)?.readAllBytes()?.also { Files.write(path, it) } ?: run {
        println("Fallback font not found!")
      }
      path.toFile()
    }
  }

  init {
    val fallbackAwtFonts = Font.createFonts(fallbackFontFile)
    if (fallbackAwtFonts.isNotEmpty()) {
      LOGGER.debug("Registering fallback font {} in AWT", fallbackAwtFonts[0])
      GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(fallbackAwtFonts[0])
    }
    FxFont.loadFonts(fallbackFontFile.inputStream(), -1.0)
  }
}

private val LOGGER = GPLogger.create("App.Fonts").delegate()
private const val FALLBACK_FONT_PATH = "/fonts/DroidSansFallbackFull.ttc"
