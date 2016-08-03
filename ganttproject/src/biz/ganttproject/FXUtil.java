// Copyright (C) 2016 BarD Software
package biz.ganttproject;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import net.sourceforge.ganttproject.GPLogger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

import javax.swing.*;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author dbarashev@bardsoftware.com
 */
public class FXUtil {
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
}
