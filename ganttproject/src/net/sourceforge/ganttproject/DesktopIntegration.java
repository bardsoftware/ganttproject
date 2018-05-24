// Copyright (C) 2018 BarD Software
package net.sourceforge.ganttproject;

import biz.ganttproject.desktop.DesktopAdapter;
import biz.ganttproject.desktop.GanttProjectApi;
import biz.ganttproject.desktop.QuitResponse;
import net.sourceforge.ganttproject.action.edit.SettingsDialogAction;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.about.AboutDialog2;

import java.io.File;
import java.io.IOException;

/**
 * @author dbarashev@bardsoftware.com
 */
public class DesktopIntegration {
  public static boolean isMacOs() {
    return System.getProperty("os.name").toLowerCase().startsWith("mac os x");
  }

  static void setup(final GanttProject app) {
    final IGanttProject project = app.getProject();
    final UIFacade uiFacade = app.getUIFacade();
    final ProjectUIFacade projectUiFacade = app.getProjectUIFacade();

    DesktopAdapter.install(new GanttProjectApi() {
      @Override
      public void showAboutDialog() {
        AboutDialog2 abd = new AboutDialog2(uiFacade);
        abd.show();
      }

      @Override
      public void showPreferencesDialog() {
        new SettingsDialogAction(project, uiFacade).actionPerformed(null);
      }

      @Override
      public void maybeQuit(QuitResponse quitResponse) {
        if (app.quitApplication()) {
          quitResponse.performQuit();
        } else {
          quitResponse.cancelQuit();
        }
      }

      @Override
      public void openFile(final File file) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            if (projectUiFacade.ensureProjectSaved(project)) {
              Document myDocument = project.getDocumentManager().getDocument(file.getAbsolutePath());
              try {
                projectUiFacade.openProject(myDocument, project);
              } catch (Document.DocumentException | IOException ex) {
                uiFacade.showErrorDialog(ex);
              }
            }
          }
        });
      }
    });
  }
}
