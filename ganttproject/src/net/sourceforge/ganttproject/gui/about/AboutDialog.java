/***************************************************************************
 AboutDialog.java 
 ------------------------------------------
 begin                : 29 juin 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package net.sourceforge.ganttproject.gui.about;

import javax.swing.Box;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.GeneralDialog;
import net.sourceforge.ganttproject.gui.options.TopPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas New dialog box for about GanttProject :)
 */
public class AboutDialog extends GeneralDialog {
    public AboutDialog(GanttProject parent) {
        super(parent, GanttProject.correctLabel(GanttLanguage.getInstance()
                .getText("about"))
                + " - Ganttproject", true, new AboutPanel());

        // hide the cancel button
        cancelButton.setVisible(false);
    }

    /** Callback for the tree selection event. */
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) (e.getPath()
                .getLastPathComponent());
        String sNode = (String) (node.getUserObject());

        // - ask the settingPanel if parameters are changed
        // boolean bHasChange = settingPanel.applyChanges(true); //no change to
        // do on this panel

        // - remove the settingPanel
        mainPanel2.remove(0);

        // - Create the new panel
        if (sNode.equals(GanttProject.correctLabel(language.getText("about")))) {
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
        vb.add(new TopPanel("  " + settingPanel.getTitle(), settingPanel
                .getComment()));
        vb.add(settingPanel);
        settingPanel.initialize();

        // - add the settingPanel into the main Panel
        mainPanel2.add(vb, 0);
        mainPanel2.repaint();
        mainPanel2.validate(); // validate the changes
    }

    /** Construct the menu settings. */
    public void constructSections() {
        addObject(GanttProject.correctLabel(language.getText("about")), null);
        addObject(language.getText("authors"), null);
        addObject(language.getText("jinfos"), null);
        addObject(language.getText("license"), null);
        addObject(language.getText("library"), null);
    }
}
