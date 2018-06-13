// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud

import biz.ganttproject.lib.fx.VBoxBuilder
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.Pane
import javafx.util.Duration
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.language.GanttLanguage
import org.apache.commons.io.IOUtils
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import javax.swing.SwingUtilities


/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudSignupPane internal constructor(private val myUpdateUi: Consumer<Pane>) : GPCloudStorage.PageUi {
  private val i18n = GanttLanguage.getInstance()
  private val httpd: HttpServerImpl by lazy {
    HttpServerImpl().apply { this.start() }
  }

  override fun createPane(): CompletableFuture<Pane> {
    val result = CompletableFuture<Pane>()
    var msg: String? = null
    try {
      msg = String.format(i18n.getText("cloud.signup.html"),
          IOUtils.toString(javaClass.getResourceAsStream("/biz/ganttproject/storage/cloud/GPCloudSignupPane.css"), "UTF-8"),
          "Sign Up to GanttProject Cloud!",
          "Online storage for collaborative work",
          "Integrated with GanttProject Desktop",
          "Free for up to 2 users/month",
          "Scalable pay-as-you-go billing",
          "Click the button or the link below to sign up. It is fast and free.",
          "http://ganttproject.cloud",
          "Sign Up Now!",
          "Already signed up? Click Continue to sign in to GanttProject Cloud.")
      //msg = String.format(i18n.getText("cloud.signup.html1"));
    } catch (e: IOException) {
      e.printStackTrace()
    }

    val rootPane = VBoxBuilder("pane-service-contents", "cloud-storage")
    rootPane.vbox.stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
    rootPane.addTitle("GanttProject Cloud")
    rootPane.add(Label("cloud storage for your projects").apply {
      this.styleClass.add("subtitle")
    })

    val signinMsg = TextArea("").apply {
      this.styleClass.add("help")
      this.isWrapText = true
      this.isEditable = false
      this.opacity = 0.0
      this.prefRowCount = 3
    }

    fun expandMsg(uri: String) {
      val msgText = """
        Please complete sign in into GanttProject cloud in a browser window.
        If it didn't open, copy this link to your browser:
        ${uri}""".trimIndent()
      signinMsg.text = msgText

      val tl = Timeline()
      val k1 = KeyValue(signinMsg.opacityProperty(), 1.0)
      val kf1 = KeyFrame(Duration.millis(1000.0), k1)
      tl.keyFrames.add(kf1)
      tl.play()
    }

    val btnSignIn = Button("Sign in")
    btnSignIn.styleClass.add("doclist-save")
    btnSignIn.addEventHandler(ActionEvent.ACTION) {


      val uri = URI("$GPCLOUD_SIGNIN_URL?callback=${httpd.listeningPort}")
      expandMsg(uri.toASCIIString())

      SwingUtilities.invokeLater {
        try {
          this.httpd.onTokenReceived = Consumer { it -> println("Received token $it") }
          Desktop.getDesktop().browse(uri)
        } catch (e: IOException) {
          GPLogger.log(e)
        } catch (e: URISyntaxException) {
          GPLogger.log(e)
        }
      }
    }

    rootPane.add(btnSignIn, Pos.CENTER, null).apply {
      this.styleClass.add("doclist-save-box")
    }

    rootPane.add(signinMsg)

    rootPane.add(Label("Not registered yet? Sign up now!").apply {
      this.styleClass.add("h2")
    })
    rootPane.add(Label("GanttProject Cloud is free for 2 users per month.").apply {
      this.styleClass.add("help")
      this.isWrapText = true
    })
    val signupBtn = Button("Sign Up")
    signupBtn.styleClass.add("doclist-save")
    rootPane.add(signupBtn, Pos.CENTER, null).apply {
      this.styleClass.add("doclist-save-box")
    }

    result.complete(rootPane.vbox)
    return result
  }
}



