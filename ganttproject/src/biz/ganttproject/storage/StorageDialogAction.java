// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import javafx.application.Platform;
import javafx.scene.control.Dialog;
import net.sourceforge.ganttproject.action.GPAction;

import java.awt.event.ActionEvent;

/**
 * @author dbarashev@bardsoftware.com
 */
public class StorageDialogAction extends GPAction {
  private final GPCloudStorageOptions myCloudStorageOptions;

  public StorageDialogAction(GPCloudStorageOptions cloudStorageOptions) {
    super("Go Online...");
    myCloudStorageOptions = cloudStorageOptions;
  }
  @Override
  public void actionPerformed(ActionEvent actionEvent) {
    Platform.runLater(() -> {
      Dialog dialog = new StorageDialogBuilder(myCloudStorageOptions).build();
      dialog.showAndWait();
    });
  }
}
