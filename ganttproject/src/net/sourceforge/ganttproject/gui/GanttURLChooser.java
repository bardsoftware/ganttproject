/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2003-2011 Dmitry Barashev

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

package net.sourceforge.ganttproject.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.UIFacade.Choice;
import net.sourceforge.ganttproject.gui.options.SpringUtilities;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Dialog for Open/Save file from/to WebDAV resource.
 *
 * @author Dmitry Barashev (major rewrite).
 * @author Alexandre Thomas (initial version).
 */
class GanttURLChooser {
    private JTextField urlField;

    private JTextField userNameField;

    private JPasswordField passwordField;

    private final GanttLanguage language = GanttLanguage.getInstance();

    private final UIFacade myUiFacade;

    private String myUrl;

    private String myUsername;

    private String myPassword;

    private Choice myChoice;

    private JCheckBox lockCheckbox;

    private GTextField myLockTimeout;

    private int myTimeout;

    protected boolean isTimeoutEnabled;

    GanttURLChooser(UIFacade uiFacade, String url, String username, String password, int timeout) {
        myUiFacade = uiFacade;
        myUrl = url;
        myUsername = username;
        myPassword = password;
        myTimeout = timeout;
        myChoice = UIFacade.Choice.CANCEL;
    }

    void show(boolean isOpenUrl) {
        JPanel panel = new JPanel(new SpringLayout());

        panel.add(new JLabel(language.getText("fileFromServer")));
        String sDefaultURL = "http://ganttproject.sourceforge.net/tmp/testGantt.xml";
        urlField = new JTextField((null != myUrl) ? myUrl : sDefaultURL);
        panel.add(urlField);

        panel.add(new JLabel(language.getText("userName")));
        userNameField = new JTextField(myUsername);
        panel.add(userNameField);

        panel.add(new JLabel(language.getText("password")));
        passwordField = new JPasswordField(myPassword);
        panel.add(passwordField);

        panel.add(new JPanel());
        panel.add(new JPanel());

        panel.add(new JLabel(language.getText("webdav.lockResource.label")));
        lockCheckbox = new JCheckBox();
        lockCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                myLockTimeout.setEnabled(lockCheckbox.isSelected());
            }
        });
        panel.add(lockCheckbox);

        panel.add(new JLabel(language.getText("webdav.lockTimeout.label")));
        myLockTimeout = new GTextField();
        myLockTimeout.setPattern(GTextField.PATTERN_INTEGER);
        if (myTimeout >= 0) {
            myLockTimeout.setText(String.valueOf(myTimeout));
            lockCheckbox.setSelected(true);
        } else {
            lockCheckbox.setSelected(false);
        }
        panel.add(myLockTimeout);
        SpringUtilities.makeCompactGrid(panel, 6, 2, 0, 0, 3, 3);

        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        OkAction okAction = new OkAction() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                myUrl = urlField.getText();
                myUsername = userNameField.getText();
                myPassword = new String(passwordField.getPassword());
                myChoice = UIFacade.Choice.OK;
                isTimeoutEnabled = lockCheckbox.isSelected();
                if (isTimeoutEnabled) {
                    String timeoutString = myLockTimeout.getText().trim();
                    try {
                        myTimeout = Integer.parseInt(timeoutString);
                    } catch (NumberFormatException e) {
                        GPLogger.log(e);
                        myTimeout = 0;
                        isTimeoutEnabled = false;
                    }
                } else {
                    myTimeout = 0;
                }
            }
        };
        CancelAction cancelAction = new CancelAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        myUiFacade.createDialog(panel, new Action[] {okAction, cancelAction},
            isOpenUrl ? language.getText("openFromServer") : language.getText("saveToServer")).show();
    }

    UIFacade.Choice getChoice() {
        return myChoice;
    }

    String getUsername() {
        return myUsername;
    }

    String getUrl() {
        return myUrl;
    }

    String getPassword() {
        return myPassword;
    }

    int getTimeout() {
        return myTimeout;
    }

    boolean isTimeoutEnabled() {
        return isTimeoutEnabled;
    }
}
