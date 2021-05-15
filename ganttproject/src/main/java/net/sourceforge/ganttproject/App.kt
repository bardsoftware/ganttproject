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
import com.bardsoftware.eclipsito.update.UpdateIntegrityChecker
import com.bardsoftware.eclipsito.update.UpdateMetadata
import com.bardsoftware.eclipsito.update.UpdateProgressMonitor
import com.bardsoftware.eclipsito.update.Updater
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
import javafx.application.Platform
import java.awt.Toolkit
import java.io.File
import java.lang.reflect.Field
import javax.swing.SwingUtilities


fun main(args: Array<String>) {
  val mainArgs = GanttProject.Args()
  JCommander(arrayOf<Any>(mainArgs), *args)
  GPLogger.init(mainArgs.logbackConfig)
  RootLocalizer = SingleTranslationLocalizer(ResourceBundle.getBundle("i18n"))
  PluginManager.setCharts(listOf())
  GanttLanguage.getInstance()
  // This is a dummy updater just to make gradle run working
  val updater = object : Updater {
    override fun getUpdateMetadata(p0: String?) = CompletableFuture.completedFuture(listOf<UpdateMetadata>())
    override fun installUpdate(p0: UpdateMetadata?, p1: UpdateProgressMonitor?, p2: UpdateIntegrityChecker?): CompletableFuture<File> {
      TODO("Not yet implemented")
    }
  }
  startUiApp(mainArgs) {
    it.updater = updater
  }
}

val mainWindow = AtomicReference<GanttProject?>(null)

/**
 * @author dbarashev@bardsoftware.com
 */
@JvmOverloads
fun startUiApp(args: GanttProject.Args, configure: (GanttProject) -> Unit = {}) {
  try {
    val toolkit: Toolkit = Toolkit.getDefaultToolkit()
    val awtAppClassNameField: Field = toolkit.javaClass.getDeclaredField("awtAppClassName")
    awtAppClassNameField.isAccessible = true
    awtAppClassNameField.set(toolkit, RootLocalizer.formatText("appliTitle"))
  } catch (e: NoSuchFieldException) {
    APP_LOGGER.error("Can't set awtAppClassName (needed on Linux to show app name in the top panel)")
  } catch (e: IllegalAccessException) {
    APP_LOGGER.error("Can't set awtAppClassName (needed on Linux to show app name in the top panel)")
  }
  val autosaveCleanup = DocumentCreator.createAutosaveCleanup()

  val splashCloser = showAsync()

  Platform.setImplicitExit(false)
  SwingUtilities.invokeLater {
    try {
      val ganttFrame = GanttProject(false)
      configure(ganttFrame)
      APP_LOGGER.debug("Main frame created")
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

