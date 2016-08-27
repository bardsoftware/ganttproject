// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import biz.ganttproject.FXUtil;
import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import com.google.common.base.Preconditions;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.document.ReadOnlyProxyDocument;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import org.controlsfx.control.SegmentedButton;
import org.controlsfx.control.StatusBar;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
  private
  @Nullable
  Dialog myDialog = null;
  private EventHandler<ActionEvent> myOnNextClick;
  private Pane myOpenStorage;
  private Pane mySaveStorage;

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
    public void resize() {
      myDialog.getDialogPane().getScene().getWindow().sizeToScene();
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

  Dialog build() {
    Dialog<Void> dialog = new Dialog<>();
    myDialog = dialog;
    Window window = dialog.getDialogPane().getScene().getWindow();
    window.setOnCloseRequest(event -> window.hide());

    dialog.getDialogPane().getStylesheets().add("biz/ganttproject/storage/StorageDialog.css");
    dialog.getDialogPane().getStyleClass().add("body");

    BorderPane borderPane = new BorderPane();
    borderPane.getStyleClass().add("pane-storage");
    borderPane.setCenter(new Pane());

    {
      VBox titleBox = new VBox();
      titleBox.getStyleClass().add("title-box");
      Label projectName = new Label(myProject.getProjectName());

      SegmentedButton buttonBar = new SegmentedButton();
      buttonBar.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
      ToggleButton btnOpen = new ToggleButton("Open other project");
      btnOpen.addEventHandler(ActionEvent.ACTION, e -> showOpenStorageUi(borderPane));
//
      ToggleButton btnSave = new ToggleButton("Save project as");
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
    //borderPane.setBottom(myNotificationPane);

    dialog.getDialogPane().setContent(borderPane);
    dialog.initModality(Modality.WINDOW_MODAL);
    dialog.setTitle("GanttProject Cloud");
    dialog.setResizable(true);
    dialog.getDialogPane().getScene().getWindow().sizeToScene();

    dialog.setOnShown(event -> dialog.getDialogPane().getScene().getWindow().sizeToScene());
    return dialog;
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
    StoragePane storagePane = new StoragePane(myCloudStorageOptions, new ReadOnlyProxyDocument(myProject.getDocument()), myDocumentReceiver, myDocumentUpdater, myDialogUi);
    storagePane.setNotificationPane(myNotificationPane);
    return storagePane.buildStoragePane(mode);
  }

  public enum Mode {
    OPEN, SAVE
  }

  public interface DialogUi {
    void resize();

    void error(Throwable e);

    void error(String s);

    void message(String message);

    void showBusyIndicator(boolean shown);
  }

  public interface Ui {
    String getId();

    Pane createUi();

    String getName();

    Optional<Pane> createSettingsUi();
  }
}
