// Copyright (C) 2018 BarD Software
package biz.ganttproject.storage.cloud

import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.openInBrowser
import biz.ganttproject.storage.StorageDialogBuilder
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.language.GanttLanguage
import org.controlsfx.control.HyperlinkLabel
import org.controlsfx.control.NotificationPane
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer


/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudSignupPane internal constructor(
    private val dialogUi: StorageDialogBuilder.DialogUi,
    private val myUpdateUi: Consumer<Pane>) : GPCloudStorage.PageUi {
  private val i18n = GanttLanguage.getInstance()

  private val httpd: HttpServerImpl by lazy {
    HttpServerImpl().apply { this.start() }
  }

  override fun createPane(): CompletableFuture<Pane> {
    val result = CompletableFuture<Pane>()

    val notification = NotificationPane()

    val rootPane = VBoxBuilder("pane-service-contents", "cloud-storage")
    rootPane.addTitle("GanttProject Cloud")
    rootPane.add(Label("collaborative storage for your projects").apply {
      this.styleClass.add("subtitle")
    })

    val signinMsg = TextArea("").apply {
      this.styleClass.addAll("text-area", "help")
      this.isWrapText = true
      this.isEditable = false
      this.prefRowCount = 3
    }

    fun expandMsg(uri: String) {
      val msgText = """
        We just've opened a new browser tab to sign in into GanttProject Cloud. If it didn't open, copy this link to your browser address bar:
        ${uri}""".trimIndent()
      signinMsg.text = msgText
      notification.graphic = signinMsg
      notification.show()


      //this.dialogUi.message(msgText)
//      signinPane.lookup(".title").isVisible = false;
//      signinPane.isVisible = true
//      signinPane.isExpanded = true
//      val text = signinMsg.lookup(".text")
//      val finalHeight = signinMsg.font.size + text.boundsInLocal.height

//      val tl = Timeline()
//      val k1 = KeyValue(signinMsg.prefHeightProperty(), finalHeight)
//      val kf1 = KeyFrame(Duration.millis(1000.0), k1)
//      tl.keyFrames.add(kf1)
//      tl.play()
    }

    val btnSignIn = Button("Sign In")
    btnSignIn.styleClass.add("btn-attention")

    btnSignIn.addEventHandler(ActionEvent.ACTION) {
      val uri = "$GPCLOUD_SIGNIN_URL?callback=${httpd.listeningPort}"
      expandMsg(uri)

      this.httpd.onTokenReceived = { token, validity, userId ->
        with(GPCloudOptions) {
          this.authToken.value = token
          this.validity.value = validity?.toIntOrNull()
          this.userId.value = userId
        }
        println(GPCloudOptions)
      }
      openInBrowser(uri)
    }

    rootPane.add(btnSignIn, Pos.CENTER, null).apply {
      this.styleClass.add("doclist-save-box")
    }

    //rootPane.add(signinPane)

    rootPane.add(Label("Not registered yet? Sign up now!").apply {
      this.styleClass.add("h2")
    })
    rootPane.add(HyperlinkLabel("GanttProject Cloud is free for up to 2 users per month. [Learn more]").apply {
      this.styleClass.add("help")
      this.onAction = EventHandler {
        val link = it.source as Hyperlink?
        when (link?.text) {
          "Learn more" -> openInBrowser(GPCLOUD_LANDING_URL)
          else -> {
          }
        }
      }
    })
    val signupBtn = Button("Sign Up")
    signupBtn.styleClass.add("btn-attention")
    signupBtn.addEventHandler(ActionEvent.ACTION) {
      openInBrowser(GPCLOUD_SIGNUP_URL)
    }

    rootPane.add(signupBtn, Pos.CENTER, null).apply {
      this.styleClass.add("doclist-save-box")
    }

    notification.content = rootPane.vbox
    notification.isShowFromTop = false
    notification.styleClass.addAll("fill-parent", "alert-info")
    val wrapperPane = BorderPane(notification)
    wrapperPane.stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
    result.complete(wrapperPane)
    return result
  }
}



