// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import javafx.application.Platform;
import javafx.scene.control.Dialog;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;

import java.awt.event.ActionEvent;

/**
 * @author dbarashev@bardsoftware.com
 */
public class StorageDialogAction extends GPAction {
  private final GPCloudStorageOptions myCloudStorageOptions;
  private final ProjectUIFacade myProjectUiFacade;
  private final IGanttProject myProject;
  private final DocumentManager myDocumentManager;

  public StorageDialogAction(IGanttProject project, ProjectUIFacade projectUIFacade,
                             DocumentManager documentManager, GPCloudStorageOptions cloudStorageOptions) {
    super("Go Online...");
    myProject = project;
    myCloudStorageOptions = cloudStorageOptions;
    myProjectUiFacade = projectUIFacade;
    myDocumentManager = documentManager;
  }
  @Override
  public void actionPerformed(ActionEvent actionEvent) {
    Platform.runLater(() -> {
      Dialog dialog = new StorageDialogBuilder(myProject, myProjectUiFacade, myDocumentManager, myCloudStorageOptions).build();
      dialog.showAndWait();
    });
  }
}
