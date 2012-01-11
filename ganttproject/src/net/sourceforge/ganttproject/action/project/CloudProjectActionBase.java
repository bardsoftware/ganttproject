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
import net.sourceforge.ganttproject.document.HttpDocument;
import net.sourceforge.ganttproject.gui.GanttURLChooser;
import net.sourceforge.ganttproject.gui.UIFacade;

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
        Document document = project.getDocument();
        GanttURLChooser uc = new GanttURLChooser(myUiFacade,
            (null != document) ? document.getURLPath() : myDocumentManager.getLastWebDAVDocumentOption().getValue(),
            (null != document) ? document.getUsername() : null,
            (null != document) ? document.getPassword() : null,
            myDocumentManager.getWebDavLockTimeoutOption().getValue());
        uc.show(isOpenUrl);
        if (uc.getChoice() == UIFacade.Choice.OK) {
            if (!sameDocument(document, uc)) {
                document = myDocumentManager.getDocument(uc.getUrl(), uc.getUsername(), uc.getPassword());
            }
            myDocumentManager.getLastWebDAVDocumentOption().setValue(uc.getUrl());
            if (uc.isTimeoutEnabled()) {
                HttpDocument.setLockDAVMinutes(uc.getTimeout());
                myDocumentManager.getWebDavLockTimeoutOption().setValue(uc.getTimeout());
            } else {
                HttpDocument.setLockDAVMinutes(-1);
            }
        }
        else {
            document = null;
        }
        return document;
    }

    private static boolean sameDocument(Document document, GanttURLChooser uc) {
        if (document == null) {
            return false;
        }
        return document.getURLPath().equals(uc.getUrl()) && document.getUsername().equals(uc.getUsername())
            && document.getPassword().equals(uc.getPassword());
    }
}
