// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPCloudStorage {
  private final GPCloudStorageOptions myOptions;
  private final Consumer<Pane> myUiConsumer;

  public GPCloudStorage(GPCloudStorageOptions options, Consumer<Pane> uiConsumer) {
    myOptions = options;
    myUiConsumer = uiConsumer;
  }

  public void init() {
    if (myOptions.getCloudServer() == null) {
      myUiConsumer.accept(createSetupCloudPane());
    }
  }

  private static Label newLabel(String key, String... classes) {
    Label label = new Label(key);
    label.getStyleClass().addAll(classes);
    return label;
  }

  private static Pane centered(Node... nodes) {
    VBox centered = new VBox();
    centered.setMaxWidth(Double.MAX_VALUE);
    centered.getStyleClass().add("center");
    centered.getChildren().addAll(nodes);
    return centered;
  }

  private Pane createSetupCloudPane() {
    VBox cloudSetupPane = new VBox();
    cloudSetupPane.getStyleClass().add("pane-service-contents");
    Label title = newLabel("Setup GanttProject Cloud", "title");
    Label titleHelp = newLabel(
        "GP Cloud is a cloud-based service for storing your projects and sharing them with the colleagues",
        "title-help");
    cloudSetupPane.getChildren().addAll(title, titleHelp);

    Label signupWarning = newLabel(
        "It seems that this GanttProject is not yet connected to the Cloud.", "alert-warning");
    Label signupSubtitle = newLabel("Not yet signed up?", "subtitle");
    Label signupHelp = newLabel(
        "Creating an account on GanttProject Cloud is free and easy. No credit card required. Get up and running instantly.",
        "help");
    cloudSetupPane.getChildren().addAll(signupWarning, signupSubtitle, signupHelp);


    Button signupButton = new Button("Sign Up");
    signupButton.getStyleClass().addAll("btn-signup");
    cloudSetupPane.getChildren().add(centered(signupButton));


    Label pinSubtitle = newLabel("Already registered on GanttProject Cloud?", "subtitle");
    Label pinHelp = newLabel(
        "You need to connect this GanttProject to the Cloud. Sign in to your account on GanttProject Cloud and request a PIN number. Type the PIN into the field below to setup access credentials",
        "help");
    cloudSetupPane.getChildren().addAll(pinSubtitle, pinHelp);

    TextField pinField = new TextField();
    pinField.setPromptText("Type your PIN");
    HBox pinBox = new HBox();
    pinBox.setMaxWidth(Region.USE_PREF_SIZE);
    pinBox.getChildren().addAll(pinField, new Button("Connect"));

    cloudSetupPane.getChildren().add(centered(pinBox));
    return cloudSetupPane;
  }


}
