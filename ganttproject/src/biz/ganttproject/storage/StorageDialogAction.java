// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import javafx.application.Platform;
import javafx.scene.control.Dialog;
import net.sourceforge.ganttproject.action.GPAction;

import java.awt.event.ActionEvent;

/**
 * @author dbarashev@bardsoftware.com
 */
public class StorageDialogAction extends GPAction {
  public StorageDialogAction() {
    super("My Projects");
  }
  @Override
  public void actionPerformed(ActionEvent actionEvent) {
    Platform.runLater(() -> {
      Dialog dialog = new StorageDialogBuilder().build();
      dialog.showAndWait();
    });
  }
}
