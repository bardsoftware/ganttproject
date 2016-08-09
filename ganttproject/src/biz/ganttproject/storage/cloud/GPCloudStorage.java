// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.storage.StorageDialogBuilder;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import net.sourceforge.ganttproject.document.DocumentStorageUi;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import org.controlsfx.control.HyperlinkLabel;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPCloudStorage implements StorageDialogBuilder.Ui {

  interface PageUi {
    CompletableFuture<Pane> createPane();
  }
  private final GPCloudStorageOptions myOptions;
  private final BorderPane myPane;
  private final HBox myButtonPane;
  private final Button myNextButton;
  private DocumentStorageUi.DocumentReceiver myDocumentReceiver;
  private StorageDialogBuilder.DialogUi myDialogUi;
  private EventHandler<ActionEvent> myNextEventHandler;

  public GPCloudStorage(GPCloudStorageOptions options) {
    myOptions = options;
    myPane = new BorderPane();
    myButtonPane = new HBox();
    myButtonPane.getStyleClass().add("button-pane");
    myButtonPane.setAlignment(Pos.CENTER);
    myNextButton = new Button("Continue");
    myButtonPane.getChildren().add(myNextButton);
    myNextButton.visibleProperty().setValue(false);
    myPane.setBottom(myButtonPane);
  }

  static Label newLabel(String key, String... classes) {
    Label label = new Label(key);
    label.getStyleClass().addAll(classes);
    //label.setPrefWidth(400);
    return label;
  }

  static HyperlinkLabel newHyperlink(EventHandler<ActionEvent> eventHandler, String text, String... classes) {
    HyperlinkLabel result = new HyperlinkLabel(text);
    result.addEventHandler(ActionEvent.ACTION, eventHandler);
    result.getStyleClass().addAll(classes);
    return result;
  }

  static Pane centered(Node... nodes) {
    VBox centered = new VBox();
    centered.setMaxWidth(Double.MAX_VALUE);
    centered.getStyleClass().add("center");
    centered.getChildren().addAll(nodes);
    return centered;
  }


  @Override
  public String getId() {
    return "cloud";
  }

  @Override
  public Pane createUi(DocumentStorageUi.DocumentReceiver documentReceiver, StorageDialogBuilder.DialogUi dialogUi) {
    myDocumentReceiver = documentReceiver;
    myDialogUi = dialogUi;
    return doCreateUi();
  }

  private Pane doCreateUi() {
    GPCloudLoginPane loginPane = new GPCloudLoginPane(myOptions, myDialogUi);
    GPCloudSignupPane signupPane = new GPCloudSignupPane(this::nextPage, loginPane);
    //GPCloudStartPane startPane = new GPCloudStartPane(this::nextPage, this::setNextButton, loginPane, signupPane);
    Optional<WebDavServerDescriptor> cloudServer = myOptions.getCloudServer();
    if (cloudServer.isPresent()) {
      WebDavServerDescriptor wevdavServer = cloudServer.get();
      if (wevdavServer.getPassword() == null) {
        loginPane.createPane().thenApply(pane -> nextPage(pane));
      } else {
        WebdavStorage webdavStorage = new WebdavStorage(wevdavServer);
        myPane.setCenter(webdavStorage.createUi(myDocumentReceiver, myDialogUi));
      }
    } else {
      signupPane.createPane().thenApply(pane -> nextPage(pane));
    }
    return myPane;
  }

//  private void setNextButton(Runnable onClick) {
//    setNextButton(onClick, "Continue");
//  }
//
//  private void setLoginButton(Runnable onClick) {
//    setNextButton(onClick, "Login");
//  }
//
//  private void setNextButton(Runnable onClick, String text) {
//    myNextButton.setVisible(true);
//    myNextButton.setDisable(false);
//    if (myNextEventHandler != null) {
//      myNextButton.removeEventHandler(ActionEvent.ACTION, myNextEventHandler);
//    }
//    myNextEventHandler = event -> onClick.run();
//    myNextButton.addEventHandler(ActionEvent.ACTION, myNextEventHandler);
//    myNextButton.setText(text);
//  }


  private Pane nextPage(Pane newPage) {
    Runnable replacePane = () -> {
      myNextButton.setDisable(true);
      myNextButton.setVisible(false);
      myPane.setCenter(newPage);
      myDialogUi.resize();
    };
    if (myPane.getCenter() == null) {
      replacePane.run();
    } else {
      FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), myPane);
      fadeIn.setFromValue(0.0);
      fadeIn.setToValue(1.0);

      FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), myPane);
      fadeOut.setFromValue(1.0);
      fadeOut.setToValue(0.1);
      fadeOut.play();
      fadeOut.setOnFinished(e -> {
        replacePane.run();
        fadeIn.setOnFinished(e1 -> myDialogUi.resize());
        fadeIn.play();
      });
    }
    return newPage;
  }
}
