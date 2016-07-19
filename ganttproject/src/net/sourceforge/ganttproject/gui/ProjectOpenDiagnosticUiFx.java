/*
GanttProject is an opensource project management tool.
Copyright (C) 2016 BarD Software s.r.o

This file is part of GanttProject.

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
package net.sourceforge.ganttproject.gui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
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
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class shows project opening diagnostics using JavaFX web view.
 *
 * @author dbarashev@bardsoftware.com
 */
class ProjectOpenDiagnosticUiFx {
  void run(final String msg, final ProjectOpenDiagnosticImpl.ShowDialogCallback showDialogCallback) {
    UIUtil.initJavaFx(new Runnable() {
      @Override
      public void run() {
        final JFXPanel contentPane = new JFXPanel();
        Platform.runLater(new Runnable() {
          public void run() {
            VBox root = new VBox();
            WebView browser = new WebView();
            WebEngine webEngine = browser.getEngine();

            webEngine.loadContent(msg);

            setOpenLinksInBrowser(webEngine);

            root.getChildren().addAll(browser);
            Scene scene = new Scene(new Group());
            scene.setRoot(root);
            contentPane.setScene(scene);
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                showDialogCallback.showDialog(contentPane);
              }
            });
          }
        });

      }
    });
  }

  private static void setOpenLinksInBrowser(final WebEngine webEngine) {
    webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
      public void changed(
          ObservableValue<? extends Worker.State> observable,
          javafx.concurrent.Worker.State oldValue,
          javafx.concurrent.Worker.State newValue) {

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
