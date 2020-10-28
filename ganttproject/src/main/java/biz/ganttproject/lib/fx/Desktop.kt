package biz.ganttproject.lib.fx

import net.sourceforge.ganttproject.GPLogger
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import javax.swing.SwingUtilities

/**
 * @author dbarashev@bardsoftware.com
 */
fun openInBrowser(url: String) {
  SwingUtilities.invokeLater {
    try {
      Desktop.getDesktop().browse(URI(url))
    } catch (e: IOException) {
      GPLogger.log(e)
    } catch (e: URISyntaxException) {
      GPLogger.log(e)
    }
  }
}

fun isBrowseSupported(): Boolean = Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
