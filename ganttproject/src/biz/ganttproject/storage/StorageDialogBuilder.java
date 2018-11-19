// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import biz.ganttproject.FXUtil;
import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import com.google.common.base.Preconditions;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
import net.sourceforge.ganttproject.language.GanttLanguage;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.SegmentedButton;

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
  private NotificationPane myNotificationPane;
  private Node myOpenStorage;
  private Pane mySaveStorage;
  private Scene myScene;
  private UIFacade.Dialog myDialog;
  private JFXPanel myJfxPanel;

  public void setDialog(UIFacade.Dialog dlg) {
    myDialog = dlg;
    myScene.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ESCAPE) {
        dlg.hide();
      }
    });
    dlg.show();
    Platform.runLater(() -> {
      myJfxPanel.setScene(null);
      myJfxPanel.setScene(myScene);
    });
  }

  private DialogUi myDialogUi = new DialogUi() {

    @Override
    public void error(Throwable e) {
      TextArea notificationText = new TextArea(e.getMessage());
      notificationText.setWrapText(true);
      myNotificationPane.setContent(notificationText);
      myNotificationPane.show();
    }

    @Override
    public void error(String message) {
      myNotificationPane.setText(message);
      myNotificationPane.show();
    }

    @Override
    public void message(String message) {
      TextArea notificationText = new TextArea(message);
      notificationText.setWrapText(true);
      notificationText.setPrefRowCount(3);
      notificationText.getStyleClass().add("info");
      myNotificationPane.setGraphic(notificationText);
      myNotificationPane.show();
    }

    @Override
    public void close() {
      myDialog.hide();
    }

    @Override
    public void resize() {
      //
      if (myJfxPanel != null) {
        myJfxPanel.setScene(null);
        myJfxPanel.setScene(myScene);
        SwingUtilities.invokeLater(myDialog::layout);
      }
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
    myDocumentUpdater = document -> {
      SwingUtilities.invokeLater(() -> {
        if (project.getDocument() == null) {
          project.setDocument(documentManager.getProxyDocument(document));
        } else {
          project.getDocument().setMirror(document);
        }
        projectUi.saveProject(project);
      });
    };
    myProject = project;
  }

  JFXPanel build() {
    BorderPane borderPane = new BorderPane();
    borderPane.getStyleClass().add("body");

    borderPane.getStyleClass().add("pane-storage");
    borderPane.setCenter(new Pane());
    ToggleButton btnSave = new ToggleButton(GanttLanguage.getInstance().getText("myProjects.save"));
    ToggleButton btnOpen = new ToggleButton(GanttLanguage.getInstance().getText("myProjects.open"));

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

    myScene = new Scene(borderPane);
    myScene.getStylesheets().add("biz/ganttproject/storage/StorageDialog.css");
    JFXPanel jfxPanel = new JFXPanel();
    jfxPanel.setScene(myScene);

    if (myProject.isModified()) {
      btnSave.fire();
    } else {
      btnOpen.fire();
    }


    myJfxPanel = jfxPanel;
    return jfxPanel;
  }

  private void showOpenStorageUi(BorderPane container) {
    if (myOpenStorage == null) {
      Pane storagePane = buildStoragePane(Mode.OPEN);
      myNotificationPane = new NotificationPane(storagePane);
      myNotificationPane.getStyleClass().addAll(
          NotificationPane.STYLE_CLASS_DARK);
      myOpenStorage = myNotificationPane;
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
  }

  public interface Ui {
    String getCategory();

    default String getId() {
      return getCategory();
    }

    Pane createUi();

    String getName();

    Optional<Pane> createSettingsUi();
  }
}
