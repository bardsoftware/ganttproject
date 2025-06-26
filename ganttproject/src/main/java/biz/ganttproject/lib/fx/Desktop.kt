package biz.ganttproject.lib.fx

import javafx.scene.image.Image
import javafx.stage.Stage
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.GanttProject
import java.awt.Desktop
import java.awt.Taskbar
import java.awt.Toolkit
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

fun Stage.installDockIcon() {
  val iconStream = { GanttProject::class.java.getResource("/icons/ganttproject-logo-512.png") }
  if (Taskbar.isTaskbarSupported()) {
    val taskbar = Taskbar.getTaskbar();

    if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
      val defaultToolkit = Toolkit.getDefaultToolkit();
      var dockIcon = defaultToolkit.getImage(iconStream());
      taskbar.setIconImage(dockIcon);
    }
  }
  val iconImage = Image(iconStream()!!.openStream())
  this.icons.add(iconImage)
}
