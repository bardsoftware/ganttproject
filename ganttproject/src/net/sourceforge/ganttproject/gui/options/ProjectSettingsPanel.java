/***************************************************************************
 ProjectSettingsPanel.java 
 ------------------------------------------
 begin                : 27 juin 2004
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
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.GanttDialogInfo;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.BrowserControl;

/**
 * Panel to edit the project properties
 * 
 * @author athomas
 */
public class ProjectSettingsPanel extends GeneralOptionPanel {

    // JTextField tfFile;
    private final JTextField tfName;

    private final JTextField tfOrganization;

    private final JTextField tfWebLink;

    private final JTextArea taDescr;

    private final IGanttProject myProject;

    public ProjectSettingsPanel(IGanttProject project) {
        super(language.correctLabel(GanttLanguage.getInstance().getText(
                "project")), language.getText("settingsProject"));

        myProject = project;
        Box vbproject = Box.createVerticalBox();

        /*
         * JPanel filePanel = new JPanel(new BorderLayout()); filePanel.add(new
         * JLabel(language.getText("file")), BorderLayout.WEST);
         * vbproject.add(filePanel); vbproject.add(tfFile = new JTextField());
         * tfFile.setEditable(false); vbproject.add(new JPanel());
         */

        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.add(new JLabel(language.getText("name")), BorderLayout.WEST);
        vbproject.add(namePanel);
        vbproject.add(tfName = new JTextField());
        vbproject.add(new JPanel());

        JPanel orgaPanel = new JPanel(new BorderLayout());
        orgaPanel.add(new JLabel(language.getText("organization")),
                BorderLayout.WEST);
        vbproject.add(orgaPanel);
        vbproject.add(tfOrganization = new JTextField());
        vbproject.add(new JPanel());

        tfWebLink = new JTextField();
        JButton bWeb = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/web_16.gif")));
        bWeb.setToolTipText(GanttProject.getToolTip(language
                .getText("openWebLink")));
        bWeb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // link to open the web link
                if (!BrowserControl.displayURL(tfWebLink.getText())) {
                    GanttDialogInfo gdi = new GanttDialogInfo(null,
                            GanttDialogInfo.ERROR, GanttDialogInfo.YES_OPTION,
                            language.getText("msg4"), language.getText("error"));
                    gdi.setVisible(true);
                }
            }
        });

        JPanel webPanel = new JPanel(new BorderLayout());
        webPanel.add(new JLabel(language.getText("webLink")), BorderLayout.WEST);
        webPanel.add(bWeb, BorderLayout.EAST);
        vbproject.add(webPanel);
        vbproject.add(tfWebLink);
        vbproject.add(new JPanel());

        JPanel descrPanel = new JPanel(new BorderLayout());
        descrPanel.add(new JLabel(language.getText("shortDescription")),
                BorderLayout.WEST);
        vbproject.add(descrPanel);
        taDescr = new JTextArea(12, 25);
        taDescr.setLineWrap(true);
        taDescr.setWrapStyleWord(true);
        JScrollPane scpArea = new JScrollPane(taDescr);
        vbproject.add(scpArea);

        JPanel projectPanel = new JPanel(new BorderLayout());
        projectPanel.add(vbproject, BorderLayout.NORTH);
        vb.add(projectPanel);

        applyComponentOrientation(language.getComponentOrientation());
    }

    public boolean applyChanges(boolean askForApply) {
        boolean hasChange;
        if (myProject.getProjectName().equals(tfName.getText())
                && myProject.getOrganization().equals(tfOrganization.getText())
                && myProject.getWebLink().equals(tfWebLink.getText())
                && myProject.getDescription().equals(taDescr.getText())) {
            hasChange = false;
        } else {
            hasChange = true;
            // apply changes if user clicked apply (or warn about pending changes and ask whether to apply or not)
            if (!askForApply || (askForApply && askForApplyChanges())) {
                myProject.setProjectName(getProjectName());
                myProject.setDescription(getProjectDescription());
                myProject.setOrganization(getProjectOrganization());
                myProject.setWebLink(getWebLink());
            }
        }
        return hasChange;
    }

    public void initialize() {
        /*
         * if(appli.projectDocument!=null) { String filename =
         * appli.projectDocument.getFilePath(); if(appli.askForSave)
         * filename+="*"; tfFile.setText(filename); }
         */
        // tfFile.setText("see ProjectSettingsPanel line 149 ??");
        tfName.setText(myProject.getProjectName());
        tfOrganization.setText(myProject.getOrganization());
        tfWebLink.setText(myProject.getWebLink());
        taDescr.setText(myProject.getDescription());
    }

    /** @return the selected project name */
    public String getProjectName() {
        return tfName.getText();
    }

    /** @return the organization */
    public String getProjectOrganization() {
        return tfOrganization.getText();
    }

    /** @return the web link */
    public String getWebLink() {
        return tfWebLink.getText();
    }

    /** @return the project description */
    public String getProjectDescription() {
        return taDescr.getText();
    }
}
