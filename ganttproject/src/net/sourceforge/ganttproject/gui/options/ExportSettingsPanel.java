/***************************************************************************
 ExportSettingsPanel.java 
 ------------------------------------------
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
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas Export parameters panel (for png export, html, print ...)
 */
public class ExportSettingsPanel extends GeneralOptionPanel {

    JCheckBox cbName;

    JCheckBox cbComplete;

    JCheckBox cbRelations;

    // JCheckBox cb3dBorder;

    private GanttProject appli;

    /** Constructor. */
    public ExportSettingsPanel(GanttProject parent) {
        super(GanttProject.correctLabel(GanttLanguage.getInstance().getText(
                "export")), GanttLanguage.getInstance().getText(
                "settingsExport"), parent);
        appli = parent;
        // export the name of the task
        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.add(cbName = new JCheckBox(), BorderLayout.WEST);
        namePanel
                .add(new JLabel(language.getText("name")), BorderLayout.CENTER);
        vb.add(namePanel);
        vb.add(new JPanel());

        // export the complete percent of the task
        JPanel completePanel = new JPanel(new BorderLayout());
        completePanel.add(cbComplete = new JCheckBox(), BorderLayout.WEST);
        completePanel.add(new JLabel(language.getText("advancement")),
                BorderLayout.CENTER);
        vb.add(completePanel);
        vb.add(new JPanel());

        // export the relationst of the task
        JPanel relationsPanel = new JPanel(new BorderLayout());
        relationsPanel.add(cbRelations = new JCheckBox(), BorderLayout.WEST);
        relationsPanel.add(new JLabel(language.getText("depends")),
                BorderLayout.CENTER);
        vb.add(relationsPanel);
        vb.add(new JPanel());

        // export the 3D border of the task
        // JPanel bordersPanel = new JPanel(new BorderLayout());
        // bordersPanel.add(cb3dBorder = new JCheckBox(), BorderLayout.WEST);
        // bordersPanel.add(new JLabel(language.getText("border3D")),
        // BorderLayout.CENTER);
        // vb.add(bordersPanel);
        vb.add(new JPanel());

    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.ganttproject.gui.options.GeneralOptionPanel#applyChanges(boolean)
     */
    public boolean applyChanges(boolean askForApply) {
        if (getExportName() == appli.getOptions().getExportName()
                && getExportComplete() == appli.getOptions()
                        .getExportComplete()
                && getExportRelations() == appli.getOptions()
                        .getExportRelations()) {
            bHasChange = false;
        } else {
            bHasChange = true;
            if (!askForApply || (askForApply && askForApplyChanges())) {
                appli.getOptions().setExportName(getExportName());
                appli.getOptions().setExportComplete(getExportComplete());
                appli.getOptions().setExportRelations(getExportRelations());
            }
        }
        return bHasChange;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.ganttproject.gui.options.GeneralOptionPanel#initialize()
     */
    public void initialize() {
        cbName.setSelected(appli.getOptions().getExportName());
        cbComplete.setSelected(appli.getOptions().getExportComplete());
        cbRelations.setSelected(appli.getOptions().getExportRelations());
        // cb3dBorder.setSelected(appli.getOptions().getExport3dBorders());
    }

    boolean getExportName() {
        return cbName.isSelected();
    }

    boolean getExportComplete() {
        return cbComplete.isSelected();
    }

    boolean getExportRelations() {
        return cbRelations.isSelected();
    }
    // boolean getExport3DBorder() {
    // return cb3dBorder.isSelected();
    // }

}
