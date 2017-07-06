// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.webdav;

import biz.ganttproject.FXUtil;
import biz.ganttproject.storage.BreadcrumbView;
import biz.ganttproject.storage.FolderItem;
import biz.ganttproject.storage.FolderView;
import biz.ganttproject.storage.StorageDialogBuilder;
import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import com.google.common.base.Strings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavStorage implements StorageDialogBuilder.Ui {
  static class WebDavResourceAsFolderItem implements FolderItem {
    private final WebDavResource myResource;

    WebDavResourceAsFolderItem(WebDavResource resource) {
      myResource = resource;
    }
    @Override
    public boolean isLocked() {
      try {
        return myResource.isLocked();
      } catch (WebDavResource.WebDavException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean isLockable() {
      return myResource.isLockSupported(true);
    }

    @NotNull
    @Override
    public String getName() {
      return myResource.getName();
    }

    @Override
    public boolean isDirectory() {
      try {
        return myResource.isCollection();
      } catch (WebDavResource.WebDavException e) {
        throw new RuntimeException(e);
      }
    }
  }
  private final GanttLanguage i18n = GanttLanguage.getInstance();
  private final StorageDialogBuilder.Mode myMode;
  private final Consumer<Document> myOpenDocument;
  private final StorageDialogBuilder.DialogUi myDialogUi;
  private final GPCloudStorageOptions myOptions;
  private WebdavLoadService myLoadService;
  private WebDavServerDescriptor myServer;
  private BreadcrumbView myBreadcrumbs;
  private Consumer<Path> myOnSelectCrumb;
  private WebDavResource myCurrentFolder;
  private StringProperty myFilename;
  private FolderView<WebDavResourceAsFolderItem> myListView;
  private BorderPane myBorderPane;

  public WebdavStorage(StorageDialogBuilder.Mode mode, Consumer<Document> openDocument,
                       StorageDialogBuilder.DialogUi dialogUi, GPCloudStorageOptions options) {
    myMode = mode;
    myOpenDocument = openDocument;
    myDialogUi = dialogUi;
    myOptions = options;
  }

  public WebdavStorage(WebDavServerDescriptor server, StorageDialogBuilder.Mode mode, Consumer<Document> openDocument,
                       StorageDialogBuilder.DialogUi dialogUi, GPCloudStorageOptions options) {
    this(mode, openDocument, dialogUi, options);
    setServer(server);
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
  public String getCategory() {
    return "webdav";
  }

  @Override
  public String getId() {
    return myServer.getRootUrl();
  }

  @Override
  public Pane createUi() {
    myBorderPane = new BorderPane();
    if (Strings.isNullOrEmpty(myServer.getPassword())) {
      myBorderPane.setCenter(createPasswordUi());
    } else {
      myBorderPane.setCenter(createStorageUi());
    }
    return myBorderPane;
  }

  private Pane createPasswordUi() {
    WebdavPasswordPane passwordPane = new WebdavPasswordPane(myServer, this::onPasswordEntered);
    return passwordPane.createUi();
  }

  private void onPasswordEntered(WebDavServerDescriptor server) {
    setServer(server);
    FXUtil.transitionCenterPane(myBorderPane, createUi(), myDialogUi::resize);
  }


  private Pane createStorageUi() {
    VBox rootPane = new VBox();
    rootPane.getStyleClass().add("pane-service-contents");
    rootPane.setPrefWidth(400);


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
    topPane.getStyleClass().add("title");
    Label title = new Label(i18n.formatText(String.format("webdav.ui.title.%s", myMode.name().toLowerCase()), myServer.name));
    topPane.getChildren().add(title);

    HBox.setHgrow(buttonBar, Priority.ALWAYS);
    topPane.getChildren().add(buttonBar);
    rootPane.getChildren().add(topPane);

    myOnSelectCrumb = selectedPath -> {
      ObservableList<WebDavResourceAsFolderItem> wrappers = FXCollections.observableArrayList();
      Consumer<ObservableList<WebDavResource>> consumer = webDavResources -> {
        webDavResources.forEach(resource -> wrappers.add(new WebDavResourceAsFolderItem(resource)));
        myListView.setResources(wrappers);
      };
      loadFolder(selectedPath,
          myDialogUi::showBusyIndicator,
          consumer, myDialogUi);
    };
    myBreadcrumbs = new BreadcrumbView(Paths.get("/", myServer.name), myOnSelectCrumb);


    rootPane.getChildren().add(myBreadcrumbs.getBreadcrumbs());

    SimpleBooleanProperty isLockingSupported = new SimpleBooleanProperty();
    isLockingSupported.addListener((observable, oldValue, newValue) -> {
      System.err.println("is locking supported="+newValue);
    });
    myListView = new FolderView<>(myDialogUi, this::deleteResource, this::toggleLockResource, isLockingSupported);
    rootPane.getChildren().add(myListView.getListView());


    myListView.getListView().setOnMouseClicked(event -> myListView.getSelectedResource().ifPresent(selectedItem -> {
      WebDavResource selectedResource = selectedItem.myResource;
      try {
        if (!selectedResource.isCollection()) {
          myFilename.setValue(selectedResource.getName());
        }
        if (event.getClickCount() == 2) {
          openResource();
        }
      } catch (IOException | WebDavResource.WebDavException e) {
        myDialogUi.error(e);
      }
    }));

    return rootPane;
  }

  private void openResource() throws WebDavResource.WebDavException, IOException {
    if (myListView.getSelectedResource().isPresent()) {
      WebDavResource selectedItem = myListView.getSelectedResource().get().myResource;
      if (selectedItem.isCollection()) {
        myBreadcrumbs.append(selectedItem.getName());
      } else {
        myOpenDocument.accept(createDocument(selectedItem));
      }
    }
  }

  private void deleteResource() {
    myListView.getSelectedResource().ifPresent(folderItem -> {
      WebDavResource resource = folderItem.myResource;
      try {
        resource.delete();
      } catch (WebDavResource.WebDavException e) {
        myDialogUi.error(e);
      }
    });
  }

  private void toggleLockResource() {
    myListView.getSelectedResource().ifPresent(folderItem -> {
      try {
        WebDavResource resource = folderItem.myResource;
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
  private void loadFolder(Path path, Consumer<Boolean> showMaskPane, Consumer<ObservableList<WebDavResource>> setResult, StorageDialogBuilder.DialogUi dialogUi) {
    if (path.getNameCount() > 1) {
      path = path.subpath(1, path.getNameCount());
    } else {
      path = path.getRoot();
    }
    myLoadService.setPath(path.toString().toString());
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

  @Override
  public Optional<Pane> createSettingsUi() {
    if (myServer == null) {
      return Optional.of(new Pane());
    }
    Consumer<WebDavServerDescriptor> updater = server -> {
      if (server == null) {
        myOptions.removeValue(myServer);
      } else {
        myOptions.updateValue(myServer, server);
      }
    };
    return Optional.of(new WebdavServerSetupPane(myServer, updater, true).createUi());
  }

}
