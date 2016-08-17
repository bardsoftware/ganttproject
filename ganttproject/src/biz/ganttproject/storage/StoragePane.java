// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import biz.ganttproject.storage.cloud.GPCloudStorage;
import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import net.sourceforge.ganttproject.document.Document;
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
  private Button myActiveBtn;
  private Map<String, Supplier<Pane>> myStorageUiMap = Maps.newHashMap();
  private List<StorageDialogBuilder.Ui> myStorageUiList = Lists.newArrayList();
  private Node myNotificationPane;

  StoragePane(GPCloudStorageOptions options, Consumer<Document> openDocument, Consumer<Document> updateDocument, StorageDialogBuilder.DialogUi dialogUi) {
    myCloudStorageOptions = options;
    myDocumentReceiver = openDocument;
    myDocumentUpdater = updateDocument;
    myDialogUi = dialogUi;
  }

  BorderPane buildStoragePane(StorageDialogBuilder.Mode mode) {
    BorderPane borderPane = new BorderPane();


    VBox servicesPane = new VBox();
    servicesPane.getStyleClass().add("pane-service-buttons");
    servicesPane.setMaxHeight(Double.MAX_VALUE);


    Consumer<Document> openDocument = mode == StorageDialogBuilder.Mode.OPEN ? myDocumentReceiver : myDocumentUpdater;
    myStorageUiList.add(new GPCloudStorage(mode, myCloudStorageOptions, openDocument, myDialogUi));
    myStorageUiList.add(new StorageDialogBuilder.Ui() {

      @Override
      public String getId() {
        return "desktop";
      }

      @Override
      public Pane createUi() {
        return new Pane();
      }
    });
    BorderPane storageUiPane = new BorderPane();
    storageUiPane.setBottom(myNotificationPane);
    myStorageUiList.forEach(storageUi -> {
      myStorageUiMap.put(storageUi.getId(), Suppliers.memoize(() -> storageUi.createUi()));
      Button btn = createButton(storageUi.getId(), event -> onStorageChange(storageUiPane, storageUi.getId()));
      servicesPane.getChildren().addAll(btn);
    });
    storageUiPane.setPrefSize(400, 400);
    borderPane.setCenter(storageUiPane);

    if (myStorageUiList.size() > 1) {
      borderPane.setLeft(servicesPane);
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
