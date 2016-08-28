// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import biz.ganttproject.FXUtil;
import biz.ganttproject.storage.cloud.GPCloudStorage;
import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import biz.ganttproject.storage.local.LocalStorage;
import biz.ganttproject.storage.webdav.WebdavServerSetupPane;
import biz.ganttproject.storage.webdav.WebdavStorage;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.*;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.ReadOnlyProxyDocument;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import net.sourceforge.ganttproject.language.GanttLanguage;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author dbarashev@bardsoftware.com
 */
public class StoragePane {
  private final GPCloudStorageOptions myCloudStorageOptions;
  private final Consumer<Document> myDocumentReceiver;
  private final Consumer<Document> myDocumentUpdater;
  private final StorageDialogBuilder.DialogUi myDialogUi;
  private final ReadOnlyProxyDocument myCurrentDocument;
  private Node myActiveStorageLabel;
  private Map<String, Supplier<Pane>> myStorageUiMap = Maps.newHashMap();
  private List<StorageDialogBuilder.Ui> myStorageUiList = Lists.newArrayList();
  private Node myNotificationPane;

  StoragePane(GPCloudStorageOptions options, ReadOnlyProxyDocument currentDocument, Consumer<Document> openDocument, Consumer<Document> updateDocument, StorageDialogBuilder.DialogUi dialogUi) {
    myCloudStorageOptions = options;
    myCurrentDocument = currentDocument;
    myDocumentReceiver = openDocument;
    myDocumentUpdater = updateDocument;
    myDialogUi = dialogUi;
  }

  BorderPane buildStoragePane(StorageDialogBuilder.Mode mode) {
    BorderPane borderPane = new BorderPane();

    BorderPane storageUiPane = new BorderPane();

    BorderPane storagePane = new BorderPane();
    storagePane.getStyleClass().add("pane-service-buttons");
    VBox storageButtons = new VBox();
    storagePane.setCenter(storageButtons);
    Button addStorage = new Button("New Storage", new FontAwesomeIconView(FontAwesomeIcon.PLUS));
    addStorage.addEventHandler(ActionEvent.ACTION, event -> onNewWebdavServer(storageUiPane));
    storagePane.setBottom(new HBox(addStorage));

    Consumer<Document> openDocument = mode == StorageDialogBuilder.Mode.OPEN ? myDocumentReceiver : myDocumentUpdater;
    myStorageUiList.add(new LocalStorage(mode, myCurrentDocument, myDocumentReceiver));
    myStorageUiList.add(new GPCloudStorage(mode, myCloudStorageOptions, openDocument, myDialogUi));
    for (WebDavServerDescriptor server : myCloudStorageOptions.getWebdavServers()) {
      WebdavStorage webdavStorageUi = new WebdavStorage(server, mode, openDocument, myDialogUi);
      myStorageUiList.add(webdavStorageUi);
    }

    storageUiPane.setBottom(myNotificationPane);
    myStorageUiList.forEach(storageUi -> {
      myStorageUiMap.put(storageUi.getId(), Suppliers.memoize(() -> storageUi.createUi()));
      Pane btnPane = createButton(storageUi,
          event -> onStorageChange(storageUiPane, storageUi.getId()),
          settingsPane -> FXUtil.transitionCenterPane(storageUiPane, settingsPane, myDialogUi::resize));
      storageButtons.getChildren().addAll(btnPane);
    });
    storageUiPane.setPrefSize(400, 400);
    borderPane.setCenter(storageUiPane);

    if (myStorageUiList.size() > 1) {
      borderPane.setLeft(storagePane);
    } else {
      storageUiPane.setCenter(myStorageUiMap.get(myStorageUiList.get(0).getId()).get());
    }
    return borderPane;
  }

  private Pane createButton(StorageDialogBuilder.Ui storageUi, EventHandler<ActionEvent> onClick, Consumer<Pane> onSettingsClick) {
    HBox result = new HBox();

    String label = GanttLanguage.getInstance().formatText(String.format("storageView.service.%s.label", storageUi.getId()), storageUi.getName());
    String iconName = GanttLanguage.getInstance().getText(String.format("storageView.service.%s.icon", storageUi.getId()));
    Button btnService = FontAwesomeIconFactory.get().createIconButton(
        FontAwesomeIcon.valueOf(iconName.toUpperCase()), label, "1em", "1em", ContentDisplay.LEFT);
    btnService.getStyleClass().add("storage-name");
    btnService.addEventHandler(ActionEvent.ACTION, event -> {
      btnService.getParent().getStyleClass().add("active");
      if (myActiveStorageLabel != null) {
        myActiveStorageLabel.getStyleClass().remove("active");
      }
      myActiveStorageLabel = btnService.getParent();
    });
    btnService.addEventHandler(ActionEvent.ACTION, onClick);
    btnService.setMaxWidth(Double.MAX_VALUE);

    HBox.setHgrow(btnService, Priority.ALWAYS);
    result.getStyleClass().add("btn-service");
    result.getChildren().addAll(btnService);

    storageUi.createSettingsUi().ifPresent(settingsPane -> {
      Button btnSettings = FontAwesomeIconFactory.get().createIconButton(FontAwesomeIcon.COG, "", "100%", "100%", ContentDisplay.GRAPHIC_ONLY);
      btnSettings.getStyleClass().add("settings");
      btnSettings.addEventHandler(ActionEvent.ACTION, event -> onSettingsClick.accept(settingsPane));
      result.getChildren().addAll(btnSettings);
    });
    return result;
  }

  private void onStorageChange(BorderPane borderPane, String storageId) {
    Pane ui = myStorageUiMap.get(storageId).get();
    FXUtil.transitionCenterPane(borderPane, ui, myDialogUi::resize);
    //ui.getStyleClass().add("display-none");
    //borderPane.setCenter(ui);
  }

  private void onNewWebdavServer(BorderPane borderPane) {
    WebDavServerDescriptor newServer = new WebDavServerDescriptor();
    WebdavServerSetupPane setupPane = new WebdavServerSetupPane(newServer, myCloudStorageOptions::addWebdavServer);
    FXUtil.transitionCenterPane(borderPane, setupPane.createUi(), myDialogUi::resize);
  }

  public void setNotificationPane(Node notificationPane) {
    myNotificationPane = notificationPane;
  }
}
