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
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.document.DocumentStorageUi;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import org.controlsfx.control.StatusBar;

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
  private EventHandler<ActionEvent> myOnNextClick;
  private StatusBar myNotificationPane;
  private DialogUi myDialogUi = new DialogUi() {
    @Override
    public void error(Throwable e) {
      setClass("alert-error");
      //myNotificationPane.setContent(createErrorPane(e.getMessage()));
      myNotificationPane.setText(e.getMessage());
      //myNotificationPane.setExpanded(true);
    }

    @Override
    public void error(String message) {
      setClass("alert-error");
      myNotificationPane.setText(message);
      //myNotificationPane.setContent(createErrorPane(message));
      //myNotificationPane.setExpanded(true);
    }

    @Override
    public void message(String message) {
      setClass("alert-info");
      myNotificationPane.setText(message);
//      myNotificationPane.setContent(createErrorPane(message));
//      myNotificationPane.setExpanded(true);
    }

    @Override
    public void showBusyIndicator(boolean show) {
      if (show) {
        myNotificationPane.setProgress(-1);
      } else {
        myNotificationPane.setProgress(0);
      }
    }

    @Override
    public void resize() {
      myDialog.getDialogPane().getScene().getWindow().sizeToScene();
    }

    @Override
    public void showNextButton(Runnable onClick) {
      myDialog.getDialogPane().getButtonTypes().add(ButtonType.NEXT);
      Node btn = myDialog.getDialogPane().lookupButton(ButtonType.NEXT);
      if (myOnNextClick != null) {
        btn.removeEventHandler(ActionEvent.ACTION, myOnNextClick);
      }
      myOnNextClick = (e) -> onClick.run();
      btn.addEventHandler(ActionEvent.ACTION, myOnNextClick);
    }

    private Node createErrorPane(String message) {
      Label result = new Label(message);
      result.getStyleClass().add("label");
      return result;
    }

    private void setClass(String className) {
      myNotificationPane.getStyleClass().clear();
      myNotificationPane.getStyleClass().add(className);
    }
  };


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
    ButtonType btnClose = new ButtonType("Close", ButtonBar.ButtonData.FINISH);
    Dialog<Void> dialog = new Dialog<>();
    myDialog = dialog;
    dialog.getDialogPane().getButtonTypes().add(btnClose);

    BorderPane borderPane = new BorderPane();


    VBox servicesPane = new VBox();
    servicesPane.getStyleClass().add("pane-service-buttons");

    myStorageUiList.add(new GPCloudStorage(myCloudStorageOptions));
    myStorageUiList.forEach(storageUi -> {
      myStorageUiMap.put(storageUi.getId(), Suppliers.memoize(() -> storageUi.createUi(myDocumentReceiver, myDialogUi)));
      Button btn = createButton(storageUi.getId(), event -> onStorageChange(borderPane, storageUi.getId()));
      servicesPane.getChildren().addAll(btn);
    });


    if (myStorageUiList.size() > 1) {
      borderPane.setLeft(servicesPane);
      Pane emptyPane = new Pane();
      emptyPane.setPrefSize(600, 600);
      borderPane.setCenter(emptyPane);
    } else {
      borderPane.setCenter(myStorageUiMap.get(myStorageUiList.get(0).getId()).get());
    }
    myNotificationPane = new StatusBar();
    myNotificationPane.getStyleClass().add("notification");
    myNotificationPane.setText("");
//    Platform.runLater(() -> {
//      Pane title = (Pane) myNotificationPane.lookup(".title");
//      if (title != null) {
//        title.setVisible(false);
//      }
//    });
    borderPane.setBottom(myNotificationPane);
    dialog.getDialogPane().getStylesheets().add("biz/ganttproject/storage/StorageDialog.css");
    dialog.getDialogPane().getStyleClass().add("body");

    dialog.getDialogPane().setContent(borderPane);
    dialog.initModality(Modality.WINDOW_MODAL);
    dialog.setTitle("GanttProject Cloud");
    dialog.setResizable(true);
    dialog.getDialogPane().getScene().getWindow().sizeToScene();
    dialog.setOnShown(event -> dialog.getDialogPane().getScene().getWindow().sizeToScene());
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

  public interface DialogUi {
    void resize();
    void showNextButton(Runnable onClick);
    void error(Throwable e);
    void error(String s);
    void message(String message);
    void showBusyIndicator(boolean shown);
  }
  public interface Ui {
    String getId();
    Pane createUi(DocumentStorageUi.DocumentReceiver documentReceiver, DialogUi dialogUi);
  }
}
