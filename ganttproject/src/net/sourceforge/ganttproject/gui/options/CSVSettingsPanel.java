/***************************************************************************
 CSVSettingsPanel.java
 -----------------
 begin                : 7 juil. 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas@ganttproject.org
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
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.io.CSVOptions;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas Panel to edit the text export parameters
 */
public class CSVSettingsPanel extends GeneralOptionPanel implements
        ActionListener {

    private final JRadioButton bFixedSize;

    private final JRadioButton bSeparatedText;

    private final JRadioButton bDoubleDot;

    private final JRadioButton bDotComa;

    private final JRadioButton bComa;

    private final JRadioButton bSpace;

    private final JRadioButton bOther;

    private final JComboBox cbTextSeparator;

    private final JTextField tfOther;

    private final JCheckBox cbTaskID;

    private final JCheckBox cbTaskName;

    private final JCheckBox cbStartDate;

    private final JCheckBox cbEndDate;

    private final JCheckBox cbTaskPercent;

    private final JCheckBox cbTaskDuration;

    private final JCheckBox cbTaskWebLink;

    private final JCheckBox cbTaskResources;

    private final JCheckBox cbTaskNotes;

    private final JCheckBox cbResID;

    private final JCheckBox cbResName;

    private final JCheckBox cbResMail;

    private final JCheckBox cbResPhone;

    private final JCheckBox cbResRole;

    private final GanttProject appli;

    public CSVSettingsPanel(GanttProject parent) {
        super(GanttProject.correctLabel(GanttLanguage.getInstance().getText(
                "csvexport")), GanttLanguage.getInstance().getText(
                "settingsCVSExport"));
        appli = parent;
        vb.add(new JSeparator());
        JPanel genePanel = new JPanel(new BorderLayout());
        JLabel lblSeparatedField = new JLabel(language
                .getText("separatedFields"));
        lblSeparatedField.setFont(new Font(lblSeparatedField.getFont()
                .getFontName(), Font.BOLD, lblSeparatedField.getFont()
                .getSize()));
        genePanel.add(lblSeparatedField, BorderLayout.WEST);
        vb.add(genePanel);

        JPanel fixedPanel = new JPanel(new BorderLayout());
        fixedPanel.add(bFixedSize = new JRadioButton(), BorderLayout.WEST);
        // bFixedSize.setEnabled(false);
        bFixedSize.addActionListener(this);
        fixedPanel.add(new JLabel(language.getText("fixedWidth")),
                BorderLayout.CENTER);
        vb.add(fixedPanel);

        JPanel separatedPanel = new JPanel(new BorderLayout());
        separatedPanel.add(bSeparatedText = new JRadioButton(),
                BorderLayout.WEST);
        bSeparatedText.addActionListener(this);
        separatedPanel.add(new JLabel(language.getText("separated")),
                BorderLayout.CENTER);
        vb.add(separatedPanel);

        JPanel separatorFieldPanel = new JPanel(new GridBagLayout());
        vb.add(separatorFieldPanel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 15;
        gbc.insets.left = 10;
        gbc.insets.top = 10;

        addUsingGBL(separatorFieldPanel, bDoubleDot = new JRadioButton(), gbc,
                0, 0, 1, 1);
        bDoubleDot.addActionListener(this);
        addUsingGBL(separatorFieldPanel, new JLabel(language
                .getText("doubledot")), gbc, 1, 0, 1, 1);
        addUsingGBL(separatorFieldPanel, bDotComa = new JRadioButton(), gbc, 3,
                0, 1, 1);
        bDotComa.addActionListener(this);
        addUsingGBL(separatorFieldPanel,
                new JLabel(language.getText("dotComa")), gbc, 4, 0, 1, 1);
        addUsingGBL(separatorFieldPanel, bComa = new JRadioButton(), gbc, 6, 0,
                1, 1);
        bComa.addActionListener(this);
        addUsingGBL(separatorFieldPanel, new JLabel(language.getText("coma")),
                gbc, 7, 0, 1, 1);
        bSpace = new JRadioButton();
        // addUsingGBL(separatorFieldPanel, bSpace , gbc, 0, 1, 1, 1);
        bSpace.addActionListener(this);
        // addUsingGBL(separatorFieldPanel, new
        // JLabel(language.getText("space")), gbc, 1, 1, 1, 1);
        addUsingGBL(separatorFieldPanel, bOther = new JRadioButton(), gbc, 6,
                1, 1, 1);
        bOther.addActionListener(this);
        addUsingGBL(separatorFieldPanel, new JLabel(language.getText("other")),
                gbc, 7, 1, 1, 1);
        addUsingGBL(separatorFieldPanel, tfOther = new JTextField(5), gbc, 8,
                1, 1, 1);

        JPanel textSeparatorFieldPanel = new JPanel(new FlowLayout());
        vb.add(textSeparatorFieldPanel);
        textSeparatorFieldPanel.add(new JLabel(language
                .getText("textSeparator")));
        cbTextSeparator = new JComboBox(appli.getOptions().getCSVOptions()
                .getSeparatedTextChars());
        textSeparatorFieldPanel.add(new JLabel("  "));
        textSeparatorFieldPanel.add(cbTextSeparator);

        vb.add(new JPanel());

        vb.add(new JSeparator());
        JPanel taskPanel = new JPanel(new BorderLayout());
        JLabel lblTaskField = new JLabel(language.getText("taskFields"));
        lblTaskField.setFont(new Font(lblTaskField.getFont().getFontName(),
                Font.BOLD, lblTaskField.getFont().getSize()));
        taskPanel.add(lblTaskField, BorderLayout.WEST);
        vb.add(taskPanel);
        JPanel taskFieldPanel = new JPanel(new GridBagLayout());
        vb.add(taskFieldPanel);
        addUsingGBL(taskFieldPanel, cbTaskID = new JCheckBox(), gbc, 0, 0, 1, 1);
        addUsingGBL(taskFieldPanel, new JLabel(language.getText("id")), gbc, 1,
                0, 1, 1);
        addUsingGBL(taskFieldPanel, cbTaskName = new JCheckBox(), gbc, 3, 0, 1,
                1);
        addUsingGBL(taskFieldPanel, new JLabel(language.getText("name")), gbc,
                4, 0, 1, 1);
        addUsingGBL(taskFieldPanel, cbTaskDuration = new JCheckBox(), gbc, 6,
                0, 1, 1);
        addUsingGBL(taskFieldPanel, new JLabel(language.getText("length")),
                gbc, 7, 0, 1, 1);
        addUsingGBL(taskFieldPanel, cbStartDate = new JCheckBox(), gbc, 0, 1,
                1, 1);
        addUsingGBL(taskFieldPanel, new JLabel(language
                .getText("dateOfBegining")), gbc, 1, 1, 1, 1);
        addUsingGBL(taskFieldPanel, cbEndDate = new JCheckBox(), gbc, 3, 1, 1,
                1);
        addUsingGBL(taskFieldPanel, new JLabel(language.getText("dateOfEnd")),
                gbc, 4, 1, 1, 1);
        addUsingGBL(taskFieldPanel, cbTaskPercent = new JCheckBox(), gbc, 6, 1,
                1, 1);
        addUsingGBL(taskFieldPanel,
                new JLabel(language.getText("advancement")), gbc, 7, 1, 1, 1);
        addUsingGBL(taskFieldPanel, cbTaskWebLink = new JCheckBox(), gbc, 0, 2,
                1, 1);
        addUsingGBL(taskFieldPanel, new JLabel(language.getText("webLink")),
                gbc, 1, 2, 1, 1);
        addUsingGBL(taskFieldPanel, cbTaskResources = new JCheckBox(), gbc, 3,
                2, 1, 1);
        addUsingGBL(taskFieldPanel, new JLabel(language.getText("resources")),
                gbc, 4, 2, 1, 1);
        addUsingGBL(taskFieldPanel, cbTaskNotes = new JCheckBox(), gbc, 6, 2,
                1, 1);
        addUsingGBL(taskFieldPanel, new JLabel(language.getText("notes")), gbc,
                7, 2, 1, 1);

        vb.add(new JPanel());

        vb.add(new JSeparator());
        JPanel resPanel = new JPanel(new BorderLayout());
        JLabel lblResField = new JLabel(language.getText("resFields"));
        lblResField.setFont(new Font(lblResField.getFont().getFontName(),
                Font.BOLD, lblResField.getFont().getSize()));
        resPanel.add(lblResField, BorderLayout.WEST);
        vb.add(resPanel);
        JPanel resFieldPanel = new JPanel(new GridBagLayout());
        vb.add(resFieldPanel);
        addUsingGBL(resFieldPanel, cbResID = new JCheckBox(), gbc, 0, 0, 1, 1);
        addUsingGBL(resFieldPanel, new JLabel(language.getText("id")), gbc, 1,
                0, 1, 1);
        addUsingGBL(resFieldPanel, cbResName = new JCheckBox(), gbc, 3, 0, 1, 1);
        addUsingGBL(resFieldPanel, new JLabel(language.getText("colName")),
                gbc, 4, 0, 1, 1);
        addUsingGBL(resFieldPanel, cbResMail = new JCheckBox(), gbc, 6, 0, 1, 1);
        addUsingGBL(resFieldPanel, new JLabel(language.getText("colMail")),
                gbc, 7, 0, 1, 1);
        addUsingGBL(resFieldPanel, cbResPhone = new JCheckBox(), gbc, 0, 1, 1,
                1);
        addUsingGBL(resFieldPanel, new JLabel(language.getText("colPhone")),
                gbc, 1, 1, 1, 1);
        addUsingGBL(resFieldPanel, cbResRole = new JCheckBox(), gbc, 3, 1, 1, 1);
        addUsingGBL(resFieldPanel, new JLabel(language.getText("colRole")),
                gbc, 4, 1, 1, 1);

        applyComponentOrientation(language.getComponentOrientation());
    }

    /** add a component to container by using GridBagConstraints. */
    private void addUsingGBL(Container container, Component component,
            GridBagConstraints gbc, int x, int y, int w, int h) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.weighty = 0;
        container.add(component, gbc);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.ganttproject.gui.options.GeneralOptionPanel#applyChanges(boolean)
     */
    public boolean applyChanges(boolean askForApply) {
        CSVOptions csvOptions = appli.getOptions().getCSVOptions();

        if (getFixed() == csvOptions.bFixedSize
                && getTaskID() == csvOptions.bExportTaskID
                && getTaskName() == csvOptions.bExportTaskName
                && getTaskSD() == csvOptions.bExportTaskStartDate
                && getTaskED() == csvOptions.bExportTaskEndDate
                && getTaskDuration() == csvOptions.bExportTaskDuration
                && getTaskPercent() == csvOptions.bExportTaskPercent
                && getTaskWebLink() == csvOptions.bExportTaskWebLink
                && getTaskResources() == csvOptions.bExportTaskResources
                && getTaskNotes() == csvOptions.bExportTaskNotes
                && getResourceID() == csvOptions.bExportResourceID
                && getResourceName() == csvOptions.bExportResourceName
                && getResourceMail() == csvOptions.bExportResourceMail
                && getResourcePhone() == csvOptions.bExportResourcePhone
                && getResourceRole() == csvOptions.bExportResourceRole
                && !separatCharHasChange()
                && getTextSeparat().equals(csvOptions.sSeparatedTextChar)) {
            bHasChange = false;
        } else {
            if (!askForApply || (askForApply && askForApplyChanges())) {
                csvOptions.sSeparatedTextChar = getTextSeparat();
                csvOptions.sSeparatedChar = getSeparat();
                csvOptions.bFixedSize = getFixed();
                csvOptions.bExportTaskID = getTaskID();
                csvOptions.bExportTaskName = getTaskName();
                csvOptions.bExportTaskStartDate = getTaskSD();
                csvOptions.bExportTaskEndDate = getTaskED();
                csvOptions.bExportTaskDuration = getTaskDuration();
                csvOptions.bExportTaskPercent = getTaskPercent();
                csvOptions.bExportTaskWebLink = getTaskWebLink();
                csvOptions.bExportTaskResources = getTaskResources();
                csvOptions.bExportTaskNotes = getTaskNotes();
                csvOptions.bExportResourceID = getResourceID();
                csvOptions.bExportResourceName = getResourceName();
                csvOptions.bExportResourceMail = getResourceMail();
                csvOptions.bExportResourcePhone = getResourcePhone();
                csvOptions.bExportResourceRole = getResourceRole();
            }
            bHasChange = true;
        }
        return bHasChange;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.ganttproject.gui.options.GeneralOptionPanel#initialize()
     */
    public void initialize() {

        cbTaskID.setSelected(appli.getOptions().getCSVOptions().bExportTaskID);
        cbTaskName
                .setSelected(appli.getOptions().getCSVOptions().bExportTaskName);
        cbStartDate
                .setSelected(appli.getOptions().getCSVOptions().bExportTaskStartDate);
        cbEndDate
                .setSelected(appli.getOptions().getCSVOptions().bExportTaskEndDate);
        cbTaskPercent
                .setSelected(appli.getOptions().getCSVOptions().bExportTaskPercent);
        cbTaskDuration
                .setSelected(appli.getOptions().getCSVOptions().bExportTaskDuration);
        cbTaskWebLink
                .setSelected(appli.getOptions().getCSVOptions().bExportTaskWebLink);
        cbTaskResources
                .setSelected(appli.getOptions().getCSVOptions().bExportTaskResources);
        cbTaskNotes
                .setSelected(appli.getOptions().getCSVOptions().bExportTaskNotes);

        cbResID
                .setSelected(appli.getOptions().getCSVOptions().bExportResourceID);
        cbResName
                .setSelected(appli.getOptions().getCSVOptions().bExportResourceName);
        cbResMail
                .setSelected(appli.getOptions().getCSVOptions().bExportResourceMail);
        cbResPhone
                .setSelected(appli.getOptions().getCSVOptions().bExportResourcePhone);
        cbResRole
                .setSelected(appli.getOptions().getCSVOptions().bExportResourceRole);

        boolean bfixed = appli.getOptions().getCSVOptions().bFixedSize;

        if (bfixed) {
            bFixedSize.setSelected(true);
            enableSeparatedButton(false);
        } else {
            bSeparatedText.setSelected(true);
            enableSeparatedButton(true);
        }

        String sSeparatedChar = appli.getOptions().getCSVOptions().sSeparatedChar;

        if (",".equals(sSeparatedChar)) {
            unselectOther(bComa);
        } else if (";".equals(sSeparatedChar)) {
            unselectOther(bDotComa);
        } else if (":".equals(sSeparatedChar)) {
            unselectOther(bDoubleDot);
        } else if (" ".equals(sSeparatedChar)) {
            unselectOther(bSpace);
        } else {
            unselectOther(bOther);
            tfOther.setText(sSeparatedChar);
        }
        if ("\"".equals(appli.getOptions().getCSVOptions().sSeparatedTextChar)) {
            cbTextSeparator.setSelectedIndex(1);
        }

    }

    /** Action performed. */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JRadioButton) {
            JRadioButton selectedButton = (JRadioButton) e.getSource();
            if (!selectedButton.isSelected())
                selectedButton.setSelected(true);
        }

        if (e.getSource() == bFixedSize && bFixedSize.isSelected()) {
            bSeparatedText.setSelected(false);
            enableSeparatedButton(false);
        } else if (e.getSource() == bSeparatedText
                && bSeparatedText.isSelected()) {
            bFixedSize.setSelected(false);
            enableSeparatedButton(true);
        } else if (e.getSource() == bDoubleDot && bDoubleDot.isSelected()) {
            unselectOther(bDoubleDot);
        } else if (e.getSource() == bDotComa && bDotComa.isSelected()) {
            unselectOther(bDotComa);
        } else if (e.getSource() == bComa && bComa.isSelected()) {
            unselectOther(bComa);
        } else if (e.getSource() == bSpace && bSpace.isSelected()) {
            unselectOther(bSpace);
        } else if (e.getSource() == bOther && bOther.isSelected()) {
            unselectOther(bOther);
        }
    }

    public void unselectOther(JRadioButton selectedButton) {
        bDoubleDot.setSelected(selectedButton == bDoubleDot);
        bDotComa.setSelected(selectedButton == bDotComa);
        bComa.setSelected(selectedButton == bComa);
        bSpace.setSelected(selectedButton == bSpace);
        bOther.setSelected(selectedButton == bOther);
        tfOther.setEnabled(selectedButton == bOther);
    }

    public void enableSeparatedButton(boolean enabled) {
        bDoubleDot.setEnabled(enabled);
        bDotComa.setEnabled(enabled);
        bComa.setEnabled(enabled);
        bSpace.setEnabled(enabled);
        bOther.setEnabled(enabled);
        tfOther.setEnabled(enabled && bOther.isSelected());
    }

    public boolean getFixed() {
        return bFixedSize.isSelected();
    }

    public boolean getTaskID() {
        return cbTaskID.isSelected();
    }

    public boolean getTaskName() {
        return cbTaskName.isSelected();
    }

    public boolean getTaskSD() {
        return cbStartDate.isSelected();
    }

    public boolean getTaskED() {
        return cbEndDate.isSelected();
    }

    public boolean getTaskPercent() {
        return cbTaskPercent.isSelected();
    }

    public boolean getTaskDuration() {
        return cbTaskDuration.isSelected();
    }

    public boolean getTaskWebLink() {
        return cbTaskWebLink.isSelected();
    }

    public boolean getTaskResources() {
        return cbTaskResources.isSelected();
    }

    public boolean getTaskNotes() {
        return cbTaskNotes.isSelected();
    }

    public boolean getResourceID() {
        return cbResID.isSelected();
    }

    public boolean getResourceName() {
        return cbResName.isSelected();
    }

    public boolean getResourcePhone() {
        return cbResPhone.isSelected();
    }

    public boolean getResourceMail() {
        return cbResMail.isSelected();
    }

    public boolean getResourceRole() {
        return cbResRole.isSelected();
    }

    public boolean separatCharHasChange() {
        CSVOptions csvOptions = appli.getOptions().getCSVOptions();
        if (bDoubleDot.isSelected() && csvOptions.sSeparatedChar.equals(":")) {
            return false;
        }
        if (bComa.isSelected() && csvOptions.sSeparatedChar.equals(",")) {
            return false;
        }
        if (bDotComa.isSelected() && csvOptions.sSeparatedChar.equals(";")) {
            return false;
        }
        if (bSpace.isSelected() && csvOptions.sSeparatedChar.equals(" ")) {
            return false;
        }
        if (bOther.isSelected()
                && csvOptions.sSeparatedChar.equals(tfOther.getText())) {
            return false;
        }
        return true;
    }

    public String getTextSeparat() {
        if (cbTextSeparator.getSelectedIndex() == 0) {
            return "\'";
        }
        return "\"";
    }

    public String getSeparat() {
        if (bDoubleDot.isSelected())
            return ":";
        if (bComa.isSelected())
            return ",";
        if (bDotComa.isSelected())
            return ";";
        if (bSpace.isSelected())
            return " ";
        return tfOther.getText();
    }
}
