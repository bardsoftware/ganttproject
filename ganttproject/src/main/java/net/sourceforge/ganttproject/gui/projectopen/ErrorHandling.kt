package net.sourceforge.ganttproject.gui.projectopen

import biz.ganttproject.app.RootLocalizer
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.ProjectOpenActivityFailed
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.gui.NotificationChannel
import net.sourceforge.ganttproject.gui.NotificationManager

fun ProjectOpenActivityFailed.showProjectOpenErrorDialog(document: Document, notificationManager: NotificationManager) {
  val msg = """
            Failed to open project: {}
            {}
            -------
            {}
          """.trimIndent()
  DOCUMENT_ERROR_LOGGER.error(msg, document.uri, this.errorTitle, this.errorDescription, exception = this.throwable)
  val notification = notificationManager.createNotification(
    NotificationChannel.ERROR, this.errorTitle, this.errorDescription, null
  )
  notificationManager.showDialog(
    RootLocalizer.formatText("project.open.error.title"),
    listOf(notification))
}

val DOCUMENT_ERROR_LOGGER = GPLogger.create("Document.Error")