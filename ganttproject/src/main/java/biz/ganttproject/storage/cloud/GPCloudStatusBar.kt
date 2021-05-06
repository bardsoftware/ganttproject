/*
Copyright 2018-2020 BarD Software s.r.o

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
package biz.ganttproject.storage.cloud

import biz.ganttproject.app.OptionElementData
import biz.ganttproject.app.OptionPaneBuilder
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.dialog
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.lib.fx.createToggleSwitch
import biz.ganttproject.storage.*
import biz.ganttproject.storage.cloud.http.tryAccessToken
import com.evanlennick.retry4j.AsyncCallExecutor
import com.evanlennick.retry4j.CallExecutorBuilder
import com.evanlennick.retry4j.config.RetryConfigBuilder
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableObjectValue
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.shape.Circle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.OkAction
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.ProxyDocument
import net.sourceforge.ganttproject.gui.ProjectUIFacade
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.language.GanttLanguage
import org.controlsfx.control.decoration.Decorator
import org.controlsfx.control.decoration.GraphicDecoration
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JOptionPane
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * This status bar appears in the bottom-left corner of the app window and shows
 * document lock status and access mode. When clicked, it opens a dialog for changing
 * lock status and access mode.
 *
 * @author dbarashev@bardsoftware.com
 */
class GPCloudStatusBar(
  private val observableDocument: ObservableObjectValue<Document>,
  private val uiFacade: UIFacade,
  private val projectUIFacade: ProjectUIFacade,
  private val project: IGanttProject
) {
  private var onLatestVersionChange: ChangeListener<LatestVersion>? = null
  private var cloudConnected: Boolean = false
    set(value) {
      field = value
      Platform.runLater {
        toggleConnect.isSelected = value
        labelConnect.text = STATUS_BAR_LOCALIZER.formatText("connected.${value.toString().toLowerCase()}")
        GPCloudOptions.cloudStatus.value = if (value) CloudStatus.CONNECTED else CloudStatus.DISCONNECTED
      }
    }
  private val toggleConnect = createToggleSwitch()
  private val labelConnect = Label()

  private val btnLock = Button().also {
    it.isVisible = false
  }
  private val btnOffline = Button().also {
    it.isVisible = false
  }
  private val reconnectLabel = Label()
  private val reconnectStatus = ReconnectStatus(reconnectLabel)
  val lockPanel = HBox().also {
    it.styleClass.add("statusbar")
    it.children.addAll(toggleConnect, labelConnect, btnOffline, btnLock, reconnectLabel)
  }

  private val modeChangeListener = ChangeListener<OnlineDocumentMode> {
    _, _, newValue -> this.onOnlineModeChange(newValue)
  }
  private val lockChangeListener = ChangeListener<LockStatus> {
    _, _, newValue -> this.onLockStatusChange(newValue)
  }
  private lateinit var status: LockStatus


  init {
    observableDocument.addListener { _, oldDocument: Document?, newDocument: Document? ->
      onDocumentChange(oldDocument, newDocument)
    }
    toggleConnect.selectedProperty().addListener { _, _, newValue ->
      if (newValue && !this.cloudConnected) {
        showConnect()
      } else if (!newValue && this.cloudConnected){
        GPCloudOptions.disconnect()
      }
    }
    btnOffline.addEventHandler(ActionEvent.ACTION) {
      showProperties()
    }
    btnLock.addEventHandler(ActionEvent.ACTION) {
      showProperties()
    }
    fun applyCloudStatus(status: CloudStatus) =
      when (status) {
        CloudStatus.UNKNOWN -> {
          tryAccessToken(onSuccess = { this.cloudConnected = true }, onError = { this.cloudConnected = false })
        }
        CloudStatus.DISCONNECTED -> this.cloudConnected = false
        CloudStatus.CONNECTED -> this.cloudConnected = true
      }
    GPCloudOptions.cloudStatus.apply {
      addListener { _, _, newValue ->
        applyCloudStatus(newValue)
      }
      applyCloudStatus(value)
    }
  }

  private fun showConnect() {
    dialog { controller ->
      controller.addStyleClass("dlg-connect")
      controller.addStyleSheet(
        "/biz/ganttproject/app/StatusBar.css"
      )
      val wrapper = BorderPane()
      controller.setContent(wrapper)
      GPCloudUiFlowBuilder().apply {
        wrapperPane = wrapper
        dialog = controller
        mainPage = object : EmptyFlowPage() {
          override var active: Boolean
            get() = super.active
            set(value) {
              if (value) controller.hide()
            }
        }
        build().start()
      }
    }
  }

  private fun showProperties() {
    this.observableDocument.get().apply {
      val onlineDocument = this.asOnlineDocument()
      if (onlineDocument is GPCloudDocument) {
        DocPropertiesUi(errorUi = {}, busyUi = {}).showDialog(onlineDocument) {
          projectUIFacade.openProject(this, this@GPCloudStatusBar.project, null)
        }
      }
    }
  }

  // This is called whenever open document changes and handles different cases.
  private fun onDocumentChange(oldDocument: Document?, newDocument: Document?) {
    GlobalScope.launch(Dispatchers.JavaFx) {

      // First we un-proxy old and new documents.
      val newDoc = if (newDocument is ProxyDocument) {
        newDocument.realDocument
      } else {
        newDocument
      }
      val oldDoc = if (oldDocument is ProxyDocument) {
        oldDocument.realDocument
      } else {
        oldDocument
      }

      // Then we remove listeners from the old document
      if (oldDoc is LockableDocument) {
        oldDoc.status.removeListener(lockChangeListener)
      }
      if (oldDoc is OnlineDocument) {
        oldDoc.mode.removeListener(modeChangeListener)
        onLatestVersionChange?.let { oldDoc.latestVersionProperty.removeListener(it) }
        onLatestVersionChange = null
      }

      // If we had a reconnect ping, we'll stop it.
      reconnectStatus.stopReconnectPing()
      // If new document is lockable, we'll add listeners and show the icon.
      if (newDoc is LockableDocument) {
        newDoc.status.addListener(lockChangeListener)
        btnLock.isVisible = true
        newDoc.reloadLockStatus().handle { lockStatus, ex -> if (ex != null) GPLogger.log(ex) else {
          updateLockStatus(lockStatus)
        }}
      } else {
        btnLock.isVisible = false
      }

      // If new document is online, we'll add some listeners too.
      if (newDoc is OnlineDocument) {
        // Listen to online mode changes: online only/mirrored/offline only
        newDoc.mode.addListener(modeChangeListener)
        btnOffline.isVisible = true
        updateOnlineMode(newDoc.mode.value)

        // Listen to the version updates
        onLatestVersionChange = ChangeListener { _, _, newValue ->
          handleLatestVersionChange(newDocument!!, newDoc, newValue)
        }
        newDoc.latestVersionProperty.addListener(onLatestVersionChange)
      } else {
        btnOffline.isVisible = false
      }
    }
  }

  private fun onLockStatusChange(newStatus: LockStatus) {
    GlobalScope.launch(Dispatchers.JavaFx) {
      updateLockStatus(newStatus)
    }
  }

  private fun updateLockStatus(status: LockStatus) {
    if (status.locked) {
      this.btnLock.graphic = FontAwesomeIconView(FontAwesomeIcon.LOCK)
      val lockOwner = STATUS_BAR_LOCALIZER.formatText("lockedBy", status.lockOwnerName ?: "")
      this.btnLock.text =
          if (GPCloudOptions.userId.value?.equals(status.lockOwnerId) == true) STATUS_BAR_LOCALIZER.formatText("locked")
          else lockOwner
      this.btnLock.tooltip = Tooltip(lockOwner)
    } else {
      this.btnLock.graphic = FontAwesomeIconView(FontAwesomeIcon.UNLOCK)
      this.btnLock.text = STATUS_BAR_LOCALIZER.formatText("notLocked")
      this.btnLock.tooltip = Tooltip(STATUS_BAR_LOCALIZER.formatText("notLocked.tooltip"))
    }

    this.status = status
  }

  private fun onOnlineModeChange(newValue: OnlineDocumentMode) {
    GlobalScope.launch(Dispatchers.JavaFx) {
      updateOnlineMode(newValue)
    }
  }

  private fun updateOnlineMode(mode: OnlineDocumentMode) {
    when (mode) {
      OnlineDocumentMode.ONLINE_ONLY -> {
        this.btnOffline.run {
          text = STATUS_BAR_LOCALIZER.formatText("mode.onlineOnly")
          graphic = FontAwesomeIconView(FontAwesomeIcon.CLOUD)
          tooltip = Tooltip(STATUS_BAR_LOCALIZER.formatText("mode.onlineOnly.tooltip"))
          Decorator.removeAllDecorations(this)
          isDisable = false
        }
        this.btnLock.isDisable = false
      }
      OnlineDocumentMode.MIRROR -> {
        this.btnOffline.run {
          text = STATUS_BAR_LOCALIZER.formatText("mode.mirror")
          graphic = FontAwesomeIconView(FontAwesomeIcon.CLOUD_DOWNLOAD)
          tooltip = Tooltip(STATUS_BAR_LOCALIZER.formatText("mode.mirror.tooltip"))
          Decorator.removeAllDecorations(this)
          isDisable = false
        }
        this.btnLock.isDisable = false
      }
      OnlineDocumentMode.OFFLINE_ONLY -> {
        this.btnOffline.run {
          text = STATUS_BAR_LOCALIZER.formatText("mode.offline")
          graphic = FontAwesomeIconView(FontAwesomeIcon.CLOUD_DOWNLOAD)
          tooltip = Tooltip(STATUS_BAR_LOCALIZER.formatText("mode.offline.tooltip"))
          Decorator.addDecoration(this, GraphicDecoration(createWarningDecoration(), Pos.BOTTOM_LEFT, 6.0, -4.0))
          isDisable = true
        }

        observableDocument.value.asOnlineDocument()?.let {
          if (it is GPCloudDocument) {
            reconnectStatus.startReconnectPing(it)
          }
        }
        this.uiFacade.showOptionDialog(
            JOptionPane.WARNING_MESSAGE,
            STATUS_BAR_LOCALIZER.formatText("mode.offline.warning"),
            arrayOf(OkAction.createVoidAction("ok"))
        )
        this.btnLock.isDisable = true
      }
    }
  }

  // This is called when cloud document changes and we receive an update notification.
  // We want to show a dialog asking to reload document or ignore the update.
  private fun handleLatestVersionChange(newDocument: Document, doc: OnlineDocument, newValue: LatestVersion) {
    OptionPaneBuilder<Boolean>().run {
      i18n = RootLocalizer.createWithRootKey("cloud.loadLatestVersion")
      graphic = FontAwesomeIconView(FontAwesomeIcon.REFRESH)
      elements = listOf(
          OptionElementData("reload", userData = true,  isSelected = true),
          OptionElementData("ignore", userData = false)
      )
      titleHelpString?.update(newValue.author, GanttLanguage.getInstance().formatDate(GanttCalendar.getInstance().apply {
        time = Date(newValue.timestamp)
      }))

      showDialog { choice ->
        if (choice) {
          GlobalScope.launch(Dispatchers.IO) {
            doc.fetch().also {
              it.update()
              projectUIFacade.openProject(newDocument, this@GPCloudStatusBar.project, null)
            }
          }
        }
      }
    }
  }
}

private fun createWarningDecoration(): Node {
  return Circle(4.0).also {
    it.styleClass.add("decoration-warning")
    it.strokeWidth = 2.0
  }
}

/**
 * Schedules reconnect pings with some backoff and updates the status bar with the information
 * about the next reconnect attempt.
 */
private class ReconnectStatus(private val label: Label) {
  private var statusUpdateFuture: ScheduledFuture<*>? = null
  private val statusUpdateExecutor = Executors.newSingleThreadScheduledExecutor()
  private var reconnectExecutor: AsyncCallExecutor<Boolean>? = null
  private val reconnectText = STATUS_BAR_LOCALIZER.create("reconnect")

  init {
    label.isVisible = false
    label.textProperty().bind(reconnectText)
  }

  fun startReconnectPing(document: GPCloudDocument) {
    LOG.debug("Starting reconnect ping.")
    val retryConfig = RetryConfigBuilder()
        .retryOnReturnValue(false)
        .retryOnAnyException()
        .withMaxNumberOfTries(1000)
        .withBackoffStrategy { numberOfTriesFailed, delayBetweenAttempts ->
          // We will exponentially increase duration until it reaches 128 seconds, and after that
          // we will send ping every ~2 min
          val result = if (numberOfTriesFailed < 7) {
            delayBetweenAttempts.multipliedBy(2.0.pow(numberOfTriesFailed.toDouble()).roundToLong())
          } else {
            Duration.ofSeconds(120L + Random.nextInt(-20, 20))
            // This is the only place in retry4j where we know the duration until the next try.
          }
          result.also(this::startCountdown)
        }
        .withDelayBetweenTries(1, ChronoUnit.SECONDS)
        .build()
    CallExecutorBuilder<Boolean>().config(retryConfig)
        .onSuccessListener {
          document.modeValue = OnlineDocumentMode.MIRROR
          stopReconnectPing()
        }
        .buildAsync(Executors.newSingleThreadExecutor())
        .also {
          it.execute { isNetworkAvailable() }
          reconnectExecutor = it as AsyncCallExecutor<Boolean>
        }
  }

  private fun startCountdown(nextTry: Duration) {
    LOG.debug("The next ping is in {}. Starting countdown", nextTry)
    GlobalScope.launch(Dispatchers.JavaFx) { this@ReconnectStatus.label.isVisible = true }
    val remainingSeconds = AtomicLong(nextTry.seconds)
    this.statusUpdateFuture = statusUpdateExecutor.scheduleWithFixedDelay({
      LOG.debug("... {} seconds", remainingSeconds.get())
      if (remainingSeconds.get() > 0) {
        GlobalScope.launch(Dispatchers.JavaFx) { reconnectText.update(remainingSeconds.getAndDecrement().toString()) }
      } else {
        throw RuntimeException("Cancelling this status update")
      }
    }, 0, 1, TimeUnit.SECONDS)
  }

  fun stopReconnectPing() {
    LOG.debug("Cancelling reconnect ping.")
    reconnectExecutor?.executorService?.shutdown()
    reconnectExecutor = null
    cancelStatusUpdate()
  }

  private fun cancelStatusUpdate() {
    statusUpdateFuture?.cancel(true)
    label.isVisible = false
  }
}
val STATUS_BAR_LOCALIZER = RootLocalizer.createWithRootKey("cloud.statusBar")
private val LOG = GPLogger.create("Cloud.StatusBar")
