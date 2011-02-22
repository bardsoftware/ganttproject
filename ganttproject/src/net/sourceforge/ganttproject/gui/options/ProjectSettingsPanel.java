/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev

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
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

    private final JTextField nameField;

    private final JTextField organizationField;

    private final JTextField webLinkField;

    private final JTextArea descriptionField;

    private final IGanttProject myProject;

    public ProjectSettingsPanel(IGanttProject project) {
        super(GanttProject.correctLabel(GanttLanguage.getInstance().getText(
                "project")), GanttLanguage.getInstance().getText(
                "settingsProject"));

        myProject = project;

        final Box vbProject = Box.createVerticalBox();

        nameField = new JTextField();
        addLabelAndComponent(vbProject, "name", nameField);

        organizationField = new JTextField();
        addLabelAndComponent(vbProject, "organization", organizationField);

        webLinkField = new JTextField();
        final JPanel webPanel = addLabelAndComponent(vbProject, "webLink", webLinkField);
        final JButton webButton = new TestGanttRolloverButton(new ImageIcon(
                getClass().getResource("/icons/web_16.gif")));
        webButton.setToolTipText(GanttProject.getToolTip(language
                .getText("openWebLink")));
        webButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // link to open the web link
                if (!BrowserControl.displayURL(webLinkField.getText())) {
                    GanttDialogInfo gdi = new GanttDialogInfo(null,
                            GanttDialogInfo.ERROR, GanttDialogInfo.YES_OPTION,
                            language.getText("msg4"), language.getText("error"));
                    gdi.setVisible(true);
                }
            }
        });
        webPanel.add(webButton, BorderLayout.EAST);

        descriptionField = new JTextArea(12, 25);
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        addLabelAndComponent(vbProject, "shortDescription", descriptionField);

        final JPanel projectPanel = new JPanel(new BorderLayout());
        projectPanel.add(vbProject, BorderLayout.NORTH);
        vb.add(projectPanel);

        applyComponentOrientation(language.getComponentOrientation());
    }

    /**
     * Adds a label and a component to box
     * @param box          is the box where the label and component are added to
     * @param labelTextID  is the text, which is looked up for the current language, used for the label
     * @param comp         is the component which is added
     * @return             the panel on which the label is placed
     */
    private JPanel addLabelAndComponent(Box box, String labelTextID, JComponent comp) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(language.getText(labelTextID)), BorderLayout.WEST);
        box.add(panel);
        box.add(comp);
        return panel;
    }

    /** This method checks if the value has changed, and asks for commit changes. */
    public boolean applyChanges(boolean askForApply) {
        if (myProject.getProjectName().equals(nameField.getText())
                && myProject.getOrganization().equals(organizationField.getText())
                && myProject.getWebLink().equals(webLinkField.getText())
                && myProject.getDescription().equals(descriptionField.getText())) {
            bHasChange = false;
            // no changes
        } else {
            bHasChange = true;
            // apply changes
            if (!askForApply || (askForApply && askForApplyChanges())) {
                myProject.setProjectName(getProjectName());
                myProject.setDescription(getProjectDescription());
                myProject.setOrganization(getProjectOrganization());
                myProject.setWebLink(getProjectWebLink());
            }
        }
        return bHasChange;
    }

    /** Initialize the component. */
    public void initialize() {
        nameField.setText(myProject.getProjectName());
        organizationField.setText(myProject.getOrganization());
        webLinkField.setText(myProject.getWebLink());
        descriptionField.setText(myProject.getDescription());
    }

    /** @return the selected project name */
    public String getProjectName() {
        return nameField.getText();
    }

    /** @return the organization */
    public String getProjectOrganization() {
        return organizationField.getText();
    }

    /** @return the web link */
    public String getProjectWebLink() {
        return webLinkField.getText();
    }

    /** @return the project description */
    public String getProjectDescription() {
        return descriptionField.getText();
    }
}
