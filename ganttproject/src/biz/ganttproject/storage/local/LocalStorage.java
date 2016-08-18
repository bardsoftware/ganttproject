// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.local;

import biz.ganttproject.storage.StorageDialogBuilder;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.FileDocument;
import net.sourceforge.ganttproject.document.ReadOnlyProxyDocument;
import net.sourceforge.ganttproject.language.GanttLanguage;

import java.io.File;
import java.util.function.Consumer;

/**
 * @author dbarashev@bardsoftware.com
 */
public class LocalStorage implements StorageDialogBuilder.Ui {
  private final StorageDialogBuilder.Mode myMode;
  private final GanttLanguage i18n = GanttLanguage.getInstance();
  private final Consumer<Document> myDocumentReceiver;
  private final ReadOnlyProxyDocument myCurrentDocument;
  private File myResult;
  private StringProperty myFilePath;

  public LocalStorage(StorageDialogBuilder.Mode mode, ReadOnlyProxyDocument currentDocument, Consumer<Document> documentReceiver) {
    myMode = mode;
    myDocumentReceiver = documentReceiver;
    myCurrentDocument = currentDocument;
  }

  @Override
  public String getName() {
    return "This Computer";
  }

  @Override
  public String getId() {
    return "desktop";
  }

  private String i18nKey(String pattern) {
    return String.format(pattern, myMode.name().toLowerCase());
  }
  @Override
  public Pane createUi() {
    VBox rootPane = new VBox();
    rootPane.getStyleClass().addAll("pane-service-contents", "local-storage");
    rootPane.setPrefWidth(400);

    HBox titleBox = new HBox();
    titleBox.getStyleClass().add("title");
    Label title = new Label(i18n.getText(i18nKey("storageService.local.%s.title")));

    HBox openBox = new HBox();
    openBox.getStyleClass().add("open");
    Button btnOpen = new Button(i18n.getText(i18nKey("storage.action.%s")));
    btnOpen.addEventHandler(ActionEvent.ACTION, event -> {
      if (myResult != null) {
        myDocumentReceiver.accept(new FileDocument(myResult));
      }
    });
    openBox.getChildren().add(btnOpen);

    HBox.setHgrow(openBox, Priority.ALWAYS);
    titleBox.getChildren().addAll(title, openBox);

    Label filename = new Label();
    filename.getStyleClass().add("filename");
    myFilePath = filename.textProperty();
    myFilePath.setValue(myCurrentDocument.getFilePath());

    HBox browseBox = new HBox();
    browseBox.getStyleClass().add("browse");
    Button btnBrowse = new Button("Browse...");
    btnBrowse.addEventHandler(ActionEvent.ACTION, event -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle(i18nKey("storageService.local.%s.fileChooser.title"));
      fileChooser.getExtensionFilters().addAll(
          new FileChooser.ExtensionFilter("GanttProject Files", "*.gan"));
      myResult = fileChooser.showOpenDialog(null);
      if (myResult != null) {
        myFilePath.setValue(new FileDocument(myResult).getFilePath());
      }
    });
    browseBox.getChildren().addAll(btnBrowse);

    rootPane.getChildren().addAll(titleBox, filename, browseBox);
    return rootPane;
  }
}
