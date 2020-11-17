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

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.document.DocumentStorageUi;
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

    DocumentStorageUi.DocumentReceiver receiver = new DocumentStorageUi.DocumentReceiver() {
      @Override
      public void setDocument(Document document) {
        result[0] = document;
      }
    };
    DocumentStorageUi webdavStorage = myDocumentManager.getWebDavStorageUi();
    DocumentStorageUi.Components components = isOpenUrl ? webdavStorage.open(document, receiver) : webdavStorage.save(document, receiver);
    myUiFacade.createDialog(components.contentPane, components.actions,
        GanttLanguage.getInstance().getCorrectedLabel((isOpenUrl ? "project.open.url" : "project.save.url"))).show();
    return result[0] == null ? null : myDocumentManager.getProxyDocument(result[0]);
  }
}
