/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Thomas Alexandre, GanttProject Team

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
package net.sourceforge.ganttproject.gui.server;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.sourceforge.ganttproject.gui.options.GeneralOptionPanel;

/**
 * The connection panel for HTTP access.
 *
 * @author athomas
 */
public class ConnectionPanel extends GeneralOptionPanel {
    public ConnectionPanel() {
        super(language.getCorrectedLabel("project.open.url"), language.getText(
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

    @Override
    public boolean applyChanges(boolean askForApply) {
        return false;
    }

    @Override
    public void initialize() {
    }
}
