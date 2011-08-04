/***************************************************************************
 GanttProject.java
 -----------------
 begin                : 30 juin 2004
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
package net.sourceforge.ganttproject.gui.server;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.GeneralDialog;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas Generic dialog for server I/O
 */
public class ServerDialog extends GeneralDialog {
    public ServerDialog(GanttProject parent) {
        super(parent, GanttLanguage.getInstance().correctLabel(GanttLanguage.getInstance()
                .getText("webServer")), true, new ConnectionPanel());
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.ganttproject.gui.GeneralDialog#constructSections()
     */
    public void constructSections() {
        addObject(language.correctLabel(language.getText("openFromServer")),
                null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
     */
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) (e.getPath()
                .getLastPathComponent());
        String sNode = (String) (node.getUserObject());

        // - ask the settingPanel if parameters are changed
        // boolean bHasChange = settingPanel.applyChanges(true); //no change to do on this panel

        // - remove the settingPanel
        mainPanel2.remove(0);

        // - Create the new panel
        if (sNode.equals(GanttLanguage.getInstance().correctLabel(
                language.getText("openFromServer")))) {
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
