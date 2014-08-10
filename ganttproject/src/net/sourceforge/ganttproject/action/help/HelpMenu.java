/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.action.help;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.document.ReadOnlyProxyDocument;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.ViewLogDialog;
import net.sourceforge.ganttproject.gui.about.AboutDialog2;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Collection of actions from Help menu.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class HelpMenu {

  private final AboutAction myAboutAction;
  private final ViewLogAction myViewLogAction;
  private final RecoverLastProjectAction myRecoverAction;

  public HelpMenu(IGanttProject project, UIFacade uiFacade, ProjectUIFacade projectUiFacade) {
    myAboutAction = new AboutAction(uiFacade);
    myViewLogAction = new ViewLogAction(uiFacade);
    myRecoverAction = new RecoverLastProjectAction(project, uiFacade, projectUiFacade);
  }

  public JMenu createMenu() {
    JMenu result = UIUtil.createTooltiplessJMenu(GPAction.createVoidAction("help"));
    result.add(myAboutAction);
    result.add(myViewLogAction);
    result.add(myRecoverAction);
    return result;
  }

  private static class AboutAction extends GPAction {
    private final UIFacade myUiFacade;

    AboutAction(UIFacade uifacade) {
      super("about");
      myUiFacade = uifacade;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      AboutDialog2 agp = new AboutDialog2(myUiFacade);
      agp.show();
    }
  }

  private static class ViewLogAction extends GPAction {
    private final UIFacade myUiFacade;

    ViewLogAction(UIFacade uiFacade) {
      super("viewLog");
      myUiFacade = uiFacade;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      ViewLogDialog.show(myUiFacade);
    }
  }

  private static class RecoverLastProjectAction extends GPAction {
    private final UIFacade myUiFacade;
    private final DocumentManager myDocumentManager;
    private final IGanttProject myProject;
    private final ProjectUIFacade myProjectUiFacade;

    RecoverLastProjectAction(IGanttProject project, UIFacade uiFacade, ProjectUIFacade projectUiFacade) {
      super("help.recover");
      myProject = project;
      myUiFacade = uiFacade;
      myDocumentManager = project.getDocumentManager();
      myProjectUiFacade = projectUiFacade;
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      try {
        final Document lastAutosaveDocument = myDocumentManager.getLastAutosaveDocument(null);
        if (lastAutosaveDocument != null) {
          runAction(lastAutosaveDocument);
        }
      } catch (IOException e) {
        GPLogger.log(new RuntimeException("Failed to read autosave documents", e));
      }
    }

    private void runAction(final Document autosaveDocument) {
      OkAction ok = new OkAction() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          recover(autosaveDocument);
        }
      };
      CancelAction skip = new CancelAction("help.recover.skip") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              Document prevAutosaveDocument = null;
              try {
                prevAutosaveDocument = myDocumentManager.getLastAutosaveDocument(autosaveDocument);
              } catch (IOException e) {
                GPLogger.log(new RuntimeException("Failed to read autosave documents", e));
              }
              if (prevAutosaveDocument != null) {
                runAction(prevAutosaveDocument);
              }
            }
          });
        }
      };
      File f = new File(autosaveDocument.getFilePath());
      myUiFacade.showOptionDialog(
          JOptionPane.INFORMATION_MESSAGE,
          GanttLanguage.getInstance().formatText("help.recover.autosaveInfo", f.getName(), new Date(f.lastModified()),
              f.length()), new Action[] { ok, skip, CancelAction.CLOSE });
    }

    protected void recover(Document recoverDocument) {
      try {
        myProjectUiFacade.openProject(new ReadOnlyProxyDocument(recoverDocument), myProject);
      } catch (Throwable e) {
        GPLogger.log(new RuntimeException("Failed to recover file " + recoverDocument.getFileName(), e));
      }
    }
  }

}
