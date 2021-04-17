package biz.ganttproject.storage.cloud

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.storage.*
import javafx.event.ActionEvent
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.document.Document
import java.util.*

/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudOfflineBrowser(val mode: StorageDialogBuilder.Mode,
                            private val dialogUi: StorageDialogBuilder.DialogUi,
                            private val documentConsumer: (Document) -> Unit) : FlowPage() {
  override fun createUi() = createBrowserPane()

  override fun resetUi() {
  }

  override fun setController(controller: GPCloudUiFlow) {
  }

  private fun createBrowserPane(): Pane {
    val builder = BrowserPaneBuilder<OfflineMirrorOptionsAsFolderItem>(this.mode, this.dialogUi::error) { _, success, _ ->
      loadOfflineMirrors(success)
    }

    val paneElements = builder.apply {
      withI18N(ourLocalizer)
      withBreadcrumbs(DocumentUri(listOf(), true, ourLocalizer.formatText("breadcrumbs.root")))
      withActionButton {}
      withListView(
        onSelectionChange = actionButtonHandler::onSelectionChange,
        itemActionFactory = { Collections.emptyMap() },
        cellFactory = { CellWithBasePath() }
      )
      withActionButton { btn ->
        btn.addEventHandler(ActionEvent.ACTION) {
          actionButtonHandler.onAction()
        }
      }

    }.build()
    paneElements.breadcrumbView?.show()
    paneElements.browserPane.stylesheets.addAll(
      "/biz/ganttproject/storage/cloud/GPCloudStorage.css",
      "/biz/ganttproject/storage/FolderViewCells.css"
    )
    return paneElements.browserPane
  }

  private val actionButtonHandler = object {
    private var selectedProject: FolderItem? = null

    fun onSelectionChange(item: FolderItem) {
      this.selectedProject = item
    }

    fun onAction() {
      selectedProject?.let {
        if (it is OfflineMirrorOptionsAsFolderItem) {
          it.options.offlineMirror?.let { path ->
            documentConsumer(GPCloudDocument(
              teamRefid = null,
              teamName = it.options.teamName,
              projectRefid = it.options.projectRefid,
              projectName = it.name,
              projectJson = null
            ))
          }
        }
      }
    }
  }
}

private val ourLocalizer = RootLocalizer.createWithRootKey("storageService.cloudOffline", BROWSE_PANE_LOCALIZER)
