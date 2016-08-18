// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.webdav;

import biz.ganttproject.storage.StorageDialogBuilder;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.webdav.HttpDocument;
import net.sourceforge.ganttproject.document.webdav.WebDavResource;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import net.sourceforge.ganttproject.language.GanttLanguage;
import org.controlsfx.control.BreadCrumbBar;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavStorage implements StorageDialogBuilder.Ui {
  private final GanttLanguage i18n = GanttLanguage.getInstance();
  private final StorageDialogBuilder.Mode myMode;
  private final Consumer<Document> myOpenDocument;
  private final StorageDialogBuilder.DialogUi myDialogUi;
  private WebdavLoadService myLoadService;
  private WebDavServerDescriptor myServer;
  private BreadCrumbBar<BreadCrumbNode> myBreadcrumbs;
  private Consumer<TreeItem<BreadCrumbNode>> myOnSelectCrumb;
  private WebDavResource myCurrentFolder;
  private StringProperty myFilename;
  private WebdavResourceListView myListView;

  public WebdavStorage(StorageDialogBuilder.Mode mode, Consumer<Document> openDocument, StorageDialogBuilder.DialogUi dialogUi) {
    myMode = mode;
    myOpenDocument = openDocument;
    myDialogUi = dialogUi;
  }

  public void setServer(WebDavServerDescriptor webdavServer) {
    myLoadService = new WebdavLoadService(webdavServer);
    myServer = webdavServer;
  }

  @Override
  public String getName() {
    return myServer.getName();
  }

  @Override
  public String getId() {
    return "webdav";
  }

  static class BreadCrumbNode {
    private String path;
    private String label;

    BreadCrumbNode(String path, String label) {
      this.path = path;
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  @Override
  public Pane createUi() {
    VBox rootPane = new VBox();
    rootPane.getStyleClass().add("pane-service-contents");
    rootPane.setPrefWidth(400);

    Label title = new Label(i18n.formatText(String.format("webdav.ui.title.%s", myMode.name().toLowerCase()), myServer.name));
    title.getStyleClass().add("title");

    HBox buttonBar = new HBox();
    buttonBar.getStyleClass().add("webdav-button-pane");
    TextField filename = new TextField();
    myFilename = filename.textProperty();

    switch (myMode) {
      case OPEN:
        filename.setDisable(true);
        Button btnOpen = new Button("Open");
        btnOpen.addEventHandler(ActionEvent.ACTION, event -> {
          try {
            openResource();
          } catch (IOException | WebDavResource.WebDavException e) {
            myDialogUi.error(e);
          }
        });
        buttonBar.getChildren().add(btnOpen);
        break;
      case SAVE:
        Button btnSave = new Button("Save");
        btnSave.addEventHandler(ActionEvent.ACTION, event -> {
          try {
            Document document = createDocument(myLoadService.createResource(myCurrentFolder, myFilename.getValue()));
            myOpenDocument.accept(document);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
        buttonBar.getChildren().add(filename);
        buttonBar.getChildren().add(btnSave);
    }

    HBox topPane = new HBox();
    topPane.getStyleClass().add("title-pane");

    topPane.getChildren().add(title);
    HBox.setHgrow(buttonBar, Priority.ALWAYS);
    topPane.getChildren().add(buttonBar);
    rootPane.getChildren().add(topPane);

    myBreadcrumbs = new BreadCrumbBar<>();
    myBreadcrumbs.getStyleClass().add("breadcrumb");

    rootPane.getChildren().add(myBreadcrumbs);

    SimpleBooleanProperty isLockingSupported = new SimpleBooleanProperty();
    isLockingSupported.addListener((observable, oldValue, newValue) -> {
      System.err.println("is locking supported="+newValue);
    });
    myListView = new WebdavResourceListView(myDialogUi, this::deleteResource, this::toggleLockResource, isLockingSupported);
    rootPane.getChildren().add(myListView.getListView());

    TreeItem<BreadCrumbNode> rootItem = new TreeItem<>(new BreadCrumbNode("/", myServer.name));
    myOnSelectCrumb = selectedCrumb -> {
      selectedCrumb.getChildren().clear();
      loadFolder(selectedCrumb.getValue().path, myDialogUi::showBusyIndicator, myListView::setResources, myDialogUi);
    };
    myBreadcrumbs.setOnCrumbAction(value -> myOnSelectCrumb.accept(value.getSelectedCrumb()));
    myBreadcrumbs.setSelectedCrumb(rootItem);
    myOnSelectCrumb.accept(rootItem);

    myListView.getListView().setOnMouseClicked(event -> {
      myListView.getSelectedResource().ifPresent(selectedItem -> {
        try {
          if (!selectedItem.isCollection()) {
            myFilename.setValue(selectedItem.getName());
          }
          if (event.getClickCount() == 2) {
            openResource();
          }
        } catch (IOException | WebDavResource.WebDavException e) {
          myDialogUi.error(e);
        }
      });
    });

    return rootPane;
  }

  private void openResource() throws WebDavResource.WebDavException, IOException {
    if (myListView.getSelectedResource().isPresent()) {
      WebDavResource selectedItem = myListView.getSelectedResource().get();
      if (selectedItem.isCollection()) {
        BreadCrumbNode crumbNode = new BreadCrumbNode(selectedItem.getAbsolutePath(), selectedItem.getName());
        TreeItem<BreadCrumbNode> treeItem = new TreeItem<>(crumbNode);
        myBreadcrumbs.getSelectedCrumb().getChildren().add(treeItem);
        myBreadcrumbs.setSelectedCrumb(treeItem);
        myOnSelectCrumb.accept(treeItem);
      } else {
        myOpenDocument.accept(createDocument(selectedItem));
      }
    }
  }

  void deleteResource() {
    myListView.getSelectedResource().ifPresent(resource -> {
      try {
        resource.delete();
      } catch (WebDavResource.WebDavException e) {
        myDialogUi.error(e);
      }
    });
  }

  void toggleLockResource() {
    myListView.getSelectedResource().ifPresent(resource -> {
      try {
        if (resource.isLocked()) {
          resource.unlock();
        } else {
          resource.lock(-1);
        }
      } catch (WebDavResource.WebDavException e) {
        myDialogUi.error(e);
      }
    });
  }
  private void loadFolder(String path, Consumer<Boolean> showMaskPane, Consumer<ObservableList<WebDavResource>> setResult, StorageDialogBuilder.DialogUi dialogUi) {
    myLoadService.setPath(path);
    myCurrentFolder = myLoadService.createRootResource();
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
