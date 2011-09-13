/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import java.util.Iterator;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentsMRU;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;

/**
 * Menu that contains a number of Most Recently Used documents. When clicked on
 * a menu item, the corresponding document is opened.
 */
public class ProjectMRUMenu extends JMenu {
    private final IGanttProject myProject;
    private final UIFacade myUIFacade;
    private final ProjectUIFacade myProjectUIFacade;

    private static final int maxSizeMRU = 5;
    private final DocumentsMRU myDocumentsMRU = new DocumentsMRU(maxSizeMRU);

    public ProjectMRUMenu(IGanttProject project, UIFacade uiFacade, ProjectUIFacade projectUIFacade) {
        super();
        myProject = project;
        myUIFacade = uiFacade;
        myProjectUIFacade = projectUIFacade;
    }

    public void add(Document document) {
        if (myDocumentsMRU.add(document)) {
            updateMenuMRU();
        }
    }

    private void updateMenuMRU() {
        removeAll();
        int index = 0;
        Iterator<Document> iterator = iterator();
        while (iterator.hasNext()) {
            index++;
            Document doc = iterator.next();
            JMenuItem mi = new JMenuItem(
                    new OpenMRUDocumentAction(index, doc, myProject, myUIFacade, myProjectUIFacade));
            add(mi);
        }
    }

    public void clear() {
        myDocumentsMRU.clear();
        removeAll();
    }

    public Iterator<Document> iterator() {
        return myDocumentsMRU.iterator();
    }
}
