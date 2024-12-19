/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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

import java.time.LocalDateTime
import javax.swing.event.HyperlinkListener

class NotificationItem(
    val channel: NotificationChannel,
    title: String,
    body: String,
    val timestamp: LocalDateTime,
    val myHyperlinkListener: HyperlinkListener
) {
    val myTitle: String = title ?: ""
    val myBody: String = body ?: ""
    @JvmField
    var isRead: Boolean = false
    var wasShown: Boolean = false

//    override fun equals(obj: Any?): Boolean {
//        if (obj === this) {
//            return true
//        }
//        if (obj is NotificationItem) {
//            val that = obj
//            return myTitle == that.myTitle && myBody == that.myBody
//        }
//        return false
//    }

//    override fun hashCode(): Int {
//        return myBody.hashCode()
//    }
}
