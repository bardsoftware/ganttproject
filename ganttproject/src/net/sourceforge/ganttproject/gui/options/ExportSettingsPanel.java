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
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.GanttProject;

/**
 * Export parameters panel (for png export, html, print ...)
 *
 * @author athomas
 */
public class ExportSettingsPanel extends GeneralOptionPanel {

    private final JCheckBox cbName;

    private final JCheckBox cbComplete;

    private final JCheckBox cbRelations;

    private final GanttProject appli;

    public ExportSettingsPanel(GanttProject parent) {
        super(language.getCorrectedLabel("export"), language
                .getText("settingsExport"));
        appli = parent;

        // export the name of the task
        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.add(cbName = new JCheckBox(), BorderLayout.WEST);
        namePanel.add(new JLabel(language.getText("name")), BorderLayout.CENTER);
        vb.add(namePanel);
        vb.add(new JPanel());

        // export the complete percent of the task
        JPanel completePanel = new JPanel(new BorderLayout());
        completePanel.add(cbComplete = new JCheckBox(), BorderLayout.WEST);
        completePanel.add(new JLabel(language.getText("advancement")),
                BorderLayout.CENTER);
        vb.add(completePanel);
        vb.add(new JPanel());

        // export the relations of the task
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

    @Override
    public boolean applyChanges(boolean askForApply) {
        boolean hasChange;
        if (getExportName() == appli.getGanttOptions().getExportName()
                && getExportComplete() == appli.getGanttOptions()
                        .getExportComplete()
                && getExportRelations() == appli.getGanttOptions()
                        .getExportRelations()) {
            hasChange = false;
        } else {
            hasChange = true;
            // apply changes if user clicked apply (or warn about pending changes and ask whether to apply o not)
            if (!askForApply || (askForApply && askForApplyChanges())) {
                appli.getGanttOptions().setExportName(getExportName());
                appli.getGanttOptions().setExportComplete(getExportComplete());
                appli.getGanttOptions().setExportRelations(getExportRelations());
            }
        }
        return hasChange;
    }

    @Override
    public void initialize() {
        cbName.setSelected(appli.getGanttOptions().getExportName());
        cbComplete.setSelected(appli.getGanttOptions().getExportComplete());
        cbRelations.setSelected(appli.getGanttOptions().getExportRelations());
        // cb3dBorder.setSelected(appli.getOptions().getExport3dBorders());
    }

    public boolean getExportName() {
        return cbName.isSelected();
    }

    public boolean getExportComplete() {
        return cbComplete.isSelected();
    }

    public boolean getExportRelations() {
        return cbRelations.isSelected();
    }

    // public boolean getExport3DBorder() {
    // return cb3dBorder.isSelected();
    // }
}
