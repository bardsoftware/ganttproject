/*
Copyright 2019 BarD Software s.r.o

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
package biz.ganttproject

import biz.ganttproject.app.*
import javafx.animation.FadeTransition
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableBooleanValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.embed.swing.SwingFXUtils
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.stage.Window
import javafx.util.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.action.GPAction
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.UIManager

interface FxUiComponent {
  fun buildNode(): Node
}
/**
 * @author dbarashev@bardsoftware.com
 */
object FXUtil {
  fun runLater(delayMs: Long, code: ()->Unit) {
    FXThread.runLater(delayMs, code)
  }
  fun runLater(code: () -> Unit) {
    FXThread.runLater(code)
  }
  fun launchFx(code: suspend ()->Unit) {
    fxScope.launch {
      code()
    }
  }

  fun startup(code: () -> Unit) {
    FXThread.startup(code)
  }
  /*
  public static Label createHtmlLabel(String htmlContent, String css) {
    WebView browser = new WebView();
    WebEngine webEngine = browser.getEngine();
    Label label = new Label();

    String htmlPage = String.format("<html><head><style type='text/css'>body {\n%s\n}</style></head><body>%s</body></html>", css, htmlContent);
    webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == Worker.State.SUCCEEDED) {
        String width = webEngine.executeScript("document.width").toString();
        label.setPrefWidth(Double.valueOf(width.replace("px", "")));
        String height = webEngine.executeScript("document.height").toString();
        label.setPrefHeight(Double.valueOf(height.replace("px", "")));
      }
    });
    webEngine.loadContent(htmlPage);

    label.setGraphic(browser);
    label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    return label;
  }


  public static void setOpenLinksInBrowser(final WebEngine webEngine) {
    webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
      public void changed(
          ObservableValue<? extends Worker.State> observable,
          Worker.State oldValue,
          Worker.State newValue) {

        if (Worker.State.SUCCEEDED.equals(newValue)) {
          NodeList nodeList = webEngine.getDocument().getElementsByTagName("a");
          for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            EventTarget eventTarget = (EventTarget) node;
            eventTarget.addEventListener("click", new EventListener() {
              @Override
              public void handleEvent(Event evt) {
                evt.preventDefault();
                EventTarget target = evt.getCurrentTarget();
                HTMLAnchorElement anchorElement = (HTMLAnchorElement) target;
                final String href = anchorElement.getHref();
                SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                    try {
                      Desktop.getDesktop().browse(new URI(href));
                    } catch (IOException | URISyntaxException e) {
                      GPLogger.log(e);
                    }
                  }
                });
              }
            }, false);
          }
        }
      }
    });
  }
*/

  /*
    public static final class WebViewFitContent extends Region {

      final WebView webview = new WebView();
      final WebEngine webEngine = webview.getEngine();
      private Runnable myOnReady;

      public WebViewFitContent(String content) {
        webview.setPrefHeight(5);
        widthProperty().addListener(new ChangeListener<Object>() {
          @Override
          public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
            Double width = (Double)newValue;
            webview.setPrefWidth(width);
            adjustHeight();
          }
        });

        webview.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
          @Override
          public void changed(ObservableValue<? extends Worker.State> arg0, Worker.State oldState, Worker.State newState)         {
            if (newState == Worker.State.SUCCEEDED) {
              adjustHeight();
            }
          }
        });
        webview.getChildrenUnmodifiable().addListener(new ListChangeListener<javafx.scene.Node>() {
          @Override
          public void onChanged(Change<? extends javafx.scene.Node> change) {
            Set<javafx.scene.Node> scrolls = webview.lookupAll(".scroll-bar");
            for (javafx.scene.Node scroll : scrolls) {
              scroll.setVisible(false);
            }
          }
        });
        webEngine.loadContent(content);
        getChildren().add(webview);
      }

      public WebEngine getWebEngine() {
        return webEngine;
      }

      public WebView getWebView() {
        return webview;
      }

      @Override
      protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        layoutInArea(webview,0,0,w,h,0, HPos.CENTER, VPos.CENTER);
      }

      private void adjustHeight() {
        Platform.runLater(new Runnable(){
          @Override
          public void run() {
            try {
              {
                Object result = webEngine.executeScript("document.getElementById('body').offsetHeight");
                if (result instanceof Integer) {
                  Integer i = (Integer) result;
                  double height = new Double(i);
                  height = height + 20;
                  webview.setPrefHeight(height);
                  //getPrefHeight();
                }
              }
              {
                Object result = webEngine.executeScript("document.getElementById('body').offsetWidth");
                if (result instanceof Integer) {
                  Integer i = (Integer) result;
                  if (i != 0) {
                    webview.setPrefWidth(new Double(i));
                  }
                }
              }
              myOnReady.run();
            } catch (JSException e) { }
          }
        });
      }

      public void setOnReady(Runnable onReady) {
        myOnReady = onReady;
      }
    }
  */
  fun transitionNode(node: Node, replacePane: ()->Unit, resizer: ()->Unit) {
    val fadeIn = FadeTransition(Duration.seconds(0.5), node)
    fadeIn.fromValue = 0.0
    fadeIn.toValue = 1.0

    val fadeOut = FadeTransition(Duration.seconds(0.5), node)
    fadeOut.fromValue = 1.0
    fadeOut.toValue = 0.1
    fadeOut.play()
    //Exception("Fade out! ").printStackTrace()
    fadeOut.setOnFinished {
      //Exception("Fade in!").printStackTrace()
      replacePane()
      fadeIn.setOnFinished {
        //borderPane.requestLayout()
        resizer()
      }
      fadeIn.play()
    }
  }
  fun transitionCenterPane(borderPane: BorderPane, newCenter: javafx.scene.Node?, resizer: () -> Unit) {
    if (newCenter == null) { return }
    val replacePane = {
      borderPane.center = newCenter
      //resizer()
    }
    if (borderPane.center == null) {
      replacePane()
      resizer()
    } else {
      transitionNode(borderPane, replacePane, resizer)
    }
  }
}

fun Node.printCss() {
  println("class=${styleClass} pseudoclass=${pseudoClassStates} scene=${this.scene}")
  //parent?.printCss()
}

fun Parent.walkTree(code: (Node)->Unit) {
  code(this)
  childrenUnmodifiable.forEach {
    when {
      it is ScrollPane -> it.content.let {
        if (it is Parent) {
          it.walkTree(code)
        }
      }
      it is TitledPane -> it.content.let {
        if (it is Parent) {
          it.walkTree(code)
        }
      }
      it is Parent -> it.walkTree(code)
      else -> code(it)
    }
  }
}

fun Node.findDescendant(predicate: (Node) -> Boolean): Node? {
  if (predicate(this)) return this
  if (this is Parent) {
    childrenUnmodifiable.forEach {
      val descendantNode = it.findDescendant(predicate)
      if (descendantNode != null) { return descendantNode }
    }
  }
  return null
}

fun Parent.applyApplicationFont() {
  FXUtil.runLater {
    this.walkTree {
      if (it is Labeled) {
        it.font = applicationFont.value
        //println("this=$it itfont=${it.font} app font=${applicationFont.value}")
        //it.style = """-fx-font-family: ${applicationFont.value.family}; -fx-font-size: ${applicationFont.value.size } """
      }
    }
  }
}

fun String.colorFromUiManager(): Color? =
  UIManager.getColor(this)?.let { swingColor ->
    Color.color(swingColor.red / 255.0, swingColor.green / 255.0, swingColor.blue / 255.0)
  }

fun centerOnOwner(child: Window, owner: Window) {
  child.x = owner.x + owner.width / 2.0 - child.width / 2.0
  child.y = owner.y + owner.height / 2.0 - child.height / 2.0
}

enum class ButtonContentDisplay {
  TEXT_ONLY, GRAPHIC_ONLY, TEXT_AND_GRAPHIC
}
data class ButtonBuilder(
  val action: ()->Unit = {},
  val text: LocalizedString = RootLocalizer.create(""),
  val isEnabled: ObservableBooleanValue = SimpleBooleanProperty(true),
  val contentDisplay: ButtonContentDisplay = ButtonContentDisplay.TEXT_ONLY,
  val styleClass: ObservableList<String> = FXCollections.observableArrayList(),
  val glyphIcon: Text? = null,
  val hasAutoRepeat: Boolean = false
  ) {
  fun build(): Button {
    if (contentDisplay != ButtonContentDisplay.TEXT_ONLY && glyphIcon == null) {
      error("Button content display is $contentDisplay but glyph icon is missing")
    }
    return Button("", glyphIcon).apply {
      this.contentDisplay = this@ButtonBuilder.contentDisplay.asContentDisplayFx()
      this.alignment = Pos.CENTER_LEFT
      if (this.contentDisplay != ContentDisplay.GRAPHIC_ONLY) {
        this.textProperty().bind(this@ButtonBuilder.text)
      } else {
        this.styleClass.add("graphic-only")
      }
      this.onAction = EventHandler {
        this@ButtonBuilder.action()
      }
      this.disableProperty().bind(Bindings.not(this@ButtonBuilder.isEnabled))
      if (hasAutoRepeat) {
        setupAutoRepeat(this, this@ButtonBuilder.action, 200)
      }
      this.styleClass.addAll(this@ButtonBuilder.styleClass)
      this@ButtonBuilder.styleClass.addListener(ListChangeListener { change ->
        while (change.next()) {
          if (change.wasRemoved()) {
            this.styleClass.removeAll(change.removed)
          }
          if (change.wasAdded()) {
            this.styleClass.addAll(change.addedSubList)
          }
        }
      })
    }
  }
}

private fun ButtonContentDisplay.asContentDisplayFx() =
  when (this) {
    ButtonContentDisplay.TEXT_ONLY -> ContentDisplay.TEXT_ONLY
    ButtonContentDisplay.GRAPHIC_ONLY -> ContentDisplay.GRAPHIC_ONLY
    ButtonContentDisplay.TEXT_AND_GRAPHIC -> ContentDisplay.RIGHT
  }

fun createButton(action: GPAction, onlyIcon: Boolean = true): Button {
  val icon = action.getGlyphIcon()
  val contentDisplay = action.getValue(GPAction.TEXT_DISPLAY) as? ContentDisplay ?: if (onlyIcon) ContentDisplay.GRAPHIC_ONLY else ContentDisplay.RIGHT
  if (icon == null && contentDisplay != ContentDisplay.TEXT_ONLY) {
    error("Icon for action ${action.id} is missing")
  }
  val hasAutoRepeat = action.getValue(GPAction.HAS_AUTO_REPEAT) as? Boolean ?: false
  return Button("", icon).apply {
    this.contentDisplay = contentDisplay
    this.alignment = Pos.CENTER_LEFT
    if (contentDisplay != ContentDisplay.GRAPHIC_ONLY) {
      this.textProperty().bind(action.localizedNameObservable)
    } else {
      this.styleClass.add("graphic-only")
    }
    this.addEventHandler(ActionEvent.ACTION) {
      SwingUtilities.invokeLater {
        action.actionPerformed(null)
      }
    }
    this.isDisable = !action.isEnabled
    action.addPropertyChangeListener {
      this.isDisable = !action.isEnabled
    }
    if (hasAutoRepeat) {
      setupAutoRepeat(this, { action.actionPerformed(null) }, 200);
    }

    //applyFontStyle(this)
  }
}

fun BufferedImage.asJavaFxImage() = SwingFXUtils.toFXImage(this, null)
private val fxScope = CoroutineScope(Dispatchers.JavaFx)


fun setupAutoRepeat(button: Button, action: ()->Unit, intervalMs: Int) {
  class MouseHandlerImpl {
    private var myTimer: Timer? = null
    fun createPressedHandler(): EventHandler<MouseEvent?> {
      return EventHandler { event: MouseEvent? ->
        if (myTimer == null) {
          myTimer = Timer(intervalMs) { action() }
          myTimer!!.setInitialDelay(intervalMs)
          myTimer!!.setDelay(intervalMs / 2)
          myTimer!!.setRepeats(true)
          myTimer!!.start()
        }
      }
    }

    fun createReleasedHandler(): EventHandler<MouseEvent?> {
      return EventHandler { event: MouseEvent? ->
        if (myTimer != null) {
          myTimer!!.stop()
          myTimer = null
        }
      }
    }
  }

  val handler = MouseHandlerImpl()
  button.setOnMousePressed(handler.createPressedHandler())
  button.setOnMouseReleased(handler.createReleasedHandler())
}
