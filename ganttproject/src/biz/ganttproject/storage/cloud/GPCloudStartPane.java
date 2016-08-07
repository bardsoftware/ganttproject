// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.storage.StorageDialogBuilder;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

import static biz.ganttproject.storage.cloud.GPCloudStorage.newLabel;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPCloudStartPane {
  private final Consumer<Pane> myUpdateUi;
  private final GPCloudLoginPane myLoginPane;
  private final GPCloudSignupPane mySignupPane;
  private final StorageDialogBuilder.DialogUi myDialogUi;

  GPCloudStartPane(StorageDialogBuilder.DialogUi dialogUi, Consumer<Pane> updateUi, GPCloudLoginPane loginPane, GPCloudSignupPane signupPane) {
    myDialogUi = dialogUi;
    myUpdateUi= updateUi;
    myLoginPane = loginPane;
    mySignupPane = signupPane;
  }
  Pane createPane() {
    VBox cloudSetupPane = new VBox();
    cloudSetupPane.setPrefWidth(400);
    cloudSetupPane.getStyleClass().add("pane-service-contents");
    Label title = newLabel("Setup GanttProject Cloud", "title");
    Label titleHelp = newLabel(
        "GanttProject Cloud is a cloud-based service for storing projects and collaborating with your colleagues",
        "title-help");
    cloudSetupPane.getChildren().addAll(title, titleHelp);

    Label areYouRegistered = newLabel("Are you registered on GanttProject Cloud?", "subtitle");
    ToggleGroup group = new ToggleGroup();
    RadioButton registered = new RadioButton("Yes, I am registered");
    registered.setToggleGroup(group);
    registered.getStyleClass().addAll("btn-radio");
    RadioButton unregistered = new RadioButton("Not yet");
    unregistered.setToggleGroup(group);
    unregistered.setSelected(true);
    unregistered.getStyleClass().addAll("btn-radio");
    cloudSetupPane.getChildren().addAll(areYouRegistered, unregistered, registered);

    Pane spacer = new Pane();
    spacer.getStyleClass().addAll("space-section");
    cloudSetupPane.getChildren().addAll(spacer);
    Button nextPage = new Button("Continue");
    nextPage.addEventHandler(ActionEvent.ACTION, event -> {
      if (unregistered.isSelected()) {
        myUpdateUi.accept(mySignupPane.createPane());
      } else {
        myUpdateUi.accept(myLoginPane.createPane());
      }
    });
    nextPage.getStyleClass().add("btn-continue");
    cloudSetupPane.getChildren().add(nextPage);
    return cloudSetupPane;
  }
}
