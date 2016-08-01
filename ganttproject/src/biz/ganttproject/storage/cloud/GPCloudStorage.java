// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.storage.StorageDialogBuilder;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import net.sourceforge.ganttproject.document.DocumentStorageUi;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPCloudStorage implements StorageDialogBuilder.Ui {
  private final GPCloudStorageOptions myOptions;
  private final BorderPane myPane;
  private DocumentStorageUi.DocumentReceiver myDocumentReceiver;
  private StorageDialogBuilder.ErrorUi myErrorUi;

  public GPCloudStorage(GPCloudStorageOptions options) {
    myOptions = options;
    myPane = new BorderPane();
  }

  static Label newLabel(String key, String... classes) {
    Label label = new Label(key);
    label.getStyleClass().addAll(classes);
    label.setPrefWidth(400);
    return label;
  }

  static Pane centered(Node... nodes) {
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


    Label signupSubtitle = newLabel("Not yet signed up?", "subtitle");
    Label signupHelp = newLabel(
        "Creating an account on GanttProject Cloud is free and easy. No credit card required. Get up and running instantly.",
        "help");
    cloudSetupPane.getChildren().addAll(signupSubtitle, signupHelp);


    Button signupButton = new Button("Sign Up");
    signupButton.getStyleClass().addAll("btn-signup");
    cloudSetupPane.getChildren().add(centered(signupButton));
    return cloudSetupPane;
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
    return cloudConnectPane;
  }


  @Override
  public String getId() {
    return "cloud";
  }

  @Override
  public Pane createUi(DocumentStorageUi.DocumentReceiver documentReceiver, StorageDialogBuilder.ErrorUi errorUi) {
    myDocumentReceiver = documentReceiver;
    myErrorUi = errorUi;
    return doCreateUi();
  }

  private Pane doCreateUi() {
    GPCloudLoginPane loginPane = new GPCloudLoginPane(myOptions, myErrorUi);
    GPCloudStartPane startPane = new GPCloudStartPane(this::replaceUi, loginPane);
    if (myPane.getChildren().isEmpty()) {
      WebDavServerDescriptor cloudServer = myOptions.getCloudServer();
      if (cloudServer == null) {
        myPane.setCenter(startPane.createPane());
      } else if (cloudServer.getPassword() == null) {
        myPane.setCenter(createConnectCloudPane());
      } else {
        WebdavStorage webdavStorage = new WebdavStorage(cloudServer);
        myPane.setCenter(webdavStorage.createUi(myDocumentReceiver, myErrorUi));
      }
    }
    return myPane;
  }

  private void replaceUi(Pane newUi) {
    FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), myPane);
    fadeIn.setFromValue(0.0);
    fadeIn.setToValue(1.0);

    FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), myPane);
    fadeOut.setFromValue(1.0);
    fadeOut.setToValue(0.1);
    fadeOut.play();
    fadeOut.setOnFinished(e ->  {
      myPane.getChildren().clear();
      myPane.getChildren().add(newUi);
      fadeIn.play();
    });

  }
}
