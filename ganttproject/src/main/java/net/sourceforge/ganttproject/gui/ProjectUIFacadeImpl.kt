/*
Copyright 2019 BarD Software s.r.o
Copyright 2005-2018 GanttProject team

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
package net.sourceforge.ganttproject.gui

import biz.ganttproject.app.*
import biz.ganttproject.core.calendar.ImportCalendarOption
import biz.ganttproject.core.option.GPOptionGroup
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.storage.*
import biz.ganttproject.storage.cloud.*
import com.google.common.collect.Lists
import com.sandec.mdfx.MDFXNode
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.Document.DocumentException
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.document.ProxyDocument
import net.sourceforge.ganttproject.document.webdav.WebDavStorageImpl
import net.sourceforge.ganttproject.gui.projectwizard.NewProjectWizard
import net.sourceforge.ganttproject.importer.BufferProject
import net.sourceforge.ganttproject.importer.asImportBufferProjectApi
import net.sourceforge.ganttproject.importer.importBufferProject
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.resource.HumanResourceMerger
import net.sourceforge.ganttproject.resource.HumanResourceMerger.MergeResourcesOption.BY_ID
import net.sourceforge.ganttproject.task.event.*
import net.sourceforge.ganttproject.task.export
import net.sourceforge.ganttproject.task.importFromDatabase
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.logging.Level
import javax.swing.SwingUtilities


@ExperimentalCoroutinesApi
class ProjectUIFacadeImpl(
    private val myWorkbenchFacade: UIFacade,
    private val documentManager: DocumentManager,
    private val undoManager: GPUndoManager) : ProjectUIFacade {
  private val i18n = GanttLanguage.getInstance()

  private val myConverterGroup = GPOptionGroup("convert", ProjectOpenStrategy.milestonesOption)
  private var isSaving = false

  override fun saveProject(project: IGanttProject, onFinish: Channel<Boolean>?) {
    if (isSaving) {
      GPLogger.logToLogger("We're saving the project now. This save request was rejected")
    }
    isSaving = true
    try {
      val broadcastWaitScope = CoroutineScope(Executors.newFixedThreadPool(2).asCoroutineDispatcher())
      val broadcastChannel = BroadcastChannel<Boolean>(1)
      broadcastChannel.openSubscription().let { channel ->
        broadcastWaitScope.launch {
          if (channel.receive()) {
            afterSaveProject(project)
            channel.cancel()
          }
        }
      }
      onFinish?.let {
        broadcastChannel.openSubscription().let { channel -> broadcastWaitScope.launch {
          it.send(channel.receive())
          channel.cancel()
        }}
      }
      ProjectSaveFlow(project = project, onFinish = broadcastChannel,
        signin = this::signin,
        error = this::onError,
        saveAs = { saveProjectAs(project) }
      ).run()
    } finally {
      isSaving = false
    }
  }

  fun onError(ex: Exception) {
    dialog {
      it.addStyleSheet("/biz/ganttproject/app/dialogs.css")
      it.addStyleClass("dialog-alert")
      it.setHeader(
        VBoxBuilder("header").apply {
          addTitle(RootLocalizer.create("error.channel.itemTitle")).also { hbox ->
            hbox.alignment = Pos.CENTER_LEFT
            hbox.isFillHeight = true
          }
        }.vbox
      )
      it.setContent(
        //createAlertBody(ex.message ?: ""),
        MDFXNode(ex.message ?: "").also { it.styleClass.add("content-pane") }
      )
      it.removeButtonBar()
    }
  }

  override fun saveProjectAs(project: IGanttProject) {
    StorageDialogAction(
      project, this, project.documentManager,
      (project.documentManager.webDavStorageUi as WebDavStorageImpl).serversOption,
      StorageDialogBuilder.Mode.SAVE,
      "project.save"
    ).doRun()
  }

  enum class CantWriteChoice {MAKE_COPY, CANCEL, RETRY}

  private fun signin(onAuth: ()->Unit) {
    dialog { controller ->
      val wrapper = BorderPane()
      controller.addStyleClass("dlg-lock", "dlg-cloud-file-options")
      controller.addStyleSheet(
        "/biz/ganttproject/storage/cloud/GPCloudStorage.css",
        "/biz/ganttproject/storage/StorageDialog.css"
      )
      wrapper.center = Pane().also {
        it.prefHeight = 400.0
        it.prefWidth = 400.0
      }
      controller.setContent(wrapper)
      GPCloudUiFlowBuilder().apply {
        flowPageChanger = createFlowPageChanger(wrapper, controller)
        mainPage = object : EmptyFlowPage() {
          override var active: Boolean
            get() = super.active
            set(value) {
              if (value) {
                controller.hide()
                onAuth()
              }
            }
        }
        build().start()
      }
    }
  }
//  private fun formatWriteStatusMessage(doc: Document, canWrite: IStatus): String {
//    assert(canWrite.code >= 0 && canWrite.code < Document.ErrorCode.values().size)
//    return RootLocalizer.formatText(
//        key = "document.error.write.${Document.ErrorCode.values()[canWrite.code].name.toLowerCase()}",
//        doc.fileName, canWrite.message)
//  }

  private fun afterSaveProject(project: IGanttProject) {
    val document = project.document
    documentManager.addToRecentDocuments(document)
    val title = i18n.getText("appliTitle") + " [" + document.fileName + "]"
    myWorkbenchFacade.setWorkbenchTitle(title)
    if (document.isLocal) {
      val url = document.uri
      if (url != null) {
        val file = File(url)
        documentManager.changeWorkingDirectory(file.parentFile)
      }
    }
    project.isModified = false
  }

  /**
   * Check if the project has been modified, before creating or opening another
   * project
   *
   * @return true when the project is **not** modified or is allowed to be
   * discarded
   */
  override fun ensureProjectSaved(project: IGanttProject): Barrier<Boolean> =
    if (project.isModified) {
      val saveChoice = myWorkbenchFacade.showConfirmationDialog(i18n.getText("msg1"),
          i18n.getText("warning"))
      when (saveChoice) {
        UIFacade.Choice.CANCEL, null -> ResolvedBarrier(false)
        UIFacade.Choice.NO -> ResolvedBarrier(true)
        UIFacade.Choice.YES, UIFacade.Choice.OK -> {
          SimpleBarrier<Boolean>().also { barrier ->
            val onFinish = Channel<Boolean>()
            CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).launch {
              try {
                if (onFinish.receive()) {
                  barrier.resolve(true)
                }
              } catch (e: Exception) {
                myWorkbenchFacade.showErrorDialog(e)
              }
            }
            saveProject(project, onFinish)
          }
        }
      }
    } else {
      ResolvedBarrier(true)
    }


  @Throws(IOException::class, DocumentException::class)
  override fun openProject(document: Document, project: IGanttProject, onFinish: Channel<Boolean>?,
                           authenticationFlow: AuthenticationFlow?) {
    try {
      ProjectOpenStrategy(
        project = project,
        uiFacade = myWorkbenchFacade,
        signin = authenticationFlow ?: this::signin,
      ).use { strategy ->
        DOCUMENT_LOGGER.debug(">>> openProject({})", document.uri)
        // Run coroutine which fetches document and wait until it sends the result to the channel.
        val docChannel = Channel<Document>()
        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).launch {
          try {
            DOCUMENT_LOGGER.debug("... waiting for the document")
            docChannel.receive().also { doc ->
              DOCUMENT_LOGGER.debug("... document is ready")
              // If document is obtained, we need to run further steps.
              // Because of historical reasons they run in Swing thread (they may modify the state of Swing components)
              SwingUtilities.invokeLater {
                try {
                  beforeClose()
                  project.close()
                  strategy.openFileAsIs(doc)
                    .checkLegacyMilestones()
                    .checkEarliestStartConstraints()
                    .runUiTasks()
                  document.asOnlineDocument()?.let {
                    if (it is GPCloudDocument) {
                      it.colloboqueClient = ColloboqueClient(project.projectDatabase, undoManager)
                      project.projectDatabase.addExternalUpdatesListener {
                        Platform.runLater {
                          val emptyTaskManager = project.taskManager.emptyClone()
                          emptyTaskManager.importFromDatabase(project.projectDatabase.readAllTasks(), project.taskManager.taskHierarchy.export())
                          val bufferProject = BufferProject(
                            emptyTaskManager,
                            project.projectDatabase,
                            project.roleManager,
                            project.activeCalendar,
                            myWorkbenchFacade
                          )
                          val mergeOption = HumanResourceMerger.MergeResourcesOption()
                          val importCalendarOption = ImportCalendarOption()
                          mergeOption.loadPersistentValue(BY_ID)
                          importCalendarOption.loadPersistentValue(ImportCalendarOption.Values.REPLACE.name)
                          importBufferProject(
                            project,
                            bufferProject,
                            myWorkbenchFacade.asImportBufferProjectApi(),
                            mergeOption,
                            importCalendarOption,
                            closeCurrentProject = true
                          )
                        }
                      }
                      it.onboard(documentManager, webSocket)
                    }
                  }
                  runBlocking { onFinish?.send(true) }
                } catch (ex: DocumentException) {
                  onFinish?.close(ex) ?: DOCUMENT_ERROR_LOGGER.error("", ex)
                }
              }
            }
          } catch (ex: Exception) {
            when (ex) {
              // If channel was closed with a cause and it was because of HTTP 403, we show UI for sign-in
              is DocumentException -> {
                onFinish?.close(ex) ?: DOCUMENT_ERROR_LOGGER.error("", ex)
              }
              else -> {
                onFinish?.close(ex) ?: DOCUMENT_ERROR_LOGGER.error("Can't open document $document", ex)
              }
            }
          }
          finally {
            DOCUMENT_LOGGER.debug("<<< openProject()")
          }
        }
        strategy.open(document, docChannel)
      }
    } catch (e: Exception) {
      throw DocumentException("Can't open document $document", e)
    }
  }

  private fun beforeClose() {
    myWorkbenchFacade.setWorkbenchTitle(i18n.getText("appliTitle"))
    undoManager.die()
  }

  override fun createProject(project: IGanttProject) {
    ensureProjectSaved(project).await { result ->
      if (result) {
        beforeClose()
        project.close()
        myWorkbenchFacade.setStatusText(i18n.getText("project.new.description"))
        showNewProjectWizard(project)
      }
    }
  }

  private fun showNewProjectWizard(project: IGanttProject) {
    val wizard = NewProjectWizard()
    wizard.createNewProject(project, myWorkbenchFacade)
  }

  override fun getOptionGroups(): Array<GPOptionGroup> {
    return arrayOf(myConverterGroup)
  }
}

@ExperimentalCoroutinesApi
class ProjectSaveFlow(
    private val project: IGanttProject,
    private val onFinish: BroadcastChannel<Boolean>,
    private val signin: (()->Unit) -> Unit,
    private val error: (Exception) -> Unit,
    private val saveAs: () -> Unit) {

  private val doneScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
  private fun done(success: Boolean) {
    doneScope.launch {
      onFinish.send(success)
      onFinish.close()
    }
  }

  fun run() {
    try {
      project.document?.let {
        if (it.asLocalDocument()?.canRead() == false) {
          saveProjectAs(project)
        } else {
          if (it is ProxyDocument) {
            it.createContents()
          }
          saveProjectTryWrite(project, it)
        }
      } ?: run {
        saveProjectAs(project)
      }
    } catch (ex: Exception) {
      error(ex)
      done(success = false)
    }
  }


  private fun saveProjectTryWrite(project: IGanttProject, document: Document) {
    val canWrite = document.canWrite()
    if (!canWrite.isOK) {
      GPLogger.getLogger(Document::class.java).log(Level.INFO, canWrite.message, canWrite.exception)
      OptionPaneBuilder<ProjectUIFacadeImpl.CantWriteChoice>().also {
        it.i18n = RootLocalizer.createWithRootKey(
          rootKey = "document.error.write.cantWrite",
          baseLocalizer = RootLocalizer
        )
        it.styleClass = "dlg-lock"
        it.styleSheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
        it.styleSheets.add("/biz/ganttproject/storage/StorageDialog.css")
        it.titleString.update(document.fileName)
        it.titleHelpString?.update(canWrite.message)
        it.graphic = FontAwesomeIconView(FontAwesomeIcon.LOCK, "64").also { icon ->
          icon.styleClass.add("img")
        }
        it.elements = listOf(
          OptionElementData("document.option.makeCopy", ProjectUIFacadeImpl.CantWriteChoice.MAKE_COPY, true),
          OptionElementData("cancel", ProjectUIFacadeImpl.CantWriteChoice.CANCEL, false),
          OptionElementData("generic.retry", ProjectUIFacadeImpl.CantWriteChoice.RETRY, false),
        )
        it.showDialog { choice ->
          SwingUtilities.invokeLater {
            when (choice) {
              ProjectUIFacadeImpl.CantWriteChoice.MAKE_COPY -> {
                saveProjectAs(project)
              }
              ProjectUIFacadeImpl.CantWriteChoice.RETRY -> {
                saveProjectTryWrite(project, document)
              }
              else -> {
                done(success = false)
              }
            }
          }
        }
      }
    } else {
      saveProjectTryLock(project, document)
    }
  }

  private fun saveProjectTryLock(project: IGanttProject, document: Document) {
    saveProjectTrySave(project, document)
  }

  enum class VersionMismatchChoice { OVERWRITE, MAKE_COPY }

  private fun saveProjectTrySave(project: IGanttProject, document: Document) {
    try {
      saveProject(document)
    } catch (e: VersionMismatchException) {
      done(success = false)
      val onlineDoc = document.asOnlineDocument()
      if (onlineDoc != null) {
        OptionPaneBuilder<VersionMismatchChoice>().also {
          it.i18n = RootLocalizer.createWithRootKey(rootKey = "cloud.versionMismatch", baseLocalizer = RootLocalizer)
          it.styleClass = "dlg-lock"
          it.styleSheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
          it.styleSheets.add("/biz/ganttproject/storage/StorageDialog.css")
          it.graphic = FontAwesomeIconView(FontAwesomeIcon.CODE_FORK, "64").also {icon ->
            icon.styleClass.add("img")
          }
          it.elements = Lists.newArrayList(
            OptionElementData("document.option.makeCopy", VersionMismatchChoice.MAKE_COPY, true)
          ).also { list ->
            if (e.canOverwrite) {
              list.add(OptionElementData("option.overwrite", VersionMismatchChoice.OVERWRITE, false))
            }
          }
          it.showDialog { choice ->
            SwingUtilities.invokeLater {
              when (choice) {
                VersionMismatchChoice.OVERWRITE -> {
                  onlineDoc.write(force = true)
                }
                VersionMismatchChoice.MAKE_COPY -> {
                  saveProjectAs(project)
                }
              }
            }
          }

        }
      }
    } catch (e: ForbiddenException) {
      signin {
        saveProjectTrySave(project, document)
      }
    } catch (e: PaymentRequiredException) {
      done(success = false)
      error(e)
    }
  }

  @Throws(IOException::class)
  private fun saveProject(document: Document) {
    //myWorkbenchFacade.setStatusText(GanttLanguage.getInstance().getText("saving") + " " + document.path)
    document.write()
    done(success = true)
  }

  private fun saveProjectAs(project: IGanttProject) {
    done(success = false)
    saveAs()
  }
}

private val DOCUMENT_LOGGER = GPLogger.create("Document.Info")
private val DOCUMENT_ERROR_LOGGER = GPLogger.create("Document.Error")
