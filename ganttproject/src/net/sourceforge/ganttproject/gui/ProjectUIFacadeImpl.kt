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

import biz.ganttproject.app.OptionElementData
import biz.ganttproject.app.OptionPaneBuilder
import biz.ganttproject.core.option.GPOptionGroup
import biz.ganttproject.storage.StorageDialogAction
import biz.ganttproject.storage.VersionMismatchException
import biz.ganttproject.storage.asOnlineDocument
import com.google.common.collect.Lists
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.CancelAction
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.Document.DocumentException
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.document.webdav.WebDavStorageImpl
import net.sourceforge.ganttproject.filter.GanttXMLFileFilter
import net.sourceforge.ganttproject.gui.projectwizard.NewProjectWizard
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.undo.GPUndoManager
import org.eclipse.core.runtime.IStatus
import java.awt.event.ActionEvent
import java.io.File
import java.io.IOException
import java.text.MessageFormat
import java.util.*
import java.util.logging.Level
import javax.swing.Action
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

class ProjectUIFacadeImpl(internal val myWorkbenchFacade: UIFacade, private val documentManager: DocumentManager, private val undoManager: GPUndoManager) : ProjectUIFacade {
  private val i18n = GanttLanguage.getInstance()

  private val myConverterGroup = GPOptionGroup("convert", ProjectOpenStrategy.milestonesOption)

  override fun saveProject(project: IGanttProject) {
    if (project.document == null) {
      saveProjectAs(project)
      return
    }
    val document = project.document
    saveProjectTryWrite(project, document)
  }

  private fun saveProjectTryWrite(project: IGanttProject, document: Document): Boolean {
    val canWrite = document.canWrite()
    if (!canWrite.isOK) {
      GPLogger.getLogger(Document::class.java).log(Level.INFO, canWrite.message, canWrite.exception)
      val message = formatWriteStatusMessage(document, canWrite)
      val actions = ArrayList<Action>()
      actions.add(object : GPAction("project.saveas") {
        override fun actionPerformed(e: ActionEvent) {
          saveProjectAs(project)
        }
      })
      if (canWrite.code == Document.ErrorCode.LOST_UPDATE.ordinal) {
        actions.add(object : GPAction("document.overwrite") {
          override fun actionPerformed(e: ActionEvent) {
            saveProjectTryLock(project, document)
          }
        })
      }
      actions.add(CancelAction.EMPTY)
      myWorkbenchFacade.showOptionDialog(JOptionPane.ERROR_MESSAGE, message, actions.toTypedArray())

      return false
    }
    return saveProjectTryLock(project, document)
  }

  private fun saveProjectTryLock(project: IGanttProject, document: Document): Boolean {
    return saveProjectTrySave(project, document)
  }

  enum class VersionMismatchChoice { OVERWRITE, MAKE_COPY }

  private fun saveProjectTrySave(project: IGanttProject, document: Document): Boolean {
    val onlineDoc = document.asOnlineDocument()
    try {
      saveProject(document)
      afterSaveProject(project)
      return true
    } catch (e: VersionMismatchException) {
      if (onlineDoc != null) {
        OptionPaneBuilder<VersionMismatchChoice>().also {
          it.i18n.rootKey = "cloud.versionMismatch"
          it.styleClass = "dlg-lock"
          it.styleSheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
          it.graphic = FontAwesomeIconView(FontAwesomeIcon.CODE_FORK)
          it.elements = Lists.newArrayList(
              OptionElementData("option.overwrite", VersionMismatchChoice.OVERWRITE, false),
              OptionElementData("option.makeCopy", VersionMismatchChoice.MAKE_COPY, true)
          )
          it.showDialog { choice ->
            SwingUtilities.invokeLater {
              when (choice) {
                VersionMismatchChoice.OVERWRITE -> {
                  onlineDoc.write(force = true)
                  afterSaveProject(project)
                }
                VersionMismatchChoice.MAKE_COPY -> {
                  saveProjectAs(project)
                }
              }
            }
            null
          }

        }
      }
      return false
    } catch (e: Throwable) {
      myWorkbenchFacade.showErrorDialog(e)
      return false
    }

  }

  private fun formatWriteStatusMessage(doc: Document, canWrite: IStatus): String {
    assert(canWrite.code >= 0 && canWrite.code < Document.ErrorCode.values().size)
    val errorCode = Document.ErrorCode.values()[canWrite.code]
    val key = "document.error.write." + errorCode.name.toLowerCase()
    return MessageFormat.format(i18n.getText(key), doc.path, canWrite.message)
  }

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

  @Throws(IOException::class)
  private fun saveProject(document: Document) {
    myWorkbenchFacade.setStatusText(GanttLanguage.getInstance().getText("saving") + " " + document.path)
    document.write()
  }

  override fun saveProjectAs(project: IGanttProject) {
    StorageDialogAction(project, myWorkbenchFacade, this, project.documentManager,
        (project.documentManager.webDavStorageUi as WebDavStorageImpl).serversOption).actionPerformed(null)
  }

  /**
   * Check if the project has been modified, before creating or opening another
   * project
   *
   * @return true when the project is **not** modified or is allowed to be
   * discarded
   */
  override fun ensureProjectSaved(project: IGanttProject): Boolean {
    if (project.isModified) {
      val saveChoice = myWorkbenchFacade.showConfirmationDialog(i18n.getText("msg1"),
          i18n.getText("warning"))
      if (UIFacade.Choice.CANCEL == saveChoice) {
        return false
      }
      if (UIFacade.Choice.YES == saveChoice) {
        try {
          saveProject(project)
          // If all those complex save procedures complete successfully and project gets saved
          // then its modified state becomes false
          // Otherwise it remains true which means we have not saved and can't continue
          return !project.isModified
        } catch (e: Exception) {
          myWorkbenchFacade.showErrorDialog(e)
          return false
        }

      }
    }
    return true
  }

  @Throws(IOException::class, DocumentException::class)
  override fun openProject(project: IGanttProject) {
    if (false == ensureProjectSaved(project)) {
      return
    }
    val fc = JFileChooser(documentManager.workingDirectory)
    val ganttFilter = GanttXMLFileFilter()

    // Remove the possibility to use a file filter for all files
    val filefilters = fc.choosableFileFilters
    for (i in filefilters.indices) {
      fc.removeChoosableFileFilter(filefilters[i])
    }
    fc.addChoosableFileFilter(ganttFilter)

    val returnVal = fc.showOpenDialog(myWorkbenchFacade.mainFrame)
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      val document = documentManager.getDocument(fc.selectedFile.absolutePath)
      openProject(document, project)
    }
  }

  @Throws(IOException::class, DocumentException::class)
  override fun openProject(document: Document, project: IGanttProject) {
    beforeClose()
    project.close()

    try {
      ProjectOpenStrategy(project, myWorkbenchFacade).use { strategy ->
        val offlineTail = { doc: Document ->
          SwingUtilities.invokeLater {
            strategy.openFileAsIs(doc)
                .checkLegacyMilestones()
                .checkEarliestStartConstraints()
                .runUiTasks()
          }
        }
        strategy.open(document, offlineTail)

//        strategy.fetchOnlineDocument(document)
//            .thenCompose {
//              if (it == null) { CompletableFuture.completedFuture(document) } else { processFetchResult(it) }
//            }
//            .thenApply {
//              offlineTail(it ?: document)
//            }
//            .exceptionally {
//              when (it) {
//                is DocumentException -> handleDocumentException(it)
//                else -> {
//                  myWorkbenchFacade.showErrorDialog(it)
//                }
//              }
//            }
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
    if (false == ensureProjectSaved(project)) {
      return
    }
    beforeClose()
    project.close()
    myWorkbenchFacade.setStatusText(i18n.getText("project.new.description"))
    showNewProjectWizard(project)
  }

  private fun showNewProjectWizard(project: IGanttProject) {
    val wizard = NewProjectWizard()
    wizard.createNewProject(project, myWorkbenchFacade)
  }

  override fun getOptionGroups(): Array<GPOptionGroup> {
    return arrayOf(myConverterGroup)
  }
}
