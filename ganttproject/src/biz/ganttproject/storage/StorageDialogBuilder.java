// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author dbarashev@bardsoftware.com
 */
public class StorageDialogBuilder {
  private Button myActiveBtn;

  Dialog build() {
    ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
    Dialog<Void> dialog = new Dialog<>();
    dialog.getDialogPane().getButtonTypes().add(loginButtonType);

    BorderPane borderPane = new BorderPane();

    VBox servicesPane = new VBox();
    servicesPane.setPadding(new Insets(10));
    servicesPane.setSpacing(8);

    Button recentService = createButton("recent", event -> {
      borderPane.setCenter(new Label("Recent locations"));
    });
    Pane cloudSetupPane = createSetupCloudPane();
    Button cloudService = createButton("cloud", event -> {
      borderPane.setCenter(cloudSetupPane);
    });
    servicesPane.getChildren().addAll(recentService, cloudService);
    servicesPane.getStyleClass().add("pane-service-buttons");

    borderPane.setLeft(servicesPane);
    dialog.getDialogPane().getStylesheets().add("biz/ganttproject/storage/StorageDialog.css");
    dialog.getDialogPane().getStyleClass().add("body");
    dialog.getDialogPane().setContent(borderPane);
    dialog.initModality(Modality.WINDOW_MODAL);
    dialog.setTitle("My Projects");
    dialog.setResizable(true);
    dialog.setWidth(300);
    dialog.setHeight(300);
    return dialog;
  }

  private Button createButton(String key, EventHandler<ActionEvent> onClick) {
    String label = GanttLanguage.getInstance().getText(String.format("storageView.service.%s.label", key));
    Button btnService = new Button(label);
    btnService.addEventHandler(ActionEvent.ACTION, event -> {
      btnService.getStyleClass().add("active");
      if (myActiveBtn != null) {
        myActiveBtn.getStyleClass().remove("active");
      }
      myActiveBtn = btnService;
    });
    btnService.addEventHandler(ActionEvent.ACTION, onClick);
    btnService.getStyleClass().add("btn-service");
    btnService.setMaxWidth(Double.MAX_VALUE);
    return btnService;
  }

  private Pane createSetupCloudPane() {
    VBox cloudSetupPane = new VBox();
    cloudSetupPane.getStyleClass().add("pane-service-contents");
    Label title = new Label("Setup GanttProject Cloud");
    title.getStyleClass().add("title");

    Label titleHelp = new Label("GP Cloud is a cloud-based service for storing your projects and sharing them with the colleagues");
    titleHelp.getStyleClass().add("title-help");

    VBox signupBox = new VBox();
    signupBox.getStyleClass().add("center");
    Button signupButton = new Button("Sign Up");
    signupButton.getStyleClass().addAll("btn-signup");
    signupButton.setAlignment(Pos.CENTER);
    Label signupHelpLabel = new Label("Sign up is fast and free");
    signupHelpLabel.getStyleClass().addAll("help", "help-signup");
    signupHelpLabel.setAlignment(Pos.CENTER);
    signupBox.getChildren().addAll(signupButton, signupHelpLabel);

    Label pinLabel = new Label("Connect");
    pinLabel.getStyleClass().add("section");

    TextField pinField = new TextField();
    pinField.setPromptText("Type your PIN");
    HBox pinBox = new HBox();
    pinBox.getChildren().addAll(pinField, new Button("Go!"));
    cloudSetupPane.setSpacing(4);
    cloudSetupPane.getChildren().addAll(title, titleHelp, signupBox,  pinLabel, pinBox);
    return cloudSetupPane;
  }
}
