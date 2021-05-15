/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2016 GanttProject team

This file is part of GanttProject.

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
package net.sourceforge.ganttproject.gui

import biz.ganttproject.app.OptionElementData
import biz.ganttproject.app.OptionPaneBuilder
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.option.DefaultEnumerationOption
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.storage.*
import com.google.common.base.Preconditions
import com.google.common.collect.Lists
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.action.OkAction
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.importer.Importer
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.plugins.PluginManager
import net.sourceforge.ganttproject.task.TaskImpl
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskManagerImpl
import net.sourceforge.ganttproject.task.algorithm.AlgorithmCollection
import org.jdesktop.swingx.JXRadioGroup
import org.osgi.service.prefs.Preferences
import org.xml.sax.SAXException
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import javax.swing.*

/**
 * When we open a file, we need to complete a number of steps in order to be sure
 * that should task dates change after the first scheduler run, user is informed about that.
 *
 * The steps are chained so that client can't invoke them in wrong order. Strategy is auto-closeable
 * and should be closed after using.
 *
 * @author bard
 */
internal class ProjectOpenStrategy(project: IGanttProject, uiFacade: UIFacade) : AutoCloseable {

  private val myUiFacade: UIFacade = Preconditions.checkNotNull(uiFacade)
  private val myProject: IGanttProject = Preconditions.checkNotNull(project)
  private val myDiagnostics: ProjectOpenDiagnosticImpl
  private val myCloseables = Lists.newArrayList<AutoCloseable>()
  private val myEnableAlgorithmsCmd: AutoCloseable
  private val myAlgs: AlgorithmCollection
  private val myTasks = Lists.newArrayList<Runnable>()
  private var myOldDuration: TimeDuration? = null
  private val i18n = GanttLanguage.getInstance()
  private var myResetModifiedState = true

  enum class ConvertMilestones {
    UNKNOWN, TRUE, FALSE
  }

  init {
    myDiagnostics = ProjectOpenDiagnosticImpl(myUiFacade)
    myAlgs = myProject.taskManager.algorithmCollection
    myEnableAlgorithmsCmd = AutoCloseable {
      myAlgs.scheduler.setEnabled(true)
      myAlgs.recalculateTaskScheduleAlgorithm.setEnabled(true)
      myAlgs.adjustTaskBoundsAlgorithm.setEnabled(true)
    }
  }

  override fun close() {
    for (c in myCloseables) {
      try {
        c.close()
      } catch (e: Exception) {
        GPLogger.log(e)
      }

    }
  }

  fun open(document: Document): Deferred<Document> {
    val docChannel = Channel<Document>()

    GlobalScope.launch(Dispatchers.IO) {
      val online = document.asOnlineDocument()
      if (online == null) {
        docChannel.send(document)
      } else {
        try {
          val currentFetch = online.fetchResultProperty.get() ?: online.fetch().also { it.update() }
          processFetchResult(currentFetch, document, docChannel)
        } catch (ex: ForbiddenException) {
          docChannel.close(ex)
        } catch (ex: PaymentRequiredException) {
          docChannel.close(ex)
        }
      }
    }

    return GlobalScope.async {
      try {
        docChannel.receive().also {
          it.checkWellFormed()
        }
      } catch (ex: SAXException) {
         throw Document.DocumentException(
            RootLocalizer.formatText("document.error.read.unsupportedFormat"), ex
         )
      }
    }

  }

  private suspend fun processFetchResult(fetchResult: FetchResult, document: Document, successChannel: Channel<Document>) {
    val onlineDoc = fetchResult.onlineDocument
    val mirrorDoc = onlineDoc.offlineMirror
    val offlineChecksum = mirrorDoc?.checksum()
    if (offlineChecksum == null) {
      successChannel.send(document)
      return
    }
    if (offlineChecksum == fetchResult.actualChecksum) {
      // Offline mirror and actual file online are identical, only version could change
      // Just read the online
      successChannel.send(document)
      return
    }
    if (fetchResult.syncVersion == fetchResult.actualVersion) {
      // This is the case when we have local modifications not yet written online,
      // e.g. because we have been offline for a while and went online
      // when GP was closed.
      showOfflineIsAheadDialog(fetchResult, document, successChannel)
    } else {
      // Online is different from mirror, and we have to find out if we had
      // any offline modifications.
      if (offlineChecksum == fetchResult.syncChecksum) {
        successChannel.send(document)
        return
      } else {
        // Files modified both locally and online. Ask user which one wins
        showForkDialog(fetchResult, document, successChannel)
      }
    }
  }

  enum class OpenOnlineDocumentChoice { USE_OFFLINE, USE_ONLINE, CANCEL }

  private fun showOfflineIsAheadDialog(fetchResult: FetchResult, document: Document, successChannel: Channel<Document>) {
    OptionPaneBuilder<OpenOnlineDocumentChoice>().run {
      i18n = RootLocalizer.createWithRootKey(rootKey = "cloud.openWhenOfflineIsAhead")
      styleClass = "dlg-lock"
      styleSheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
      styleSheets.add("/biz/ganttproject/storage/StorageDialog.css")
      graphic = FontAwesomeIconView(FontAwesomeIcon.UNLOCK)
      elements = listOf(
          OptionElementData("useOffline", OpenOnlineDocumentChoice.USE_OFFLINE, true),
          OptionElementData("useOnline", OpenOnlineDocumentChoice.USE_ONLINE),
          OptionElementData("cancel", OpenOnlineDocumentChoice.CANCEL)
      )

      showDialog { choice ->
        when (choice) {
          OpenOnlineDocumentChoice.USE_OFFLINE -> {
            fetchResult.useMirror = true
            GlobalScope.launch {
              successChannel.send(document)
            }
          }
          OpenOnlineDocumentChoice.USE_ONLINE -> {
            GlobalScope.launch {
              successChannel.send(document)
            }
          }
          OpenOnlineDocumentChoice.CANCEL -> {

          }
        }
      }
    }
  }

  private fun showForkDialog(fetchResult: FetchResult, document: Document, successChannel: Channel<Document>) {
    OptionPaneBuilder<OpenOnlineDocumentChoice>().run {
      i18n = RootLocalizer.createWithRootKey(rootKey = "cloud.openWhenDiverged")
      styleClass = "dlg-lock"
      styleSheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
      styleSheets.add("/biz/ganttproject/storage/StorageDialog.css")
      graphic = FontAwesomeIconView(FontAwesomeIcon.UNLOCK)
      elements = listOf(
          OptionElementData("useOffline", OpenOnlineDocumentChoice.USE_OFFLINE, true),
          OptionElementData("useOnline", OpenOnlineDocumentChoice.USE_ONLINE),
          OptionElementData("cancel", OpenOnlineDocumentChoice.CANCEL)
      )

      showDialog { choice ->
        when (choice) {
          OpenOnlineDocumentChoice.USE_OFFLINE -> {
            fetchResult.useMirror = true
            GlobalScope.launch {
              successChannel.send(document)
            }
          }
          OpenOnlineDocumentChoice.USE_ONLINE -> {
            GlobalScope.launch {
              successChannel.send(document)
            }
          }
          OpenOnlineDocumentChoice.CANCEL -> {
          }
        }
      }
    }
  }

  private fun handleDocumentException(ex: Document.DocumentException) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }


  // First we open file "as is", that is, without running any algorithms which
  // change task dates.
  @Throws(Exception::class)
  fun openFileAsIs(document: Document): Step1 {
    myCloseables.add(myEnableAlgorithmsCmd)
    myAlgs.scheduler.setEnabled(false)
    myAlgs.recalculateTaskScheduleAlgorithm.setEnabled(false)
    myAlgs.adjustTaskBoundsAlgorithm.setEnabled(false)
    myAlgs.scheduler.setDiagnostic(myDiagnostics)
    try {
      myProject.open(document)
    } finally {
      myAlgs.scheduler.setDiagnostic(null)
    }
    if (document.portfolio != null) {
      val defaultDocument = document.portfolio.defaultDocument
      myProject.open(defaultDocument)
    }
    myOldDuration = myProject.taskManager.projectLength
    return Step1()
  }

  // This step checks if there are legacy 1-day milestones in the project.
  // If there are legacy milestones, we ask the user what shall we do with them.
  // This involves interaction with Swing thread and later task of patching
  // milestones.
  internal inner class Step1 {
    fun checkLegacyMilestones(): Step2 {
      val taskManager = myProject.taskManager
      var hasLegacyMilestones = false
      for (t in taskManager.tasks) {
        if ((t as TaskImpl).isLegacyMilestone) {
          hasLegacyMilestones = true
          break
        }
      }

      if (hasLegacyMilestones && taskManager.isZeroMilestones == null) {
        val option = if (milestonesOption.selectedValue == null)
          ConvertMilestones.UNKNOWN
        else
          milestonesOption.selectedValue
        when (option) {
          ProjectOpenStrategy.ConvertMilestones.UNKNOWN -> myTasks.add(Runnable {
            try {
              myProject.taskManager.algorithmCollection.scheduler.setDiagnostic(myDiagnostics)
              tryPatchMilestones(myProject, taskManager)
            } finally {
              myProject.taskManager.algorithmCollection.scheduler.setDiagnostic(null)
            }
          })
          ProjectOpenStrategy.ConvertMilestones.TRUE -> {
            taskManager.isZeroMilestones = true
            myResetModifiedState = false
          }
          ProjectOpenStrategy.ConvertMilestones.FALSE -> taskManager.isZeroMilestones = false
          else -> taskManager.isZeroMilestones = false
        }
      }
      return Step2()
    }

    // Asks user what shall we do with milestones and updates milestones if user
    // decides to convert them. This code is executed by Step3.runUiTasks
    private fun tryPatchMilestones(project: IGanttProject, taskManager: TaskManager) {
      val buttonConvert = JRadioButton(i18n.getText("legacyMilestones.choice.convert"))
      val buttonKeep = JRadioButton(i18n.getText("legacyMilestones.choice.keep"))
      buttonConvert.isSelected = true
      val group = JXRadioGroup.create(arrayOf(buttonConvert, buttonKeep))
      group.setLayoutAxis(BoxLayout.PAGE_AXIS)
      val remember = JCheckBox(i18n.getText("legacyMilestones.choice.remember"))

      val content = Box.createVerticalBox()
      val question = JLabel(i18n.getText("legacyMilestones.question"), SwingConstants.LEADING)
      question.isOpaque = true
      question.alignmentX = 0.5f
      content.add(question)
      content.add(Box.createVerticalStrut(15))
      content.add(group)
      content.add(Box.createVerticalStrut(5))
      content.add(remember)

      val icon = Box.createVerticalBox()
      icon.add(JLabel(GPAction.getIcon("64", "dialog-question.png")))
      icon.add(Box.createVerticalGlue())

      val result = JPanel(BorderLayout())
      result.add(content, BorderLayout.CENTER)
      result.add(icon, BorderLayout.WEST)
      result.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
      myUiFacade.createDialog(result, arrayOf<Action>(object : OkAction() {
        override fun actionPerformed(e: ActionEvent) {
          taskManager.isZeroMilestones = buttonConvert.isSelected
          if (remember.isSelected) {
            milestonesOption.selectedValue = if (buttonConvert.isSelected) ConvertMilestones.TRUE else ConvertMilestones.FALSE
          }
          adjustTasks(taskManager)
          project.isModified = true
        }
      }), i18n.getText("legacyMilestones.title")).show()
    }

    private fun adjustTasks(taskManager: TaskManager) {
      try {
        taskManager.algorithmCollection.scheduler.run()
      } catch (e: Exception) {
        GPLogger.logToLogger(e)
      }

    }
  }

  // This step runs the scheduler and checks if there are tasks with earliest start constraints
  // which changed their dates. Such tasks will be reported in the dialog.
  internal inner class Step2 {
    @Throws(Exception::class)
    fun checkEarliestStartConstraints(): Step3 {
      myAlgs.scheduler.setDiagnostic(myDiagnostics)
      // ==== Uncomment to see Scheduler Report in any project
      // myDiagnostics.info("Lorem ipsum dolor sit amet!")
      // myProject.taskManager.tasks.forEach { t -> myDiagnostics.addModifiedTask(t, Date(), Date()) }
      // ====
      try {
        // This actually runs the scheduler by enabling it
        myEnableAlgorithmsCmd.close()
        // We enabled algoritmhs so we don't need to keep them in the list of closeables
        myCloseables.remove(myEnableAlgorithmsCmd)
      } finally {
        myAlgs.scheduler.setDiagnostic(null)
      }
      // Analyze earliest start dates
      for (t in myProject.taskManager.tasks) {
        if (t.third != null && myDiagnostics.myModifiedTasks.containsKey(t)) {
          myDiagnostics.addReason(t, "scheduler.warning.table.reason.earliestStart")
        }
      }

      val newDuration = myProject.taskManager.projectLength
      if (!myDiagnostics.myModifiedTasks.isEmpty()) {
        // Some tasks have been modified, so let's add introduction text to the dialog
        myDiagnostics.info(i18n.getText("scheduler.warning.summary.item0"))
        if (newDuration.length != myOldDuration!!.length) {
          myDiagnostics.info(i18n.formatText("scheduler.warning.summary.item1", myOldDuration, newDuration))
        }
      }
      return Step3()
    }
  }

  // This step runs the collected UI tasks. First (optional) task is legacy milestones question;
  // the remaining are added here.
  internal inner class Step3 {
    fun runUiTasks(): Step4 {
      myTasks.add(Runnable {
        if (!myDiagnostics.myMessages.isEmpty()) {
          myDiagnostics.showDialog()
        }
      })
      if (myDiagnostics.myMessages.isEmpty() && myResetModifiedState) {
        myTasks.add(Runnable { myProject.isModified = false })
      }
      myTasks.add(Runnable {
        try {
          this@ProjectOpenStrategy.close()
        } catch (e: Exception) {
          GPLogger.log(e)
        }
      })
      processTasks(myTasks)
      return Step4()
    }

    private fun processTasks(tasks: MutableList<Runnable>) {
      if (tasks.isEmpty()) {
        return
      }
      val task = tasks.removeAt(0)
      val wrapper = Runnable {
        task.run()
        processTasks(tasks)
      }
      SwingUtilities.invokeLater(wrapper)
    }
  }

  internal inner class Step4 {
    fun onFetchResultChange(document: Document, callback: () -> Unit) {
      val onlineDocument = document.asOnlineDocument()
      if (onlineDocument != null) {
        val changeListener = object : ChangeListener<FetchResult?> {
          override fun changed(observable: ObservableValue<out FetchResult?>?, oldFetch: FetchResult?, newFetch: FetchResult?) {
            println("oldFetch=${oldFetch?.actualChecksum} newFetch=${newFetch?.actualChecksum}")
            if (oldFetch != null && newFetch != null) {
              if (oldFetch.actualVersion != newFetch.actualVersion) {
                observable?.removeListener(this)
                callback()
              }
            }
          }
        }
        onlineDocument.fetchResultProperty.addListener(changeListener)
      }

    }
  }
  companion object {
    val milestonesOption = DefaultEnumerationOption(
        "milestones_to_zero", ConvertMilestones.values())
  }
}

/**
 * This class controls the process of opening a document passed in the command line arguments (or double-clicked in the
 * user interface). It tries to open the document and should it fail, tries to import it from MS Project format.
 */
internal class CommandLineProjectOpenStrategy(
  private val project: IGanttProject,
  private val documentManager: DocumentManager,
  private val taskManager: TaskManagerImpl,
  private val uiFacade: UIFacade,
  private val projectUiFacade: ProjectUIFacade,
  private val preferences: Preferences
) {
  fun openStartupDocument(path: String, hackFireProjectCreated: ()->Unit) {
    val document: Document = documentManager.getDocument(path)
    val finishChannel = Channel<Boolean>()
    GlobalScope.launch { projectUiFacade.openProject(document, project, finishChannel) }
    GlobalScope.launch {
      try {
        finishChannel.receive()
      } catch (e: Document.DocumentException) {
        hackFireProjectCreated() // this will create columns in the tables, which are removed by previous call to openProject()
        if (!tryImportDocument(document)) {
          uiFacade.showErrorDialog(e)
        }
      } catch (e: IOException) {
        hackFireProjectCreated() // this will create columns in the tables, which are removed by previous call to openProject()
        if (!tryImportDocument(document)) {
          uiFacade.showErrorDialog(e)
        }
      }
    }
  }

  private fun tryImportDocument(document: Document): Boolean {
    var success = false
    val importers = PluginManager.getExtensions(
      Importer.EXTENSION_POINT_ID,
      Importer::class.java
    )
    for (importer in importers) {
      if (Pattern.matches(".*(" + importer.fileNamePattern + ")$", document.filePath)) {
        try {
          taskManager.setEventsEnabled(false)
          importer.setContext(project, uiFacade, preferences)
          importer.setFile(File(document.filePath))
          importer.run()
          success = true
          break
        } catch (e: Throwable) {
          if (!GPLogger.log(e)) {
            e.printStackTrace(System.err)
          }
        } finally {
          taskManager.setEventsEnabled(true)
        }
      }
    }
    return success
  }
}
