/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.app

import biz.ganttproject.FXUtil
import biz.ganttproject.findDescendant
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.notifications.Notifications
import biz.ganttproject.lib.fx.vbox
import com.sandec.mdfx.MarkdownView
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import com.vladsch.flexmark.util.data.MutableDataSet
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.stage.Popup
import javafx.stage.Stage
import javafx.stage.Window
import javafx.util.Callback
import javafx.util.Duration
import net.sourceforge.ganttproject.gui.NotificationChannel
import net.sourceforge.ganttproject.gui.NotificationItem
import net.sourceforge.ganttproject.gui.NotificationManager
import net.sourceforge.ganttproject.gui.ViewLogDialog
import org.apache.commons.text.StringEscapeUtils
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.SwingUtilities
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

/**
 * Implements notifications as popup bubbles in the bottom-right corner of the application window.
 * Besides, adds notification indicators to the status bar and shows a more detailed notification dialog on clicking
 * the buttons.
 */
class NotificationManagerImpl : NotificationManager {
  private lateinit var owner: Stage

  private val notifications = mutableListOf<NotificationItem>()
  private val hasRss = SimpleBooleanProperty(false)
  private val hasError = SimpleIntegerProperty(0)

  fun setOwner(stage: Stage) { owner = stage }

  init {
    // Notifications API does not provide any way to listen on the popup events, so we use a workaround:
    // listen to all windows and find the one that is a notification popup.
    Window.getWindows().addListener { c: ListChangeListener.Change<out Window?> ->
      c.list.firstOrNull { it?.isNotificationPopup() ?: false }?.let {
        it.onHidden = EventHandler { evt ->
          val notificationItem = it.scene.root.findDescendant {
            it.userData is NotificationItem
          }?.userData as? NotificationItem
          if (notificationItem != null) {
            notificationItem.wasShown = true
          }
        }

      }
    }
  }

  override fun createNotification(channel: NotificationChannel, title: String, body: String, hyperlinkListener: HyperlinkListener?) =
    NotificationItem(channel, title, body, LocalDateTime.now(), hyperlinkListener ?: NotificationManager.DEFAULT_HYPERLINK_LISTENER)

  override fun addNotifications(notifications: List<NotificationItem>) {
    if (notifications.isEmpty()) {
      return
    }
    this.notifications.addAll(notifications.map {
      val body = if (it.isHtml()) {
        html2md(it.myBody)
      } else {
        it.myBody
      }
      when (it.channel) {
        NotificationChannel.ERROR -> hasError.set(2)
        NotificationChannel.WARNING -> if (hasError.get() != 2) hasError.set(1)
        NotificationChannel.RSS -> hasRss.set(true)
      }
      NotificationItem(it.channel, it.myTitle, body, it.timestamp, it.myHyperlinkListener)
    })
    updateUi()
  }

  override fun showNotification(channel: NotificationChannel) {
    TODO("Not yet implemented")
  }

  private val overflowText = RootLocalizer.create("notification.overflow.text")
  private var isUpdateRunning = false
  private fun updateUi() {
    if (isUpdateRunning) {
      overflowText.update(notifications.size)
      return
    }
    isUpdateRunning = true
    FXUtil.runLater {
      val thresholdNotification = Notifications.create().owner(owner)
        .title(RootLocalizer.formatText("notification.overflow.title"))
        .graphic(Label().also { it.textProperty().bind(overflowText) })
        .hideAfter(Duration.seconds(3600.0))
      val popupBuilder = Notifications.create().owner(owner)
        .hideAfter(Duration.seconds(3600.0))
        .position(Pos.BOTTOM_RIGHT)
        .threshold(4, thresholdNotification)
      val headItems = notifications.filter { it.wasShown.not() }
      headItems.forEach { item ->
        when (item.channel) {
          NotificationChannel.RSS -> {
            popupBuilder.graphic(StackPane().also {
              it.stylesheets.add("/biz/ganttproject/app/mdfx.css")
              it.styleClass.add("notification")
              it.children.add(item.asMarkdownView())
              it.prefWidth = 400.0
              it.prefHeight = 400.0
              it.userData = item
            })
            popupBuilder.show()
          }

          NotificationChannel.WARNING -> {
            popupBuilder.title(item.myTitle).text(item.myBody)
            popupBuilder.graphic(
              ImageView(
                Notifications::class.java.getResource("/org/controlsfx/dialog/dialog-warning.png")?.toExternalForm()
              ).also {
                it.userData = item
              }
            )
            popupBuilder.show()
          }

          NotificationChannel.ERROR -> {
            val imageView = ImageView(
              Notifications::class.java.getResource("/org/controlsfx/dialog/dialog-error.png")?.toExternalForm()
            ).also {
              it.userData = item
            }
            popupBuilder.title(item.myTitle).text(item.myBody)
            popupBuilder.graphic(imageView)
            popupBuilder.show()
          }

          null -> {}
        }
      }
      isUpdateRunning = false
    }
  }

  fun createStatusBarComponent() = HBox().also {
    it.spacing = 5.0
    it.stylesheets.add("biz/ganttproject/app/StatusBar.css")
    it.styleClass.addAll("statusbar", "align_right", "notifications")
    val errorButton = Button("--------")
    errorButton.onAction = EventHandler { showErrors() }
    val rssButton = Button(RootLocalizer.formatText("notification.channel.rss.label"), NEWS_ICON)
    rssButton.onAction = EventHandler { showNews() }

    hasError.addListener { _, _, newValue ->
      FXUtil.runLater {
        when (newValue) {
          0 -> errorButton.styleClass.clear()
          1 -> {
            errorButton.text = RootLocalizer.formatText("notification.channel.warning.label")
            errorButton.graphic = WARNING_ICON
            errorButton.styleClass.clear()
            errorButton.styleClass.addAll("unread", "warning")
          }
          2 -> {
            errorButton.text = RootLocalizer.formatText("notification.channel.error.label")
            errorButton.graphic = ERROR_ICON
            errorButton.styleClass.clear()
            errorButton.styleClass.addAll("unread", "error")
          }
        }
      }
    }
    hasRss.addListener { _, _, newValue ->
      rssButton.styleClass.clear()
      if (newValue) {
        rssButton.styleClass.addAll("unread", "news")
      }
    }
    it.children.addAll(rssButton, errorButton)
    HBox.setHgrow(errorButton, Priority.NEVER)
  }

  private fun showNews() {
    dialog(id = "newsLog") {dlg ->
      setupNotificationDialog(dlg, "", notifications.filter { it.channel == NotificationChannel.RSS }.reversed())
    }
  }

  private fun showErrors() {
    dialog(id = "errorLog") {dlg ->
      setupNotificationDialog(
        dlg,
        RootLocalizer.formatText("viewLog"),
        notifications.filter { it.channel != NotificationChannel.RSS }.reversed()
      )
      dlg.setupButton(ButtonType.NEXT) {btn ->
        btn.styleClass.add("btn-regular")
        btn.text = RootLocalizer.formatText("viewLog")
        btn.onAction = EventHandler {
          SwingUtilities.invokeLater {
            ViewLogDialog.show()
          }
        }
      }
    }
  }

  private fun setupNotificationDialog(dlg: DialogController, title: String, notifications: List<NotificationItem>) {
    dlg.addStyleClass("dlg", "dlg-notification")
    dlg.addStyleSheet(
      "/biz/ganttproject/app/mdfx.css",
      "/biz/ganttproject/app/Dialog.css",
      "/biz/ganttproject/app/NotificationManager.css"
    )
    dlg.setEscCloseEnabled(true)

    dlg.setHeader(
      VBoxBuilder("header").apply {
        addTitleString(title).also { hbox ->
          hbox.alignment = Pos.CENTER_LEFT
          hbox.isFillHeight = true
          hbox.children.add(Region().also { node -> HBox.setHgrow(node, Priority.ALWAYS) })
        }
      }.vbox
    )

    val listView = ListView<NotificationItem>().also {
      it.styleClass.add("notification")
      it.items = FXCollections.observableArrayList()
      it.items.addAll(notifications)
      it.cellFactory = Callback { CellImpl() }
    }

    dlg.setContent(vbox {
      add(listView)
    })

    dlg.setupButton(ButtonType.OK) {btn ->
      btn.styleClass.add("btn-attention")
    }
    dlg.onClosed = {
      updateUi()
    }
  }
}



private class CellImpl : ListCell<NotificationItem>() {
  init {
    stylesheets.add("/biz/ganttproject/app/mdfx.css")
    styleClass.add("column-item-cell")
    alignment = Pos.CENTER_LEFT
  }

  override fun updateItem(item: NotificationItem?, empty: Boolean) {
    super.updateItem(item, empty)
    if (item == null || empty) {
      text = ""
      graphic = null
      return
    }
    text = null
    graphic = vbox {
      add(Label(item.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).also { it.styleClass.add("timestamp") })
      add(item.asMarkdownView())
    }
    item.isRead = true
  }
}

private fun NotificationItem.asMarkdownView() =
  object : MarkdownView("""|
      |**${myTitle}**
      |
      |${myBody}
    """.trimMargin().also(StringEscapeUtils::unescapeJava)) {
    override fun setLink(node: Node, link: String, description: String?) {
      node.cursor = Cursor.HAND
      node.setOnMouseClicked { this@asMarkdownView.myHyperlinkListener?.hyperlinkUpdate(
        HyperlinkEvent(this, HyperlinkEvent.EventType.ACTIVATED, URI.create(link.trim()).toURL()))
      }
    }
  }


private fun NotificationItem.isHtml() =
  HTML_TAGS.any { myBody.contains(it, ignoreCase = true) }

private fun html2md(html: String) =
  FlexmarkHtmlConverter.builder(MutableDataSet()).build().convert(html)

private fun Window.isNotificationPopup() =
  this is Popup && run {
    this.scene.root.findDescendant {
      it.styleClass.contains("notification-bar")
    }
  } != null

private val HTML_TAGS = setOf("<br>", "<br/>", "<html>", "<body>", "<p>")
private val ERROR_ICON = FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_CIRCLE)
private val WARNING_ICON = FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_CIRCLE)
private val NEWS_ICON = MaterialIconView(MaterialIcon.ANNOUNCEMENT)
