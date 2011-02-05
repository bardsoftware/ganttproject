/***************************************************************************
 GanttURLChooser.java  -  description
 -------------------
 begin                : july 2003
 copyright            : (C) 2003 by Thomas Alexandre
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

package net.sourceforge.ganttproject.gui;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.UIFacade.Choice;
import net.sourceforge.ganttproject.gui.options.SpringUtilities;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Class to select a file from th web
 */
public class GanttURLChooser {
    private JTextField urlField;

    private JTextField userNameField;

    private JPasswordField passwordField;

    private final GanttLanguage language = GanttLanguage.getInstance();

    private final UIFacade myUiFacade;

    private String myUrl;

    private String myUsername;

    private String myPassword;

    private Choice myChoice;

    public GanttURLChooser(UIFacade uiFacade, String url, String username, String password) {
        myUiFacade = uiFacade;
        myUrl = url;
        myUsername = username;
        myPassword = password;
        myChoice = UIFacade.Choice.CANCEL;
    }

    public void show(boolean isOpenUrl) {
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

        SpringUtilities.makeCompactGrid(panel, 3, 2, 0, 0, 3, 3);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        OkAction okAction = new OkAction() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                myUrl = urlField.getText();
                myUsername = userNameField.getText();
                myPassword = new String(passwordField.getPassword());
                myChoice = UIFacade.Choice.OK;
            }
        };
        CancelAction cancelAction = new CancelAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        myUiFacade.showDialog(panel, new Action[] {okAction, cancelAction},
            isOpenUrl ? language.getText("openFromServer") : language.getText("saveToServer"));
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
}
