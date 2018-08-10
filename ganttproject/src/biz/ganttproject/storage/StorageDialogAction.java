// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage;

import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author dbarashev@bardsoftware.com
 */
public class StorageDialogAction extends GPAction {
  private final GPCloudStorageOptions myCloudStorageOptions;
  private final ProjectUIFacade myProjectUiFacade;
  private final IGanttProject myProject;
  private final DocumentManager myDocumentManager;
  private final UIFacade myUiFacade;

  public StorageDialogAction(IGanttProject project, UIFacade uiFacade, ProjectUIFacade projectUIFacade,
                             DocumentManager documentManager, GPCloudStorageOptions cloudStorageOptions) {
    super("myProjects.action");
    myProject = project;
    myUiFacade = uiFacade;
    myCloudStorageOptions = cloudStorageOptions;
    myProjectUiFacade = projectUIFacade;
    myDocumentManager = documentManager;
  }

  @Override
  public void actionPerformed(ActionEvent actionEvent) {

    UIUtil.initJavaFx(() -> Platform.runLater(() -> {

      StorageDialogBuilder dialogBuilder = new StorageDialogBuilder(myProject, myProjectUiFacade, myDocumentManager, myCloudStorageOptions);
      JFXPanel contentPane = dialogBuilder.build();
      //dialogBuilder
      SwingUtilities.invokeLater(() -> {
        UIFacade.Dialog dlg = myUiFacade.createDialog(contentPane, new Action[0], getI18n("myProjects.title"));
        dialogBuilder.setDialog(dlg);
      });
      //dialog.show();
    }));
  }
}
