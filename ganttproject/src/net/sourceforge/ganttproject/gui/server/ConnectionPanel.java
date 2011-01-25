/***************************************************************************
 GanttProject.java
 -----------------
 begin                : 1 juil. 2004
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

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.options.GeneralOptionPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas The connection panel for http acces.
 */
public class ConnectionPanel extends GeneralOptionPanel {
    public ConnectionPanel() {
        super(GanttProject.correctLabel(GanttLanguage.getInstance().getText(
                "openFromServer")), GanttLanguage.getInstance().getText(
                "settingsConnection"));

        Box vbServer = Box.createVerticalBox();

        // url textfield
        JPanel urlPanel = new JPanel(new BorderLayout());
        urlPanel.add(new JLabel(language.getText("fileFromServer")),
                BorderLayout.WEST);

        vbServer.add(urlPanel);
        vbServer.add(new JTextField("http://"));
        vbServer.add(new JPanel());

        // login textfield
        JPanel loginPanel = new JPanel(new BorderLayout());
        loginPanel.add(new JLabel(language.getText("userName")),
                BorderLayout.WEST);

        vbServer.add(loginPanel);
        vbServer.add(new JTextField());
        vbServer.add(new JPanel());

        // password textfield
        JPanel passwordPanel = new JPanel(new BorderLayout());
        passwordPanel.add(new JLabel(language.getText("userName")),
                BorderLayout.WEST);

        vbServer.add(passwordPanel);
        vbServer.add(new JPasswordField());
        vbServer.add(new JPanel());

        JPanel serverPanel = new JPanel(new BorderLayout());
        serverPanel.add(vbServer, BorderLayout.NORTH);
        vb.add(serverPanel);

        applyComponentOrientation(language.getComponentOrientation());
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.ganttproject.gui.options.GeneralOptionPanel#applyChanges(boolean)
     */
    public boolean applyChanges(boolean askForApply) {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.ganttproject.gui.options.GeneralOptionPanel#initialize()
     */
    public void initialize() {
    }

}
