// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import javafx.beans.property.BooleanProperty;
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
public class GPCloudStartPane  {
  private final Consumer<GPCloudStorage.PageUi> myUpdateUi;
  private final GPCloudLoginPane myLoginPane;
  private final GPCloudSignupPane mySignupPane;
  private final Consumer<Runnable> myOnClickNext;
  private BooleanProperty myIsRegistered;

  GPCloudStartPane(Consumer<GPCloudStorage.PageUi> updateUi, Consumer<Runnable> onClickNext, GPCloudLoginPane loginPane, GPCloudSignupPane signupPane) {
    myOnClickNext = onClickNext;
    myUpdateUi= updateUi;
    myLoginPane = loginPane;
    mySignupPane = signupPane;
  }
  public Pane createPane() {
    VBox cloudSetupPane = new VBox();
    cloudSetupPane.setPrefWidth(400);
    cloudSetupPane.getStyleClass().add("pane-service-contents");
    Label title = newLabel("GanttProject Cloud", "title");
    Label titleHelp = newLabel(
        "We have not found your GanttProject Cloud access credentials",
        "title-help");
    cloudSetupPane.getChildren().addAll(title, titleHelp);

    Label areYouRegistered = newLabel("Are you registered on GanttProject Cloud?", "subtitle");
    ToggleGroup group = new ToggleGroup();
    RadioButton registered = new RadioButton("Yes, I am registered");
    registered.setToggleGroup(group);
    registered.getStyleClass().addAll("btn-radio");
    myIsRegistered = registered.selectedProperty();
    RadioButton unregistered = new RadioButton("Not yet");
    unregistered.setToggleGroup(group);
    unregistered.setSelected(true);
    unregistered.getStyleClass().addAll("btn-radio");
    cloudSetupPane.getChildren().addAll(areYouRegistered, unregistered, registered);

    return cloudSetupPane;
  }

  public void setVisible(boolean visible) {
    myOnClickNext.accept(() -> {
      if (myIsRegistered.getValue()) {
        myUpdateUi.accept(myLoginPane);
      } else {
        myUpdateUi.accept(mySignupPane);
      }
    });

  }
}
