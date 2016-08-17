// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.local;

import biz.ganttproject.storage.StorageDialogBuilder;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * @author dbarashev@bardsoftware.com
 */
public class LocalStorage implements StorageDialogBuilder.Ui {
  @Override
  public String getId() {
    return "desktop";
  }

  @Override
  public Pane createUi() {
    VBox rootPane = new VBox();
    rootPane.getStyleClass().add("pane-service-contents");
    rootPane.setPrefWidth(400);

    Label title = new Label("Open from this computer");
    title.getStyleClass().add("title");

    HBox buttonBar = new HBox();
    buttonBar.getStyleClass().add("webdav-button-pane");
    Button btnOpen = new Button("Open");
    btnOpen.addEventHandler(ActionEvent.ACTION, event -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Open Resource File");
      fileChooser.getExtensionFilters().addAll(
          new FileChooser.ExtensionFilter("GanttProject Files", "*.gan"));
      fileChooser.showOpenDialog(null);
    });
    buttonBar.getChildren().add(btnOpen);

    HBox topPane = new HBox();
    topPane.getStyleClass().add("title-pane");

    topPane.getChildren().add(title);
    HBox.setHgrow(buttonBar, Priority.ALWAYS);
    topPane.getChildren().add(buttonBar);
    rootPane.getChildren().add(topPane);

    return rootPane;
  }
}
