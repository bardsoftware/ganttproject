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

import biz.ganttproject.FXUtil;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;

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

            FXUtil.setOpenLinksInBrowser(webEngine);

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

}
