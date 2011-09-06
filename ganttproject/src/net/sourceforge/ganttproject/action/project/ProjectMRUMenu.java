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

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentsMRU;

public class ProjectMRUMenu extends JMenu {
    private final GanttProject myProject;

    private static final int maxSizeMRU = 5;
    private final DocumentsMRU myDocumentsMRU = new DocumentsMRU(maxSizeMRU);

    public ProjectMRUMenu(GanttProject project) {
        super();
        myProject = project;
    }

    public DocumentsMRU getDocumentsMRU() {
        return myDocumentsMRU;
    }

    public void add(Document document) {
        if (myDocumentsMRU.add(document)) {
            updateMenuMRU();
        }
    }

    private void updateMenuMRU() {
        removeAll();
        int index = 0;
        Iterator<Document> iterator = myDocumentsMRU.iterator();
        while (iterator.hasNext()) {
            index++;
            Document document = iterator.next();
            JMenuItem mi = new JMenuItem(new OpenMRUDocumentAction(index, document, myProject));
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
