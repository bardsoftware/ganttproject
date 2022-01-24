/*
Copyright 2018 BarD Software s.r.o

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

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.openInBrowser
import com.google.common.base.Strings
import com.sandec.mdfx.MDFXNode
import javafx.event.ActionEvent
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority


/**
 * This page shows a small introduction which explains what is GanttProject Cloud and
 * provides "Sign In" and "Register" buttons.
 *
 * @author dbarashev@bardsoftware.com
 */
class GPCloudSignupPane() : FlowPage() {
  private lateinit var controller: GPCloudUiFlow
  private val i18n = RootLocalizer.createWithRootKey("cloud.signup", RootLocalizer)

  override fun createUi() = createPane()
  override fun resetUi() {}
  override fun setController(controller: GPCloudUiFlow) {
    this.controller = controller
  }

  private fun createPane(msgIntro: String? = null): Pane {
    val vboxBuilder = VBoxBuilder()
    vboxBuilder.addTitle(i18n.formatText("title"))
    vboxBuilder.add(Label().apply {
      this.textProperty().bind(i18n.create("titleHelp"))
      this.styleClass.add("help")
    })
    if (!Strings.isNullOrEmpty(msgIntro)) {
      vboxBuilder.add(Label(msgIntro).apply {
        this.styleClass.add("intro")
      })
    }
    val mdfx = MDFXNode(i18n.create("body").value).also {
      it.styleClass.add("medskip")
    }
    vboxBuilder.add(mdfx, Pos.CENTER, Priority.ALWAYS)


    val btnSignUp = Button(i18n.formatText("register"))
    btnSignUp.styleClass.add("btn-attention")
    btnSignUp.addEventHandler(ActionEvent.ACTION) {
      openInBrowser(GPCLOUD_SIGNUP_URL)
    }
    val btnSignIn = Button(i18n.formatText("generic.signIn")).also {
      it.addEventFilter(ActionEvent.ACTION) {
        this.controller.transition(SceneId.SIGNIN)
      }
      it.styleClass.addAll("btn-attention", "secondary")
    }


    GridPane().also { grid ->
      grid.add(Pane(), 0, 0)
      grid.add(btnSignUp, 1, 0)
      grid.add(btnSignIn, 2, 0)
      GridPane.setMargin(btnSignUp, Insets(0.0, 5.0, 0.0, 0.0))
      grid.columnConstraints.add(ColumnConstraints().apply {
        hgrow = Priority.ALWAYS
        isFillWidth = true
      })
      grid.styleClass.addAll("fill-parent", "btnbar")
      vboxBuilder.add(grid, Pos.CENTER_RIGHT, Priority.NEVER)
    }


    return paneAndImage1(vboxBuilder.vbox)
  }
}


