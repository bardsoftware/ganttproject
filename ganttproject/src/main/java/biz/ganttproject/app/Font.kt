package biz.ganttproject.app

import net.sourceforge.ganttproject.GPLogger
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.io.File
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

private val LOGGER = GPLogger.create("App.Fonts").delegate()
private const val FALLBACK_FONT_FILENAME = "DroidSansFallbackFull.ttc"
private const val FALLBACK_FONT_PATH = "/fonts/$FALLBACK_FONT_FILENAME"
