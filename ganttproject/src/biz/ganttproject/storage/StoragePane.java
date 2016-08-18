// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import biz.ganttproject.storage.cloud.GPCloudStorage;
import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import biz.ganttproject.storage.local.LocalStorage;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.ReadOnlyProxyDocument;
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
  private Button myActiveBtn;
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


    BorderPane storagePane = new BorderPane();
    storagePane.getStyleClass().add("pane-service-buttons");
    VBox storageButtons = new VBox();
    storagePane.setCenter(storageButtons);
    Button addStorage = new Button("New Storage", new FontAwesomeIconView(FontAwesomeIcon.PLUS));
    storagePane.setBottom(new HBox(addStorage));

    Consumer<Document> openDocument = mode == StorageDialogBuilder.Mode.OPEN ? myDocumentReceiver : myDocumentUpdater;
    myStorageUiList.add(new LocalStorage(mode, myCurrentDocument, myDocumentReceiver));
    myStorageUiList.add(new GPCloudStorage(mode, myCloudStorageOptions, openDocument, myDialogUi));
    BorderPane storageUiPane = new BorderPane();
    storageUiPane.setBottom(myNotificationPane);
    myStorageUiList.forEach(storageUi -> {
      myStorageUiMap.put(storageUi.getId(), Suppliers.memoize(() -> storageUi.createUi()));
      Button btn = createButton(storageUi.getId(), event -> onStorageChange(storageUiPane, storageUi.getId()));
      storageButtons.getChildren().addAll(btn);
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

  private Button createButton(String key, EventHandler<ActionEvent> onClick) {
    String label = GanttLanguage.getInstance().getText(String.format("storageView.service.%s.label", key));
    String iconName = GanttLanguage.getInstance().getText(String.format("storageView.service.%s.icon", key));
    Button btnService = FontAwesomeIconFactory.get().createIconButton(
        FontAwesomeIcon.valueOf(iconName.toUpperCase()), label, "1em", "1em", ContentDisplay.LEFT);
    btnService.addEventHandler(ActionEvent.ACTION, event -> {
      btnService.getStyleClass().add("active");
      if (myActiveBtn != null) {
        myActiveBtn.getStyleClass().remove("active");
      }
      myActiveBtn = btnService;
    });
    btnService.addEventHandler(ActionEvent.ACTION, onClick);
    btnService.getStyleClass().add("btn-service");
    btnService.setMaxWidth(Double.MAX_VALUE);
    return btnService;
  }

  private void onStorageChange(BorderPane borderPane, String storageId) {
    Pane ui = myStorageUiMap.get(storageId).get();
    ui.getStyleClass().add("display-none");
    borderPane.setCenter(ui);
  }

  public void setNotificationPane(Node notificationPane) {
    myNotificationPane = notificationPane;
  }
}
