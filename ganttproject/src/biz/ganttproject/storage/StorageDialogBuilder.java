// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import biz.ganttproject.storage.cloud.GPCloudStorage;
import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import com.google.common.collect.Lists;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import net.sourceforge.ganttproject.language.GanttLanguage;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author dbarashev@bardsoftware.com
 */
public class StorageDialogBuilder {
  private final GPCloudStorageOptions myCloudStorageOptions;
  private Button myActiveBtn;

  class StorageUiConsumer implements Consumer<Pane> {
    private final Button myServiceButton;
    private final BorderPane myBorderPane;
    private Pane myPane;

    StorageUiConsumer(BorderPane borderPane, Button serviceButton) {
      myBorderPane = borderPane;
      myServiceButton = serviceButton;
    }
    @Override
    public void accept(Pane pane) {
      myPane = pane;
      if (StorageDialogBuilder.this.myActiveBtn == myServiceButton) {
        myBorderPane.setCenter(pane);
      }
    }
  }
  List<StorageUiConsumer> serviceUiConsumers = Lists.newArrayList();

  EventHandler<ActionEvent> onServiceChange = event -> {
    serviceUiConsumers.forEach(consumer -> {
      consumer.accept(consumer.myPane);
    });
  };

  public StorageDialogBuilder(GPCloudStorageOptions cloudStorageOptions) {
    myCloudStorageOptions = cloudStorageOptions;
  }

  Dialog build() {
    ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
    Dialog<Void> dialog = new Dialog<>();
    dialog.getDialogPane().getButtonTypes().add(loginButtonType);

    BorderPane borderPane = new BorderPane();


    VBox servicesPane = new VBox();
    servicesPane.setPadding(new Insets(10));
    servicesPane.setSpacing(8);

    Button cloudService = createButton("cloud", onServiceChange);
    StorageUiConsumer cloudConsumer = new StorageUiConsumer(borderPane, cloudService);
    serviceUiConsumers.add(cloudConsumer);
    GPCloudStorage cloudStorage = new GPCloudStorage(myCloudStorageOptions, cloudConsumer);
    cloudStorage.init();

    servicesPane.getChildren().addAll(cloudService);
    servicesPane.getStyleClass().add("pane-service-buttons");

    borderPane.setLeft(servicesPane);
    dialog.getDialogPane().getStylesheets().add("biz/ganttproject/storage/StorageDialog.css");
    dialog.getDialogPane().getStyleClass().add("body");
    dialog.getDialogPane().setContent(borderPane);
    dialog.initModality(Modality.WINDOW_MODAL);
    dialog.setTitle("My Projects");
    dialog.setResizable(true);
    dialog.setWidth(300);
    dialog.setHeight(300);
    cloudService.fire();

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

}
