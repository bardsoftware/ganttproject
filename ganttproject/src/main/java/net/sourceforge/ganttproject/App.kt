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

import biz.ganttproject.LoggerApi
import biz.ganttproject.app.*
import biz.ganttproject.platform.DummyUpdater
import biz.ganttproject.storage.cloud.GPCloudEnv
import biz.ganttproject.storage.cloud.getCloudEnv
import com.beust.jcommander.JCommander
import com.sun.javafx.application.LauncherImpl
import javafx.application.Platform
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.sourceforge.ganttproject.export.CommandLineExportApplication
import net.sourceforge.ganttproject.gui.CommandLineProjectOpenStrategy
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.plugins.PluginManager
import net.sourceforge.ganttproject.task.TaskManagerImpl
import org.slf4j.Logger
import java.io.File
import java.lang.Thread.UncaughtExceptionHandler
import java.util.*
import javax.swing.JOptionPane
import javax.swing.SwingUtilities


fun main(args: Array<String>) {
  _explicitTranslation = createTranslation(Locale.US, ResourceBundle.getBundle("i18n"))
  RootLocalizer = SingleTranslationLocalizer(defaultTranslation)
  var builder = AppBuilder(args).withLogging().withWindowVisible().runBeforeUi {
    PluginManager.setViewProviders(listOf())
    GanttLanguage.getInstance()
  }
  if (getCloudEnv() == GPCloudEnv.EMULATOR) {
    builder = builder.withDocument("cloud://asdfg/Test Team/Test Project")
  }
  builder.whenAppInitialized {
    it.updater = DummyUpdater
  }.launch()
}

fun startUiApp(configure: (GanttProject) -> Unit = {}) {
  APP_LOGGER.debug("Starting the UI.")
  Platform.setImplicitExit(true)
  applicationBarrier.await { ganttProjectFxApp ->
      Thread.setDefaultUncaughtExceptionHandler { t, e ->
        APP_LOGGER.error("Uncaught exception", exception = e)
        e.printStackTrace()
      }
      try {
        val ganttProject = GanttProject(ganttProjectFxApp.stage)
        configure(ganttProject)
        ganttProjectFxApp.ganttProject = ganttProject
        ganttProject.notificationManagerImpl.setOwner(ganttProjectFxApp.stage)
        APP_LOGGER.debug("Main frame created")
      } catch (e: Throwable) {
        APP_LOGGER.error("Failure when launching application", exception = e)
        e.printStackTrace()
        val msg = """Failed to launch the UI:
        |${e.message}
        |
        |More details in the log file: ${GPLogger.getLogFile()} 
      """.trimMargin()
        JOptionPane.showMessageDialog(null, msg, "Failed to launch the UI", JOptionPane.ERROR_MESSAGE)
        System.exit(1)
      }
  }
  val job = GlobalScope.launch {
    // This hack was borrowed from StackOverflow: https://stackoverflow.com/questions/31219169/javafx-application-name-on-gnome/54467323#54467323
    LauncherImpl.launchApplication(
      GanttProjectFxApp::class.java,
      GanttProjectFxPreloader::class.java,
      arrayOf()
    )
  }
  runBlocking {
    job.join()
  }
}

val APP_LOGGER: LoggerApi<Logger> = GPLogger.create("App")

typealias RunBeforeUi = ()->Unit
typealias RunAfterWindowOpened = () -> Unit
typealias RunAfterAppInitialized = (GanttProject) -> Unit
typealias RunWhenDocumentReady = (IGanttProject) -> Unit

class AppBuilder(args: Array<String>) {
  val mainArgs = GanttProject.Args()
  val cliArgs = CommandLineExportApplication.Args()
  val cliParser = JCommander(arrayOf(mainArgs, cliArgs), *args)

  fun isCli(): Boolean = !cliArgs.exporter.isNullOrBlank()
  private val runBeforeUiCommands = mutableListOf<RunBeforeUi>()
  private val runAfterWindowOpenedCommands = mutableListOf<RunAfterWindowOpened>()
  private val runAfterAppInitializedCommands = mutableListOf<RunAfterAppInitialized>()
  private val runWhenDocumentReady = mutableListOf<RunWhenDocumentReady>()

  fun runBeforeUi(cmd: RunBeforeUi): AppBuilder {
    runBeforeUiCommands.add(cmd)
    return this
  }
  fun withLogging(): AppBuilder {
    runBeforeUi {
      if (mainArgs.log && "auto" == mainArgs.logFile) {
        mainArgs.logFile = System.getProperty("user.home") + File.separator + "ganttproject.log"
      }
      if (mainArgs.log && mainArgs.logFile.trim().isNotEmpty()) {
        try {
          GPLogger.setLogFile(mainArgs.logFile)
        } catch (ex: Exception) {
          println("Failed to write log to file: " + ex.message)
          ex.printStackTrace()
          System.exit(1)
        }
      }

      GPLogger.logSystemInformation()

    }
    Runtime.getRuntime().addShutdownHook(Thread {
      GPLogger.printLogLocation()
    })
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
      GPLogger.log(e)
    }
    SwingUtilities.invokeLater {
      Thread.currentThread().uncaughtExceptionHandler = UncaughtExceptionHandler { _, e ->
          e.printStackTrace()
          GPLogger.log(e)
      }
    }
    whenWindowOpened {
      Platform.runLater {
        Thread.currentThread().uncaughtExceptionHandler = UncaughtExceptionHandler {
            _, e -> GPLogger.log(e)
        }
      }
    }
    return this
  }
  fun withSplash(): AppBuilder {
    val splashCloser = showAsync().get()
    whenWindowOpened {
      try {
        APP_LOGGER.debug("Closing the splash window")
        splashCloser.run()
      } catch (ex: Exception) {
        ex.printStackTrace()
      }
    }
    return this
  }
  fun withWindowVisible(): AppBuilder {
    whenAppInitialized { ganttProject ->
      ganttProject.doShow()
    }
    return this
  }

  fun withDocument(path: String?): AppBuilder {
    whenAppInitialized { ganttProject ->
      val strategy = CommandLineProjectOpenStrategy(
        ganttProject.project, ganttProject.documentManager,
        ganttProject.taskManager as TaskManagerImpl,
        ganttProject.uiFacade, ganttProject.projectUIFacade,
        ganttProject.ganttOptions.pluginPreferences
      )
      ganttProject.addProjectEventListener(object : ProjectEventListener.Stub() {
        override fun projectOpened(
          barrierRegistry: BarrierEntrance,
          barrier: Barrier<IGanttProject>
        ) {
          barrier.await { runWhenDocumentReady.forEach { cmd -> cmd(ganttProject.project) } }
        }
      })
      strategy.openStartupDocument(path)
    }
    return this
  }

  fun whenAppInitialized(code: RunAfterAppInitialized): AppBuilder {
    runAfterAppInitializedCommands.add(code)
    return this
  }
  fun whenWindowOpened(code: RunAfterWindowOpened): AppBuilder {
    runAfterWindowOpenedCommands.add(code)
    return this
  }
  fun whenDocumentReady(code: RunWhenDocumentReady): AppBuilder {
    runWhenDocumentReady.add(code)
    return this
  }

  fun launch() {
    runBeforeUiCommands.forEach { cmd -> cmd() }
    startUiApp { ganttProject: GanttProject ->
      ganttProject.updater = org.eclipse.core.runtime.Platform.getUpdater() ?: DummyUpdater
      ganttProject.uiFacade.onWindowOpened {
        APP_LOGGER.debug("Window opened. Running afterWindowOpened commands.")
          runAfterWindowOpenedCommands.forEach { cmd -> cmd() }
      }
      ganttProject.uiInitializationPromise.await {
        APP_LOGGER.debug("UI initialized. Running afterAppInitialized commands.")
        runAfterAppInitializedCommands.forEach { cmd -> cmd(ganttProject) }
      }
    }
  }
}
