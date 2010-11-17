/***************************************************************************
 SettingsDialog.java    
 -----------------------------------------------------
 begin                : jun 2004
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.Mediator;
import net.sourceforge.ganttproject.gui.GeneralDialog;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas Dialog to edit the preferences for the application.
 */
public class SettingsDialog extends GeneralDialog implements ActionListener {
    /** If true restart the initialization */
    private boolean reinit = false;

    private final JButton restoreButton;

    public SettingsDialog(GanttProject parent) {
        super(parent, GanttProject.correctLabel(GanttLanguage.getInstance()
                .getText("settings")), true, new WelcomeSettingsPanel(parent));

        restoreButton = new JButton(language.getText("restoreDefaults"));
        restoreButton.setName("restore");
        restoreButton.addActionListener(this);
        if (southPanel != null) {
            southPanel.add(restoreButton);
        }
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                if (settingPanel != null) {
                    settingPanel.rollback();
                }
            }
        });
    }

    /** Construct the menu settings. */
    public void constructSections() {
        addPagesFromProviders();
        DefaultMutableTreeNode projectNode = addObject(GanttProject
                .correctLabel(language.getText("project")), null);
        addObject(GanttProject.correctLabel(language.getText("weekends")), null);
        DefaultMutableTreeNode generalNode = addObject(language
                .getText("general"), null);
        DefaultMutableTreeNode exportNode = addObject(GanttProject
                .correctLabel(language.getText("export")), null);
        addObject(language.getText("resourceRole"), null);

        // general section
        addObject(language.getText("parameters"), generalNode);
        addObject(language.getText("looknfeel"), generalNode);
        addObject(language.getText("languages"), generalNode);
        //addObject(language.getText("colors"), generalNode);

        // Export section
        // addObject ("html", exportNode);
        addObject("csv", exportNode);

        // Just to see the first level of the tree
        treeSections.scrollPathToVisible(new TreePath(projectNode.getPath()));
    }

    private void addPagesFromProviders() {
        Object[] extensions = Mediator.getPluginManager().getExtensions("net.sourceforge.ganttproject.OptionPageProvider", OptionPageProvider.class);
        for (int i=0; i<extensions.length; i++) {
            OptionPageProvider nextProvider = (OptionPageProvider) extensions[i];
            addObject(nextProvider, null);
        }
    }

    /** Callback for the tree selection event. */
    public void valueChanged(TreeSelectionEvent e) {
        if (reinit) {
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) (e.getPath()
                .getLastPathComponent());
        boolean bHasChange = settingPanel.applyChanges(true);

        // construct the sections
        if ((settingPanel instanceof LanguageSettingsPanel) && bHasChange) {
            reinit = true;
            rootNode.removeAllChildren();
            treeModel.setRoot(rootNode);
            treeModel.reload();
            constructSections();
            reinit = false;
        }

        // - remove the settingPanel
        mainPanel2.remove(0);
        
        Box vb = Box.createVerticalBox();        
        Object userObject = node.getUserObject();
        if (userObject instanceof OptionPageProvider) {
            settingPanel = new OptionPageProviderPanel((OptionPageProvider) userObject, getProject(), getUIFacade());
        } else {

        // - ask the settingPanel if parameters are changed

        // - Create the new panel
            String sNode = (String) (node.getUserObject());
            if (sNode.equals(language.getText("languages"))) {
                settingPanel = new LanguageSettingsPanel(appli);
            } else if (sNode.equals(GanttProject.correctLabel(language
                    .getText("project")))) {
                settingPanel = new ProjectSettingsPanel(getProject());
            } else if (sNode.equals(GanttProject.correctLabel(language
                    .getText("weekends")))) {
                settingPanel = new WeekendsSettingsPanel(getProject());
            } else if (sNode.equals(GanttProject.correctLabel(language
                    .getText("parameters")))) {
                settingPanel = new ParametersSettingsPanel(appli);
            } else if (sNode.equals(GanttProject.correctLabel(language
                    .getText("resourceRole")))) {
                settingPanel = new RolesSettingsPanel(appli);
            } else if (sNode.equals(language.getText("looknfeel"))) {
                settingPanel = new LnFSettingsPanel(appli);
            } else if (sNode.equals(GanttProject.correctLabel(language
                    .getText("export")))) {
                settingPanel = new ExportSettingsPanel(appli);
            } else if (sNode.equals("csv")) {
                settingPanel = new CSVSettingsPanel(appli);
            } else {
                settingPanel = new WelcomeSettingsPanel(appli);
            }
            vb.add(new TopPanel("  " + settingPanel.getTitle(), settingPanel
                    .getComment()));
        }
        // - initialize the panel
        vb.add(Box.createVerticalStrut(20));
        settingPanel.initialize();
        vb.add(settingPanel.getComponent());

        // - add the settingPanel into the main Panel
        mainPanel2.add(vb, 0);
        mainPanel2.repaint();
        mainPanel2.validate(); // validate the changes
    }

    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (e.getSource() == restoreButton) {
            appli.restoreOptions();
            // refresh the current panel
            settingPanel.rollback();
        }
    }
    
    

}
