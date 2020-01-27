/*
Copyright 2019 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

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

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.SingleTranslationLocalizer
import biz.ganttproject.app.showAsync
import com.beust.jcommander.JCommander
import net.sourceforge.ganttproject.document.DocumentCreator
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.plugins.PluginManager
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.lang.Thread.UncaughtExceptionHandler
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities


fun main(args: Array<String>) {
  val mainArgs = GanttProject.Args()
  JCommander(arrayOf<Any>(mainArgs), *args)
  GPLogger.init()
  RootLocalizer = SingleTranslationLocalizer(ResourceBundle.getBundle("i18n"))
  PluginManager.setCharts(listOf())
  GanttLanguage.getInstance()
  startUiApp(mainArgs) {
    it.setUpdater {
      CompletableFuture.completedFuture(listOf())
    }
  }
}

val mainWindow = AtomicReference<GanttProject?>(null)

/**
 * @author dbarashev@bardsoftware.com
 */
@JvmOverloads
fun startUiApp(args: GanttProject.Args, configure: (GanttProject) -> Unit = {}) {
  val autosaveCleanup = DocumentCreator.createAutosaveCleanup()

  val splashCloser = showAsync()


  SwingUtilities.invokeLater {
    try {
      val ganttFrame = GanttProject(false)
      configure(ganttFrame)
      System.err.println("Main frame created")
      mainWindow.set(ganttFrame)
      ganttFrame.addWindowListener(object : WindowAdapter() {
        override fun windowOpened(e: WindowEvent) {
          try {
            splashCloser.get().run()
          } catch (ex: Exception) {
            ex.printStackTrace()
          }
        }
      })
    } catch (e: Exception) {
      APP_LOGGER.error("Failure when launching application", exception = e)
    } finally {
      Thread.currentThread().uncaughtExceptionHandler = UncaughtExceptionHandler {
        _, e -> GPLogger.log(e)
      }
    }
  }

  SwingUtilities.invokeLater { mainWindow.get()!!.doShow() }
  SwingUtilities.invokeLater { mainWindow.get()!!.doOpenStartupDocument(args) }
  if (autosaveCleanup != null) {
    ourExecutor.submit(autosaveCleanup)
  }
}

private val ourExecutor: ExecutorService = Executors.newSingleThreadExecutor()
val APP_LOGGER = GPLogger.create("App")

