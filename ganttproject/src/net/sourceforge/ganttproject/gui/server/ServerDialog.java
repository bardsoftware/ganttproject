/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Alexandre Thomas, GanttProject Team

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
package net.sourceforge.ganttproject.gui.server;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.GeneralDialog;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Generic dialog for server I/O
 *
 * @author athomas
 */
public class ServerDialog extends GeneralDialog {
    public ServerDialog(GanttProject parent) {
        super(parent, GanttLanguage.getInstance().correctLabel(GanttLanguage.getInstance()
                .getText("webServer")), true, new ConnectionPanel());
    }

    @Override
    public void constructSections() {
        addObject(language.getCorrectedLabel("project.open.url"), null);
    }

    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) (e.getPath()
                .getLastPathComponent());
        String sNode = (String) (node.getUserObject());

        // - remove the settingPanel
        mainPanel2.remove(0);

        // - Create the new panel
        if (sNode.equals(language.getCorrectedLabel("project.open.url"))) {
            settingPanel = new ConnectionPanel();
        }

        // - initialize the panel
        settingPanel.initialize();

        // - add the settingPanel into the main Panel
        mainPanel2.add(settingPanel, 0);
        mainPanel2.repaint();
        mainPanel2.validate(); // validate the changes
    }
}
