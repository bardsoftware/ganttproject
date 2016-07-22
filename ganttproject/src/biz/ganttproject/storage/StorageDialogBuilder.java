// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import biz.ganttproject.storage.cloud.GPCloudStorage;
import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.document.DocumentStorageUi;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author dbarashev@bardsoftware.com
 */
public class StorageDialogBuilder {
  private final GPCloudStorageOptions myCloudStorageOptions;
  private final DocumentStorageUi.DocumentReceiver myDocumentReceiver;
  private Button myActiveBtn;
  private Map<String, Supplier<Pane>> myStorageUiMap = Maps.newHashMap();
  private List<Ui> myStorageUiList = Lists.newArrayList();
  private @Nullable Dialog myDialog = null;

  public StorageDialogBuilder(@Nonnull IGanttProject project, ProjectUIFacade projectUi, DocumentManager documentManager, @Nonnull GPCloudStorageOptions cloudStorageOptions) {
    myCloudStorageOptions = Preconditions.checkNotNull(cloudStorageOptions);
    myDocumentReceiver = document -> {
      projectUi.openProject(documentManager.getProxyDocument(document), project);
      Objects.requireNonNull(myDialog, "Dialog is expected to be created by this moment").close();
    };
  }

  private void onStorageChange(BorderPane borderPane, String storageId) {
    Pane ui = myStorageUiMap.get(storageId).get();
    borderPane.setCenter(ui);
  }

  Dialog build() {
    ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
    Dialog<Void> dialog = new Dialog<>();
    dialog.getDialogPane().getButtonTypes().add(loginButtonType);

    BorderPane borderPane = new BorderPane();


    VBox servicesPane = new VBox();
    servicesPane.getStyleClass().add("pane-service-buttons");

    myStorageUiList.add(new GPCloudStorage(myCloudStorageOptions));
    myStorageUiList.forEach(storageUi -> {
      myStorageUiMap.put(storageUi.getId(), Suppliers.memoize(() -> storageUi.createUi(myDocumentReceiver)));
      Button btn = createButton(storageUi.getId(), event -> onStorageChange(borderPane, storageUi.getId()));
      servicesPane.getChildren().addAll(btn);
    });


    borderPane.setLeft(servicesPane);
    Pane emptyPane = new Pane();
    emptyPane.setPrefSize(600, 600);
    borderPane.setCenter(emptyPane);

    dialog.getDialogPane().getStylesheets().add("biz/ganttproject/storage/StorageDialog.css");
    dialog.getDialogPane().getStyleClass().add("body");
    dialog.getDialogPane().setContent(borderPane);
    dialog.initModality(Modality.WINDOW_MODAL);
    dialog.setTitle("My Projects");
    dialog.setResizable(true);
    dialog.setWidth(300);
    dialog.setHeight(300);
    dialog.setOnShown(event -> ((Button)servicesPane.getChildren().get(0)).fire());

    myDialog = dialog;
    return dialog;
  }

  private Button createButton(String key, EventHandler<ActionEvent> onClick) {
    String label = GanttLanguage.getInstance().getText(String.format("storageView.service.%s.label", key));
    Button btnService = new Button(label);
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

  public interface Ui {
    String getId();
    Pane createUi(DocumentStorageUi.DocumentReceiver documentReceiver);
  }
}
