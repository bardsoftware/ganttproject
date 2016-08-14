// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.FXUtil;
import biz.ganttproject.storage.StorageDialogBuilder;
import biz.ganttproject.storage.webdav.WebdavStorage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import org.controlsfx.control.HyperlinkLabel;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPCloudStorage implements StorageDialogBuilder.Ui {


  interface PageUi {
    CompletableFuture<Pane> createPane();
  }

  private final StorageDialogBuilder.Mode myMode;
  private final GPCloudStorageOptions myOptions;
  private final BorderPane myPane;
  private final HBox myButtonPane;
  private final Button myNextButton;
  private final Consumer<Document> myOpenDocument;
  private StorageDialogBuilder.DialogUi myDialogUi;

  public GPCloudStorage(StorageDialogBuilder.Mode mode, GPCloudStorageOptions options, Consumer<Document> openDocument, StorageDialogBuilder.DialogUi dialogUi) {
    myMode = mode;
    myOptions = options;
    myPane = new BorderPane();
    myButtonPane = new HBox();
    myButtonPane.getStyleClass().add("button-pane");
    myButtonPane.setAlignment(Pos.CENTER);
    myNextButton = new Button("Continue");
    myButtonPane.getChildren().add(myNextButton);
    myNextButton.visibleProperty().setValue(false);
    myPane.setBottom(myButtonPane);
    myOpenDocument = openDocument;
    myDialogUi = dialogUi;
  }

  static Label newLabel(String key, String... classes) {
    Label label = new Label(key);
    label.getStyleClass().addAll(classes);
    return label;
  }

  static HyperlinkLabel newHyperlink(EventHandler<ActionEvent> eventHandler, String text, String... classes) {
    HyperlinkLabel result = new HyperlinkLabel(text);
    result.addEventHandler(ActionEvent.ACTION, eventHandler);
    result.getStyleClass().addAll(classes);
    return result;
  }

  @Override
  public String getId() {
    return "cloud";
  }

  @Override
  public Pane createUi() {
    return doCreateUi();
  }

  private Pane doCreateUi() {
    WebdavStorage webdavStorage = new WebdavStorage(myMode, myOpenDocument, myDialogUi);
    GPCloudLoginPane loginPane = new GPCloudLoginPane(myOptions, myDialogUi, this::nextPage, webdavStorage);
    GPCloudSignupPane signupPane = new GPCloudSignupPane(this::nextPage, loginPane);
    Optional<WebDavServerDescriptor> cloudServer = myOptions.getCloudServer();
    if (cloudServer.isPresent()) {
      WebDavServerDescriptor wevdavServer = cloudServer.get();
      if (wevdavServer.getPassword() == null) {
        loginPane.createPane().thenApply(pane -> nextPage(pane));
      } else {
        webdavStorage.setServer(wevdavServer);
        myPane.setCenter(webdavStorage.createUi());
      }
    } else {
      signupPane.createPane().thenApply(pane -> nextPage(pane));
    }
    return myPane;
  }

  private Pane nextPage(Pane newPage) {
    FXUtil.transitionCenterPane(myPane, newPage, myDialogUi::resize);
    return newPage;
  }
}
