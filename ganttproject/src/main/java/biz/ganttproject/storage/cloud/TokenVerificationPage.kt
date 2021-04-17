/*
Copyright 2021 Dmitry Barashev, BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.Spinner
import biz.ganttproject.lib.fx.vbox
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import java.time.Duration
import java.time.Instant

/**
 * @author dbarashev@bardsoftware.com
 */
class TokenVerificationPage : FlowPage() {
  override fun createUi(): Pane {
    val i18nSignin = RootLocalizer.createWithRootKey("cloud.authPane", i18n)

    val expirationInstant = Instant.ofEpochSecond(GPCloudOptions.validity.value.toLongOrNull() ?: 0)
    val remainingDuration = Duration.between(Instant.now(), expirationInstant)
    val expirationValue =
      if (!remainingDuration.isNegative) {
        val hours = remainingDuration.toHours()
        val minutes = remainingDuration.minusMinutes(hours * 60).toMinutes()
        if (hours > 0) {
          i18nSignin.formatText("expirationValue_hm", hours, minutes)
        } else {
          i18nSignin.formatText("expirationValue_m", minutes)
        }
      } else ""

    return paneAndImage(vbox {
      vbox.styleClass.add("fill-parent")
      addTitle(i18nSignin.formatText("title"))
      add(Label(i18nSignin.formatText("expirationMsg", expirationValue)).apply {
        this.styleClass.add("help")
      })
      add(Spinner(Spinner.State.WAITING).pane.also {
        it.maxWidth = Double.MAX_VALUE
        it.maxHeight = Double.MAX_VALUE
      }, Pos.CENTER, Priority.ALWAYS)
      add(Label(i18nSignin.formatText("progressLabel")), Pos.CENTER, Priority.NEVER).also {
        it.styleClass.add("medskip")
      }
      vbox
    })
  }

  override fun resetUi() {}

  override fun setController(controller: GPCloudUiFlow) {}
}

private val i18n = RootLocalizer.createWithRootKey("cloud.signup", RootLocalizer)
