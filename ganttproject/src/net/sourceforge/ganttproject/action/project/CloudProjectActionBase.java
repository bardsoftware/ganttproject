/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.action.project;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.document.DocumentStorageUi;
import net.sourceforge.ganttproject.document.DocumentStorageUi.DocumentDescriptor;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Base class for actions doing open/save from/to cloud.
 *
 * @author dbarashev (Dmitry Barashev)
 */
abstract class CloudProjectActionBase extends GPAction {
  private final DocumentManager myDocumentManager;
  private final UIFacade myUiFacade;

  protected CloudProjectActionBase(String key, UIFacade uiFacade, DocumentManager documentManager) {
    super(key);
    myUiFacade = uiFacade;
    myDocumentManager = documentManager;
  }

  protected Document showURLDialog(IGanttProject project, boolean isOpenUrl) {
    final Document document = project.getDocument();
    final Document[] result = new Document[1];

    class OkActionImpl extends OkAction implements DocumentStorageUi.DocumentReceiver {
      private DocumentDescriptor myChosenDocument;

      @Override
      public void actionPerformed(ActionEvent e) {
        if (myChosenDocument != null && !sameDocument(document, myChosenDocument)) {
          result[0] = myDocumentManager.getDocument(myChosenDocument.url, myChosenDocument.username, myChosenDocument.password);
        }
      }

      @Override
      public void setDocument(DocumentDescriptor document) {
        myChosenDocument = document;
      }
    }

    OkActionImpl okAction = new OkActionImpl();
    DocumentStorageUi webdavStorage = myDocumentManager.getWebDavStorageUi();
    JComponent component = isOpenUrl ? webdavStorage.open(document, okAction) : webdavStorage.save(document, okAction);
    myUiFacade.createDialog(component, new Action[] { okAction, CancelAction.EMPTY },
        GanttLanguage.getInstance().getCorrectedLabel((isOpenUrl ? "project.open.url" : "project.save.url"))).show();
    return result[0];
  }

  private static boolean sameDocument(Document d1, DocumentDescriptor chosenDocument) {
    if (d1 == null || chosenDocument == null) {
      return false;
    }
    return d1.getURI().toString().equals(chosenDocument.url) && d1.getUsername().equals(chosenDocument.username)
        && d1.getPassword().equals(chosenDocument.password);
  }
}
