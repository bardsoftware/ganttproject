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
package net.sourceforge.ganttproject.gui.about;

import javax.swing.Box;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.GeneralDialog;
import net.sourceforge.ganttproject.gui.options.TopPanel;

/**
 * New dialog box for about GanttProject
 *
 * @author athomas
 */
public class AboutDialog extends GeneralDialog {
    public AboutDialog(GanttProject parent) {
        super(parent, language.getCorrectedLabel("about") + " - Ganttproject",
                true, new AboutPanel());

        // hide the cancel button
        cancelButton.setVisible(false);
    }

    /** Callback for the tree selection event. */
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath()
                .getLastPathComponent();
        String sNode = (String) node.getUserObject();

        // - remove the settingPanel
        mainPanel2.remove(0);

        // - Create the new panel
        if (sNode.equals(language.getCorrectedLabel("about"))) {
            settingPanel = new AboutPanel();
        } else if (sNode.equals(language.getText("authors"))) {
            settingPanel = new AboutAuthorPanel();
        } else if (sNode.equals(language.getText("jinfos"))) {
            settingPanel = new AboutJavaInfosPanel();
        } else if (sNode.equals(language.getText("license"))) {
            settingPanel = new AboutLicensePanel();
        } else if (sNode.equals(language.getText("library"))) {
            settingPanel = new AboutLibraryPanel();
        }

        Box vb = Box.createVerticalBox();
        vb.add(TopPanel.create("  " + settingPanel.getTitle(), settingPanel
                .getComment()));
        vb.add(settingPanel);
        settingPanel.initialize();

        // - add the settingPanel into the main Panel
        mainPanel2.add(vb, 0);
        mainPanel2.repaint();
        mainPanel2.validate(); // validate the changes
    }

    /** Construct the menu settings. */
    @Override
    public void constructSections() {
        addObject(language.getCorrectedLabel("about"), null);
        addObject(language.getText("authors"), null);
        addObject(language.getText("jinfos"), null);
        addObject(language.getText("license"), null);
        addObject(language.getText("library"), null);
    }
}
