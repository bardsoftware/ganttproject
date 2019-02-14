// Copyright (C) 2016 BarD Software
package biz.ganttproject;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

/**
 * @author dbarashev@bardsoftware.com
 */
public class FXUtil {
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
  public static void createBreathingButton(javafx.scene.control.Button button) {
    ScaleTransition animation = new ScaleTransition(Duration.seconds(2), button);
    animation.setAutoReverse(true);
    animation.setCycleCount(Timeline.INDEFINITE);
    animation.setByX(0.1);
    animation.setByY(0.1);
    animation.play();
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
  public static void transitionCenterPane(BorderPane borderPane, javafx.scene.Node newCenter, Runnable resizer) {
    Runnable replacePane = () -> {
      borderPane.setCenter(newCenter);
      resizer.run();
    };
    if (borderPane.getCenter() == null) {
      replacePane.run();
    } else {
      FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), borderPane);
      fadeIn.setFromValue(0.0);
      fadeIn.setToValue(1.0);

      FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), borderPane);
      fadeOut.setFromValue(1.0);
      fadeOut.setToValue(0.1);
      fadeOut.play();
      fadeOut.setOnFinished(e -> {
        replacePane.run();
        fadeIn.setOnFinished(e1 -> resizer.run());
        fadeIn.play();
      });
    }
  }
}
