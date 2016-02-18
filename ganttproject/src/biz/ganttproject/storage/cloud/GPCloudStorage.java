// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.storage.StorageDialogBuilder;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPCloudStorage implements StorageDialogBuilder.Ui {
  private final GPCloudStorageOptions myOptions;
  private final GridPane myPane;

  public GPCloudStorage(GPCloudStorageOptions options) {
    myOptions = options;
    myPane = new GridPane();
    myPane.setAlignment(Pos.CENTER);
  }

  private static Label newLabel(String key, String... classes) {
    Label label = new Label(key);
    label.getStyleClass().addAll(classes);
    label.setPrefWidth(400);
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
    cloudSetupPane.setPrefWidth(400);
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
    addPinControls(cloudSetupPane);
    return cloudSetupPane;
  }

  private void addPinControls(Pane pane) {
    TextField pinField = new TextField();
    pinField.setPromptText("Type your PIN");
    HBox pinBox = new HBox();
    pinBox.setMaxWidth(Region.USE_PREF_SIZE);
    pinBox.getChildren().addAll(pinField, new Button("Connect"));

    pane.getChildren().add(centered(pinBox));
  }

  private Pane createConnectCloudPane() {
    VBox cloudConnectPane = new VBox();
    cloudConnectPane.setPrefWidth(400);
    cloudConnectPane.getStyleClass().add("pane-service-contents");
    Label title = newLabel("Sign in to GanttProject Cloud", "title");
    Label titleHelp = newLabel(
        "You seem to be registered on GanttProject Cloud but you don't store your password on disk. You need to request a new PIN and type it into the field below",
        "title-help");
    cloudConnectPane.getChildren().addAll(title, titleHelp);
    addPinControls(cloudConnectPane);
    return cloudConnectPane;
  }


  @Override
  public String getId() {
    return "cloud";
  }

  @Override
  public Pane createUi() {
    if (myPane.getChildren().isEmpty()) {
      WebDavServerDescriptor cloudServer = myOptions.getCloudServer();
      if (cloudServer == null) {
        myPane.getChildren().add(createSetupCloudPane());
      } else if (cloudServer.getPassword() == null) {
        myPane.getChildren().add(createConnectCloudPane());
      }
    }
    return myPane;
  }
}
