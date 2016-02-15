// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

/**
 * @author dbarashev@bardsoftware.com
 */
public class StorageDialogBuilder {
  Dialog build() {
    ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
    Dialog<Void> dialog = new Dialog<>();
    dialog.getDialogPane().getButtonTypes().add(loginButtonType);

    BorderPane borderPane = new BorderPane();

    VBox servicesPane = new VBox();
    servicesPane.setPadding(new Insets(10));
    servicesPane.setSpacing(8);
    Button filesystemService = new Button("Local machine");
    filesystemService.setOnAction(event -> {
      borderPane.setCenter(new Label("Local machine"));
    });
    Button recentService = new Button("Recent locations");
    recentService.setOnAction(event -> {
      borderPane.setCenter(new Label("Recent locations"));
    });
    Button cloudService = new Button("GanttProject Cloud");
    cloudService.setOnAction(event -> {
      borderPane.setCenter(new Label("GanttProject cloud"));
    });
    servicesPane.getChildren().addAll(filesystemService, recentService, cloudService);

    borderPane.setLeft(servicesPane);
    dialog.getDialogPane().setContent(borderPane);
    dialog.initModality(Modality.WINDOW_MODAL);
    dialog.setTitle("My Projects");
    dialog.setResizable(true);
    return dialog;
  }
}
