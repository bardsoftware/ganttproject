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

import javafx.animation.FadeTransition
import javafx.animation.ScaleTransition
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.control.Dialog
import javafx.scene.input.KeyCombination
import javafx.scene.layout.BorderPane
import javafx.util.Duration

/**
 * @author dbarashev@bardsoftware.com
 */
object FXUtil {
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
  fun createBreathingButton(button: javafx.scene.control.Button) {
    val animation = ScaleTransition(Duration.seconds(2.0), button)
    animation.isAutoReverse = true
    animation.cycleCount = Timeline.INDEFINITE
    animation.byX = 0.1
    animation.byY = 0.1
    animation.play()
  }

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
  fun transitionCenterPane(borderPane: BorderPane, newCenter: javafx.scene.Node?, resizer: () -> Unit) {
    if (newCenter == null) { return }
    val replacePane = Runnable {
      borderPane.center = newCenter
        //resizer()
    }
    if (borderPane.center == null) {
      replacePane.run()
    } else {
      val fadeIn = FadeTransition(Duration.seconds(0.5), borderPane)
      fadeIn.fromValue = 0.0
      fadeIn.toValue = 1.0

      val fadeOut = FadeTransition(Duration.seconds(0.5), borderPane)
      fadeOut.fromValue = 1.0
      fadeOut.toValue = 0.1
      fadeOut.play()
      //Exception("Fade out! ").printStackTrace()
      fadeOut.setOnFinished {
        //Exception("Fade in!").printStackTrace()
        replacePane.run()
        fadeIn.setOnFinished {
          borderPane.requestLayout()
          resizer()
        }
        fadeIn.play()
      }
    }
  }

  fun showDialog(dlg: Dialog<Unit>) {
    Platform.runLater {
      dlg.also {
        it.isResizable = true
        it.dialogPane.apply {
          styleClass.addAll("dlg-lock", "dlg-cloud-file-options")
          stylesheets.addAll("/biz/ganttproject/storage/cloud/GPCloudStorage.css", "/biz/ganttproject/storage/StorageDialog.css")

          val window = scene.window
          window.onCloseRequest = EventHandler {
            window.hide()
          }
          scene.accelerators[KeyCombination.keyCombination("ESC")] = Runnable { window.hide() }
        }
        it.onShown = EventHandler { _ ->
          it.dialogPane.layout()
          it.dialogPane.scene.window.sizeToScene()
        }
        it.show()
      }
    }

  }
}
