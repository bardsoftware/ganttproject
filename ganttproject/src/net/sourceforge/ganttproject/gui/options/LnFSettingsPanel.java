/***************************************************************************
 LnFSettingsPanel 
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sourceforge.ganttproject.GPToolBar;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.GanttLookAndFeelInfo;
import net.sourceforge.ganttproject.gui.GanttLookAndFeels;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas Panel to choose the look'n'feel for GanttProject
 */
public class LnFSettingsPanel extends GeneralOptionPanel implements
        ItemListener {

    // combo box to store all look'n'feel data
    private final JComboBox cbLnf;

    private final JCheckBox cbSmallIcon;

    private final JComboBox cbButtonType;

//    private final JCheckBox cbShowStatus;

    private final JList list;

    private DefaultListModel iconList;

    private DefaultListModel buttonList;

    private final JList listDeleted;

    private DefaultListModel deletedIconList;

    private DefaultListModel deletedButtonList;

    private boolean bIconMoved = false;

    private final GanttProject appli;

    public LnFSettingsPanel(GanttProject parent) {
        super(GanttLanguage.getInstance().getText("looknfeel"), GanttLanguage
                .getInstance().getText("settingsLooknFeel"));

        appli = parent;

        // create the combo box with all languages
        cbLnf = new JComboBox();
        cbLnf.setName("comboLnf");
        // cbLnf.addActionListener(this);

        GanttLookAndFeelInfo[] lookAndFeels = GanttLookAndFeels
                .getGanttLookAndFeels().getInstalledLookAndFeels();
        for (int i = 0; i < lookAndFeels.length; i++) {
            cbLnf.addItem(lookAndFeels[i]);
        }

        cbLnf.addItemListener(this);

        JPanel languagePanel = new JPanel(new BorderLayout());
        languagePanel.add(cbLnf, BorderLayout.NORTH);
        vb.add(languagePanel);
        vb.add(new JPanel());

        // use small icons

        JPanel iconTextPanel = new JPanel(new FlowLayout());
        iconTextPanel.add(new JLabel(language.getText("show")));
        iconTextPanel.add(cbButtonType = new JComboBox());

        cbButtonType.addItem(language.getText("buttonIcon"));
        cbButtonType.addItem(language.getText("buttonIconText"));
        cbButtonType.addItem(language.getText("buttonText"));

        iconTextPanel.add(new JLabel("  "));
        iconTextPanel.add(cbSmallIcon = new JCheckBox());
        iconTextPanel.add(new JLabel(language.getText("useSmalIcons")));
        JPanel iconTextPanel2 = new JPanel(new BorderLayout());
        iconTextPanel2.add(iconTextPanel, BorderLayout.WEST);
        vb.add(iconTextPanel2);
        vb.add(new JPanel());

//        // status bar setting
//        JPanel statusPanel = new JPanel(new BorderLayout());
//        statusPanel.add(cbShowStatus = new JCheckBox(), BorderLayout.WEST);
//        statusPanel.add(new JLabel(language.getText("showStatusBar")),
//                BorderLayout.CENTER);
//        vb.add(statusPanel);
//        vb.add(new JPanel());
        JPanel positionPanel = new JPanel(new FlowLayout());
        JPanel currentPanel = new JPanel(new BorderLayout());
        JPanel deletedPanel = new JPanel(new BorderLayout());

        currentPanel.add(new JLabel(language.getText("currentToolBar")),
                BorderLayout.NORTH);
        deletedPanel.add(new JLabel(language.getText("availableToolBar")),
                BorderLayout.NORTH);

        /*
         * iconList = new DefaultListModel (); buttonList = new DefaultListModel
         * (); for (int i = 0 ; i < appli.getButtonList().size() ; i++)
         * buttonList.addElement(appli.getButtonList().getElementAt(i)); for
         * (int i = 0 ; i < buttonList.size() ; i++) { if
         * (buttonList.elementAt(i).equals(language.getText("separator").toUpperCase()))
         * iconList.addElement (buttonList.getElementAt(i)); else { if
         * (((TestGanttRolloverButton)buttonList.getElementAt(i)).getIcon() ==
         * null) iconList.addElement
         * (((TestGanttRolloverButton)buttonList.getElementAt(i)).getText());
         * else iconList.addElement
         * (((TestGanttRolloverButton)buttonList.getElementAt(i)).getIcon()); } }
         */

        list = new JList();
        listDeleted = new JList();
        listDeleted.setName("listDeleted");

        JScrollPane scrollPane = new JScrollPane(listDeleted);
        scrollPane.setPreferredSize(new Dimension(110, 200));
        deletedPanel.add(scrollPane, BorderLayout.EAST);

        // list = new JList(iconList);
        list.setName("list");
        scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(110, 200));
        currentPanel.add(scrollPane, BorderLayout.EAST);

        Box currentButtonBox = Box.createVerticalBox();
        Box deletedButtonBox = Box.createVerticalBox();

        JButton bUp = new JButton();
        bUp.setIcon(new ImageIcon(getClass().getResource("/icons/up_16.gif")));
        bUp.setToolTipText(GanttProject.getToolTip(GanttProject
                .correctLabel(language.getText("upTask"))));
        bUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upButtonActionPerformed(evt);
            }
        });

        JButton bDown = new JButton();
        bDown.setIcon(new ImageIcon(getClass()
                .getResource("/icons/down_16.gif")));
        bDown.setToolTipText(GanttProject.getToolTip(GanttProject
                .correctLabel(language.getText("downTask"))));
        bDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downButtonActionPerformed(evt);
            }
        });

        JButton bSeparator = new JButton();
        bSeparator.setIcon(new ImageIcon(getClass().getResource(
                "/icons/separator_16.gif")));
        bSeparator.setToolTipText(GanttProject.getToolTip(GanttProject
                .correctLabel(language.getText("separator"))));
        bSeparator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                separatorButtonActionPerformed(evt);
            }
        });

        JButton bDelete = new JButton();
        bDelete.setIcon(new ImageIcon(getClass().getResource(
                "/icons/indent_16.gif")));
        bDelete.setToolTipText(GanttProject.getToolTip(GanttProject
                .correctLabel(language.getText("delete"))));
        bDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });
        JButton bAdd = new JButton();
        bAdd.setIcon(new ImageIcon(getClass().getResource(
                "/icons/unindent_16.gif")));
        bAdd.setToolTipText(GanttProject.getToolTip(GanttProject
                .correctLabel(language.getText("add"))));
        bAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        currentButtonBox.add(new JPanel());
        currentButtonBox.add(new JPanel());
        currentButtonBox.add(bUp);
        currentButtonBox.add(new JPanel());
        currentButtonBox.add(bDown);
        currentButtonBox.add(new JPanel());
        currentButtonBox.add(new JPanel());
        deletedButtonBox.add(new JPanel());
        deletedButtonBox.add(new JPanel());
        deletedButtonBox.add(bAdd);
        deletedButtonBox.add(new JPanel());
        deletedButtonBox.add(bDelete);
        deletedButtonBox.add(new JPanel());
        deletedButtonBox.add(new JPanel());

        positionPanel.add(currentButtonBox);
        positionPanel.add(currentPanel);
        positionPanel.add(new JPanel());
        positionPanel.add(deletedButtonBox);
        positionPanel.add(new JPanel());
        positionPanel.add(deletedPanel);
        positionPanel.add(new JPanel());
        vb.add(positionPanel);
        vb.add(new JPanel());
        applyComponentOrientation(language.getComponentOrientation());
    }

    /** This method check if the value has changed, and ask for commit changes. */
    public boolean applyChanges(boolean askForApply) {
        if (cbSmallIcon.isSelected() != appli.getOptions().getIconSize()
                .equals("16")) {
            bHasChange = true;
        }

        if (cbButtonType.getSelectedIndex() != appli.getOptions()
                .getButtonShow()) {
            bHasChange = true;
        }

//        if (getShowStatusBar() != appli.getOptions().getShowStatusBar())
//            bHasChange = true;
        if (bIconMoved) {
            bHasChange = true;
        }
        // if there is changes
        if (bHasChange) {
            if (!askForApply || (askForApply && askForApplyChanges())) {
                appli.changeLookAndFeel(getLookAndFeel());
                appli.changeOrder(buttonList, deletedButtonList);
                appli.getOptions().setIconSize(
                        cbSmallIcon.isSelected() ? "16" : "24");
                appli.getOptions().setButtonShow(
                        cbButtonType.getSelectedIndex());
                //appli.getOptions().setShowStatusBar(cbShowStatus.isSelected());
                //appli.getStatusBar().setVisible(getShowStatusBar());
                appli.applyButtonOptions();

            }
        }
        return bHasChange;
    }

    /** The look'n'feel has changed. */
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            this.bHasChange = true;
        }
    }

    /** Return the class of the style */
    public GanttLookAndFeelInfo getLookAndFeel() {
        return ((GanttLookAndFeelInfo) cbLnf.getSelectedItem());
    }

//    boolean getShowStatusBar() {
//        return cbShowStatus.isSelected();
//    }

    /** Initialize the component. */
    public void initialize() {
        cbLnf.setSelectedItem(appli.lookAndFeel);
        cbSmallIcon.setSelected(appli.getOptions().getIconSize().equals("16"));
        cbButtonType.setSelectedIndex(appli.getOptions().getButtonShow());
//        cbShowStatus.setSelected(appli.getOptions().getShowStatusBar());
        deletedIconList = new DefaultListModel();
        deletedIconList.addElement(GPToolBar.SEPARATOR_OBJECT);
        deletedButtonList = new DefaultListModel();
        deletedButtonList.addElement(GPToolBar.SEPARATOR_OBJECT);
        for (int i = 0; i < appli.getDeletedButtonList().size(); i++) {
            if (GPToolBar.SEPARATOR_OBJECT!=appli.getDeletedButtonList().getElementAt(i)) {
                deletedButtonList.addElement(appli.getDeletedButtonList()
                        .getElementAt(i));
            }
        }
        for (int i = 1; i < deletedButtonList.size(); i++) {
            if (((TestGanttRolloverButton) deletedButtonList.getElementAt(i))
                    .getIcon() == null) {
                deletedIconList
                        .addElement(((TestGanttRolloverButton) deletedButtonList
                                .getElementAt(i)).getText());
            } else {
                deletedIconList
                        .addElement(((TestGanttRolloverButton) deletedButtonList
                                .getElementAt(i)).getIcon());
            }
        }
        iconList = new DefaultListModel();
        buttonList = new DefaultListModel();
        for (int i = 0; i < appli.getButtonList().size(); i++) {
            buttonList.addElement(appli.getButtonList().getElementAt(i));
        }
        for (int i = 0; i < buttonList.size(); i++) {
            if (buttonList.elementAt(i).equals(
                    GPToolBar.SEPARATOR_OBJECT)) {
                iconList.addElement(buttonList.getElementAt(i));
            } else {
                if (((TestGanttRolloverButton) buttonList.getElementAt(i))
                        .getIcon() == null) {
                    iconList.addElement(((TestGanttRolloverButton) buttonList
                            .getElementAt(i)).getText());
                } else {
                    iconList.addElement(((TestGanttRolloverButton) buttonList
                            .getElementAt(i)).getIcon());
                }
            }
        }

        list.setModel(iconList);
        listDeleted.setModel(deletedIconList);
        bHasChange = false;
    }

    /** Action on click the up button. */
    private void upButtonActionPerformed(java.awt.event.ActionEvent evt) {
        Object[] objects = list.getSelectedValues();
        int[] selection = list.getSelectedIndices();
        if (objects.length > 0) {
            if (selection[0] != 0) {
                list.ensureIndexIsVisible(selection[0]);
                for (int i = 0; i < objects.length; i++) {
                    int index = selection[i];
                    iconList.setElementAt(iconList.getElementAt(index - 1),
                            index);
                    iconList.setElementAt(objects[i], index - 1);

                    Object object = buttonList.getElementAt(index);
                    buttonList.setElementAt(buttonList.getElementAt(index - 1),
                            index);
                    buttonList.setElementAt(object, index - 1);

                    selection[i] = index - 1;
                }
                list.setSelectedIndices(selection);
            }
            bIconMoved = true;
        }
    }

    /** Action on click the down button. */
    private void downButtonActionPerformed(java.awt.event.ActionEvent evt) {
        Object[] objects = list.getSelectedValues();
        int[] selection = list.getSelectedIndices();
        if (objects.length > 0) {
            if (selection[selection.length - 1] != iconList.size() - 1) {
                list.ensureIndexIsVisible(selection[selection.length - 1]);
                for (int i = objects.length - 1; i > -1; i--) {
                    int index = selection[i];
                    iconList.setElementAt(iconList.getElementAt(index + 1),
                            index);
                    iconList.setElementAt(objects[i], index + 1);

                    Object object = buttonList.getElementAt(index);
                    buttonList.setElementAt(buttonList.getElementAt(index + 1),
                            index);
                    buttonList.setElementAt(object, index + 1);
                    selection[i] = index + 1;
                }
                list.setSelectedIndices(selection);
            }
            bIconMoved = true;
        }
    }

    /** Add a new separator. */
    private void separatorButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int index = 0;
        if (list.getSelectedIndex() != -1) {
            index = list.getSelectedIndex();
        }
        iconList.add(index, GPToolBar.SEPARATOR_OBJECT);

        buttonList.add(index, GPToolBar.SEPARATOR_OBJECT);
        list.setSelectedIndex(index);
        bIconMoved = true;
    }

    /** Remove the selected button on the list. */
    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {

        int[] indexes = list.getSelectedIndices();

        if (indexes.length != 0) {
            for (int i = 0; i < indexes.length; i++) {

                indexes[i] = indexes[i] - i;
                Object icon = iconList.remove(indexes[i]);
                Object button = buttonList.remove(indexes[i]);
                if (icon.getClass() != String.class) {
                    deletedIconList.addElement(icon);
                    deletedButtonList.addElement(button);

                }

            }
            listDeleted.setSelectedIndex(deletedIconList.getSize() - 1);
            if (iconList.getSize() > 0)
                list.setSelectedIndex(indexes[0]);
            bHasChange = true;
        }
    }

    /** Add available button to the list. */
    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int index = list.getSelectedIndex();
        if (index == -1)
            index = 0;
        int[] indexesDeleted = listDeleted.getSelectedIndices();
        if (indexesDeleted.length != 0) {
            int j = 0;
            for (int i = 0; i < indexesDeleted.length; i++) {
                if (indexesDeleted[i] == 0) {
                    j = 1;
                    indexesDeleted[i] = indexesDeleted[i];
                } else {
                    indexesDeleted[i] = indexesDeleted[i] - i + j;
                }
                Object icon = GPToolBar.SEPARATOR_OBJECT;
                Object button = GPToolBar.SEPARATOR_OBJECT;
                if (deletedIconList.getElementAt(indexesDeleted[i]).getClass() != String.class) {
                    icon = deletedIconList.remove(indexesDeleted[i]);
                    button = deletedButtonList.remove(indexesDeleted[i]);
                }
                // System.out.println (index + " + " + i + " = " + index + i);
                if (iconList.getSize() != index + i) {
                    iconList.add(index + i + 1, icon);
                    buttonList.add(index + i + 1, button);
                } else {
                    iconList.add(index + i, icon);
                    buttonList.add(index + i, button);
                }

            }
            if (iconList.getSize() != index + 1) {
                list.setSelectedIndex(index + 1);
            } else {
                list.setSelectedIndex(index);
            }

            if (deletedIconList.getSize() <= indexesDeleted[0]) {
                listDeleted.setSelectedIndex(0);
            } else {
                listDeleted.setSelectedIndex(indexesDeleted[0]);
            }

            bHasChange = true;
        }
    }
}
