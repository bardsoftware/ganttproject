// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.storage.StorageDialogBuilder;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentStorageUi;
import net.sourceforge.ganttproject.document.webdav.HttpDocument;
import net.sourceforge.ganttproject.document.webdav.WebDavResource;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import org.controlsfx.control.BreadCrumbBar;
import org.controlsfx.control.MaskerPane;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavStorage implements StorageDialogBuilder.Ui {
  private final DocumentStorageUi.DocumentReceiver myDocumentReceiver;
  private final StorageDialogBuilder.DialogUi myDialogUi;
  private WebdavLoadService myLoadService;
  private WebDavServerDescriptor myServer;

  public WebdavStorage(DocumentStorageUi.DocumentReceiver documentReceiver, StorageDialogBuilder.DialogUi dialogUi) {
    myDocumentReceiver = documentReceiver;
    myDialogUi = dialogUi;
  }

  void setServer(WebDavServerDescriptor webdavServer) {
    myLoadService = new WebdavLoadService(webdavServer);
    myServer = webdavServer;
  }
  @Override
  public String getId() {
    return null;
  }

  @Override
  public Pane createUi() {
    VBox rootPane = new VBox();
    rootPane.getStyleClass().add("pane-service-contents");
    rootPane.setPrefWidth(400);
    class BreadCrumbNode {
      private String path;
      private String label;
      BreadCrumbNode(String path, String label) { this.path = path; this.label = label; }

      @Override
      public String toString() {
        return this.label;
      }
    }
    BreadCrumbBar<BreadCrumbNode> breadcrumbs = new BreadCrumbBar<>();
    breadcrumbs.getStyleClass().add("breadcrumb");

    rootPane.getChildren().add(breadcrumbs);

    ListView<WebDavResource> filesTable = new ListView<>();
    filesTable.setCellFactory(param -> new ListCell<WebDavResource>() {
      @Override
      protected void updateItem(WebDavResource item, boolean empty) {
        if (item == null) {
          setText("");
        } else {
          super.updateItem(item, empty);
          if (empty) {
            setGraphic(null);
          } else {
            setText(item.getName());
          }
        }
      }
    });
    rootPane.getChildren().add(filesTable);
    StackPane stackPane = new StackPane();
    MaskerPane maskerPane = new MaskerPane();
    stackPane.getChildren().addAll(rootPane, maskerPane);

    TreeItem<BreadCrumbNode> rootItem = new TreeItem<>(new BreadCrumbNode("/", myServer.name));
    Consumer<TreeItem<BreadCrumbNode>> handler = selectedCrumb -> {
      selectedCrumb.getChildren().clear();
      loadFolder(selectedCrumb.getValue().path, maskerPane::setVisible, filesTable::setItems, myDialogUi);
    };
    breadcrumbs.setOnCrumbAction(value -> handler.accept(value.getSelectedCrumb()));
    breadcrumbs.setSelectedCrumb(rootItem);
    handler.accept(rootItem);

    filesTable.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        try {
          WebDavResource selectedItem = filesTable.getSelectionModel().getSelectedItem();
          if (selectedItem.isCollection()) {
            BreadCrumbNode crumbNode = new BreadCrumbNode(selectedItem.getAbsolutePath(), selectedItem.getName());
            TreeItem<BreadCrumbNode> treeItem = new TreeItem<>(crumbNode);
            breadcrumbs.getSelectedCrumb().getChildren().add(treeItem);
            breadcrumbs.setSelectedCrumb(treeItem);
            handler.accept(treeItem);
          } else {
            myDocumentReceiver.setDocument(createDocument(selectedItem));
          }
        } catch (IOException | Document.DocumentException | WebDavResource.WebDavException e) {
          myDialogUi.error(e);
        }
      }
    });
    return stackPane;
  }

  private void loadFolder(String path, Consumer<Boolean> showMaskPane, Consumer<ObservableList<WebDavResource>> setResult, StorageDialogBuilder.DialogUi dialogUi) {
    myLoadService.setPath(path);
    myLoadService.setOnSucceeded((event) -> {
      Worker<ObservableList<WebDavResource>> source = event.getSource();
      setResult.accept(source.getValue());
      showMaskPane.accept(false);
    });
    myLoadService.setOnFailed((event) -> {
      showMaskPane.accept(false);
      dialogUi.error("WebdavService failed!");
    });
    myLoadService.setOnCancelled((event) -> {
      showMaskPane.accept(false);
      GPLogger.log("WebdavService cancelled!");
    });
    myLoadService.restart();
    showMaskPane.accept(true);
  }

  private Document createDocument(WebDavResource resource) throws IOException {
    return new HttpDocument(resource, myServer.getUsername(), myServer.getPassword(), HttpDocument.NO_LOCK);
  }
}
