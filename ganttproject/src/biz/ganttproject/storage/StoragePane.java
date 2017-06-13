// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import biz.ganttproject.FXUtil;
import biz.ganttproject.lib.fx.ComponentsKt;
import biz.ganttproject.lib.fx.ListItemBuilder;
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
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import kotlin.Unit;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.document.ReadOnlyProxyDocument;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import net.sourceforge.ganttproject.language.GanttLanguage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private final DocumentManager myDocumentManager;
  private Node myActiveStorageLabel;
  private Map<String, Supplier<Pane>> myStorageUiMap = Maps.newHashMap();
  private List<StorageDialogBuilder.Ui> myStorageUiList = Lists.newArrayList();
  private Node myNotificationPane;
  private BorderPane storageUiPane = new BorderPane();
  StoragePane(GPCloudStorageOptions options,
              DocumentManager documentManager,
              ReadOnlyProxyDocument currentDocument,
              Consumer<Document> openDocument,
              Consumer<Document> updateDocument,
              StorageDialogBuilder.DialogUi dialogUi) {
    myCloudStorageOptions = options;
    myDocumentManager = documentManager;
    myCurrentDocument = currentDocument;
    myDocumentReceiver = openDocument;
    myDocumentUpdater = updateDocument;
    myDialogUi = dialogUi;
  }

  BorderPane buildStoragePane(StorageDialogBuilder.Mode mode) {
    BorderPane borderPane = new BorderPane();

    BorderPane storagePane = new BorderPane();
    storagePane.getStyleClass().add("pane-service-buttons");
    VBox storageButtons = new VBox();
    storagePane.setCenter(storageButtons);
    Button addStorage = new Button("New Storage", new FontAwesomeIconView(FontAwesomeIcon.PLUS));
    addStorage.addEventHandler(ActionEvent.ACTION, event -> onNewWebdavServer(storageUiPane));
    storagePane.setBottom(new HBox(addStorage));

    reloadStorageLabels(storageButtons, mode, Optional.empty());
    myCloudStorageOptions.getList().addListener((ListChangeListener<WebDavServerDescriptor>) c -> {
      reloadStorageLabels(storageButtons, mode, myActiveStorageLabel == null ? Optional.empty() : Optional.of(myActiveStorageLabel.getId()));
    });
    storageUiPane.setPrefSize(400, 400);
    borderPane.setCenter(storageUiPane);

    if (myStorageUiList.size() > 1) {
      borderPane.setLeft(storagePane);
    } else {
      storageUiPane.setCenter(myStorageUiMap.get(myStorageUiList.get(0).getCategory()).get());
    }
    return borderPane;
  }

  private void reloadStorageLabels(VBox storageButtons, StorageDialogBuilder.Mode mode, Optional<String> selectedId) {
    storageButtons.getChildren().clear();
    myStorageUiList.clear();
    myStorageUiMap.clear();
    GanttLanguage i18n = GanttLanguage.getInstance();

    Consumer<Document> doOpenDocument = mode == StorageDialogBuilder.Mode.OPEN ? myDocumentReceiver : myDocumentUpdater;
    Consumer<Document> openDocument = document -> {
      try {
        doOpenDocument.accept(document);
        myDialogUi.close();
      } catch (Exception e) {
        myDialogUi.error(e);
      }
    };
    myStorageUiList.add(new LocalStorage(
        mode == StorageDialogBuilder.Mode.OPEN ? new StorageMode.Open() : new StorageMode.Save(),
        myCurrentDocument,
        openDocument)
    );
    myStorageUiList.add(new RecentProjects(
        mode == StorageDialogBuilder.Mode.OPEN ? new StorageMode.Open() : new StorageMode.Save(),
        myDocumentManager,
        myCurrentDocument,
        openDocument)
    );
    myStorageUiList.add(new GPCloudStorage(mode, myCloudStorageOptions, openDocument, myDialogUi));
    for (WebDavServerDescriptor server : myCloudStorageOptions.getWebdavServers()) {
      WebdavStorage webdavStorageUi = new WebdavStorage(server, mode, openDocument, myDialogUi, myCloudStorageOptions);
      myStorageUiList.add(webdavStorageUi);
    }

    myStorageUiList.forEach(storageUi -> {
      myStorageUiMap.put(storageUi.getId(), Suppliers.memoize(() -> storageUi.createUi()));

      String itemLabel = i18n.formatText(
          String.format("storageView.service.%s.label", storageUi.getCategory()), storageUi.getName());
      String itemIcon = i18n.getText(
          String.format("storageView.service.%s.icon", storageUi.getCategory()));

      Node listItemContent = ComponentsKt.buildFontAwesomeButton(
          itemIcon,
          itemLabel,
          event -> onStorageChange(storageUiPane, storageUi.getId()),
          "storage-name");
      ListItemBuilder builder = new ListItemBuilder(listItemContent);
      builder.setOnSelectionChange(this::setSelected);

      storageUi.createSettingsUi().ifPresent(settingsPane -> {
        Node listItemOnHover = ComponentsKt.buildFontAwesomeButton(FontAwesomeIcon.COG.name(),
            null,
            event -> {
              FXUtil.transitionCenterPane(storageUiPane, settingsPane, myDialogUi::resize);
              return Unit.INSTANCE;
            },
            "settings"
        );
        builder.setHoverNode(listItemOnHover);
      });
      Pane btnPane = builder.build();
      btnPane.getStyleClass().add("btn-service");
      btnPane.setId(storageUi.getId());
      storageButtons.getChildren().addAll(btnPane);
      if (selectedId.isPresent() && selectedId.get().equals(storageUi.getId())) {
        setSelected(btnPane);
      }
    });
    selectedId.ifPresent(id -> onStorageChange(storageUiPane, id));
  }

  private Unit setSelected(Parent pane) {
    pane.getStyleClass().add("active");
    if (myActiveStorageLabel != null) {
      myActiveStorageLabel.getStyleClass().remove("active");
    }
    myActiveStorageLabel = pane;
    return Unit.INSTANCE;
  }

  private Unit onStorageChange(BorderPane borderPane, String storageId) {
    Pane ui = myStorageUiMap.get(storageId).get();
    FXUtil.transitionCenterPane(borderPane, ui, myDialogUi::resize);
    return Unit.INSTANCE;
    //ui.getStyleClass().add("display-none");
    //borderPane.setCenter(ui);
  }

  private void onNewWebdavServer(BorderPane borderPane) {
    WebDavServerDescriptor newServer = new WebDavServerDescriptor();
    WebdavServerSetupPane setupPane = new WebdavServerSetupPane(newServer, myCloudStorageOptions::addValue, false);
    FXUtil.transitionCenterPane(borderPane, setupPane.createUi(), myDialogUi::resize);
  }

  public void setNotificationPane(Node notificationPane) {
    myNotificationPane = notificationPane;
    storageUiPane.setBottom(notificationPane);
  }
}
