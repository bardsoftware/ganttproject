// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import biz.ganttproject.FXUtil;
import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import com.google.common.base.Preconditions;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.document.ReadOnlyProxyDocument;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import org.controlsfx.control.SegmentedButton;
import org.controlsfx.control.StatusBar;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author dbarashev@bardsoftware.com
 */
public class StorageDialogBuilder {
  private final IGanttProject myProject;
  private final GPCloudStorageOptions myCloudStorageOptions;
  private final Consumer<Document> myDocumentReceiver;
  private final Consumer<Document> myDocumentUpdater;
  private StatusBar myNotificationPane;
  //private
  //@Nullable
  //Dialog myDialog = null;
  private EventHandler<ActionEvent> myOnNextClick;
  private Pane myOpenStorage;
  private Pane mySaveStorage;
  private Scene myScene;
  private UIFacade.Dialog myDialog;

  public void setDialog(UIFacade.Dialog dlg) {
    myDialog = dlg;
    myScene.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ESCAPE) {
        dlg.hide();
      }
    });
  }
  private DialogUi myDialogUi = new DialogUi() {

    @Override
    public void error(Throwable e) {
      setClass("alert-error");
      //myNotificationPane.setContent(createErrorPane(e.getMessage()));
      myNotificationPane.setText(e.getMessage());
      //myNotificationPane.setExpanded(true);
    }

    @Override
    public void error(String message) {
      setClass("alert-error");
      myNotificationPane.setText(message);
      //myNotificationPane.setContent(createErrorPane(message));
      //myNotificationPane.setExpanded(true);
    }

    @Override
    public void message(String message) {
      setClass("alert-info");
      myNotificationPane.setText(message);
//      myNotificationPane.setContent(createErrorPane(message));
//      myNotificationPane.setExpanded(true);
    }

    @Override
    public void showBusyIndicator(boolean show) {
      if (show) {
        myNotificationPane.setProgress(-1);
      } else {
        myNotificationPane.setProgress(0);
      }
    }

    @Override
    public void close() {
      myDialog.hide();
      //myJDialog.setVisible(false);
      //myDialog.setResult(Boolean.TRUE);
      //myDialog.close();
    }

    @Override
    public void resize() {
      //myDialog.getDialogPane().getScene().getWindow().sizeToScene();
    }

    private void setClass(String className) {
      myNotificationPane.getStyleClass().clear();
      myNotificationPane.getStyleClass().add(className);
    }
  };


  public StorageDialogBuilder(@Nonnull IGanttProject project, ProjectUIFacade projectUi, DocumentManager documentManager, @Nonnull GPCloudStorageOptions cloudStorageOptions) {
    myCloudStorageOptions = Preconditions.checkNotNull(cloudStorageOptions);
    myDocumentReceiver = document -> SwingUtilities.invokeLater(() -> {
      try {
        projectUi.openProject(documentManager.getProxyDocument(document), project);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (Document.DocumentException e) {
        e.printStackTrace();
      }
    });
    myDocumentUpdater = document -> SwingUtilities.invokeLater(() -> {
      if (project.getDocument() == null) {
        project.setDocument(documentManager.getProxyDocument(document));
      } else {
        project.getDocument().setMirror(document);
      }
      try {
        project.getDocument().write();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    myProject = project;
  }

  JFXPanel build() {
    //Dialog<Void> dialog = new Dialog<>();
    //myDialog = dialog;
    //Window window = dialog.getDialogPane().getScene().getWindow();
    //window.setOnCloseRequest(event -> window.hide());

    BorderPane borderPane = new BorderPane();
    myScene = new Scene(borderPane);
    myScene.getStylesheets().add("biz/ganttproject/storage/StorageDialog.css");
    borderPane.getStyleClass().add("body");

    borderPane.getStyleClass().add("pane-storage");
    borderPane.setCenter(new Pane());
    ToggleButton btnSave = new ToggleButton("Save project as");
    ToggleButton btnOpen = new ToggleButton("Open other project");

    {
      VBox titleBox = new VBox();
      titleBox.getStyleClass().add("title-box");
      Label projectName = new Label(myProject.getProjectName());

      SegmentedButton buttonBar = new SegmentedButton();
      buttonBar.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
      btnOpen.addEventHandler(ActionEvent.ACTION, e -> showOpenStorageUi(borderPane));
//
      btnSave.addEventHandler(ActionEvent.ACTION, e -> showSaveStorageUi(borderPane));
      buttonBar.getButtons().addAll(btnSave, btnOpen);
      HBox buttonWrapper = new HBox();
      buttonWrapper.getStyleClass().addAll("open-save-buttons");
      buttonWrapper.getChildren().add(buttonBar);

      titleBox.getChildren().addAll(projectName, buttonWrapper);
      borderPane.setTop(titleBox);
    }
    myNotificationPane = new StatusBar();
    myNotificationPane.getStyleClass().add("notification");
    myNotificationPane.setText("");
    JFXPanel jfxPanel = new JFXPanel();
    jfxPanel.setScene(myScene);


    return jfxPanel;
    //myJDialog.getContentPane().add(jfxPanel);
    //return myDialogUi;
    //borderPane.setBottom(myNotificationPane);


//    dialog.getDialogPane().setContent(borderPane);
//    dialog.initModality(Modality.APPLICATION_MODAL);
//
//    dialog.setTitle("My Projects");
//    dialog.setResizable(true);
//    dialog.getDialogPane().getScene().getWindow().sizeToScene();
//    dialog.getDialogPane().getScene().setOnKeyPressed(keyEvent -> {
//      if (keyEvent.getCode() == KeyCode.ESCAPE) {
//        window.hide();
//      }
//    });
//
//    dialog.setOnShown(event -> {
//      dialog.getDialogPane().getScene().getWindow().sizeToScene();
//      if (myProject.isModified()) {
//        //showSaveStorageUi(borderPane);//
//        btnSave.fire();
//      } else {
//        showOpenStorageUi(borderPane);
//        btnOpen.fire();
//      }
//    });
//    return dialog;
  }

  private void showOpenStorageUi(BorderPane container) {
    if (myOpenStorage == null) {
      myOpenStorage = buildStoragePane(Mode.OPEN);
    }
    FXUtil.transitionCenterPane(container, myOpenStorage, myDialogUi::resize);
  }

  private void showSaveStorageUi(BorderPane container) {
    if (mySaveStorage == null) {
      mySaveStorage = buildStoragePane(Mode.SAVE);
    }
    FXUtil.transitionCenterPane(container, mySaveStorage, myDialogUi::resize);
  }

  private Pane buildStoragePane(Mode mode) {
    StoragePane storagePane = new StoragePane(myCloudStorageOptions, myProject.getDocumentManager(), new ReadOnlyProxyDocument(myProject.getDocument()), myDocumentReceiver, myDocumentUpdater, myDialogUi);
    storagePane.setNotificationPane(myNotificationPane);
    return storagePane.buildStoragePane(mode);
  }

  public enum Mode {
    OPEN, SAVE
  }

  public interface DialogUi {
    void close();

    void resize();

    void error(Throwable e);

    void error(String s);

    void message(String message);

    void showBusyIndicator(boolean shown);
  }

  public interface Ui {
    String getCategory();
    default String getId() { return getCategory(); }
    Pane createUi();

    String getName();

    Optional<Pane> createSettingsUi();
  }
}
