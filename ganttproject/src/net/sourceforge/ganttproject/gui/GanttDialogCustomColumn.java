/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Date;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sourceforge.ganttproject.CustomPropertyClass;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.Mediator;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.DateOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultDateOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.StringOption;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Dialog opened to create a new customColumn.
 *
 * @author bbaranne
 */
public class GanttDialogCustomColumn  {
    private static GanttLanguage language = GanttLanguage.getInstance();

    private JPanel panelDefaultValue = null;

    private CardLayout cardLayoutDefaultValue = null;

    private JPanel panelText = null;

    private JPanel panelInteger = null;

    private JPanel panelDouble = null;

    private Component panelDate = null;

    private JPanel panelBoolean = null;

    private JTextArea textArea = null;

    private GTextField fieldInteger = null;

    private GTextField fieldDouble = null;

    private JRadioButton radioTrue = null;

    private JRadioButton radioFalse = null;

    //private GanttCalendar date = null;
    private DateOption myDate = new DefaultDateOption("taskProperties.customColumn.defaultDate") {
        @Override
        public void setValue(Date value) {
            super.setValue(value);
            commit();
            lock();
        }
    };

    private StringOption myName = new DefaultStringOption("taskProperties.customColumn.name") {
        @Override
        public void setValue(String value) {
            super.setValue(value);
            commit();
            lock();
        }
    };

    private EnumerationOption myType = new DefaultEnumerationOption<CustomPropertyClass>(
            "taskProperties.customColumn.type", CustomPropertyClass.values()) {
        @Override
        public void setValue(String value) {
            super.setValue(value);
            int selectedIndex = getSelectedIndex(value);
            switch (selectedIndex) {
            case 0:
                cardLayoutDefaultValue.show(panelDefaultValue, CustomPropertyClass.TEXT.getJavaClass().getName());
                break;
            case 1:
                cardLayoutDefaultValue.show(panelDefaultValue, CustomPropertyClass.INTEGER.getJavaClass().getName());
                break;
            case 2:
                cardLayoutDefaultValue.show(panelDefaultValue, CustomPropertyClass.DOUBLE.getJavaClass().getName());
                break;
            case 3:
                cardLayoutDefaultValue.show(panelDefaultValue, CustomPropertyClass.DATE.getJavaClass().getName());
                break;
            case 4:
                cardLayoutDefaultValue.show(panelDefaultValue, CustomPropertyClass.BOOLEAN.getJavaClass().getName());
                break;
            }
        }
        private int getSelectedIndex(String value) {
            return getSelectedType(value);
        }
    };

    private final UIFacade myUIFacade;
    private final GPOption[] myOptions = new GPOption[] {myName, myType};
    private final GPOptionGroup myOptionGroup = new GPOptionGroup("taskProperties.customColumn", myOptions);
    private boolean isOk;

    private CustomPropertyManager myCustomPropertyManager;

    public GanttDialogCustomColumn(UIFacade uiFacade, CustomPropertyManager customPropertyManager) {
        myCustomPropertyManager = customPropertyManager;
        myUIFacade = uiFacade;
        myOptionGroup.lock();
        myDate.lock();
        myDate.setValue(new Date());
        myName.setValue("");
        myOptionGroup.setTitled(false);
        isOk = false;
    }


    private int getSelectedType(String typeName) {
        for (CustomPropertyClass columnClass : CustomPropertyClass.values()) {
            if (columnClass.getDisplayName().equals(typeName)) {
                return columnClass.ordinal();
            }
        }
        return -1;

    }

    public void setVisible(boolean visible) {
        Component rootComponent = getComponent();
        getUIFacade().createDialog(rootComponent, new Action[] {
                new OkAction() {
                    public void actionPerformed(ActionEvent e) {
                        myOptionGroup.commit();
                        myDate.commit();
                        GanttDialogCustomColumn.this.ok();
                    }},
                new CancelAction() {
                    public void actionPerformed(ActionEvent e) {
                        myOptionGroup.rollback();
                        myDate.rollback();
                    }
        }}, "").show();
    }

    private UIFacade getUIFacade() {
        return myUIFacade;
    }
    private Component getComponent() {
        JPanel result = new JPanel();
        GridBagConstraints constraints = new GridBagConstraints();

        OptionsPageBuilder builder = new OptionsPageBuilder();

        {
            // Text
            textArea = new JTextArea();
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setColumns(25);
            textArea.setRows(5);
            JScrollPane jsp = new JScrollPane(textArea);
            panelText = new JPanel(new BorderLayout());
            panelText.add(jsp, BorderLayout.CENTER);
        }
        {
            // Boolean
            radioTrue = new JRadioButton(language.getText("yes"));
            radioFalse = new JRadioButton(language.getText("no"));
            ButtonGroup group = new ButtonGroup();
            group.add(radioFalse);
            group.add(radioTrue);
            radioTrue.setSelected(true);
            panelBoolean = new JPanel(new GridBagLayout());
            constraints.gridx = 0;
            constraints.gridy = 0;
            panelBoolean.add(radioTrue, constraints);
            constraints.gridx = 0;
            constraints.gridy = 1;
            panelBoolean.add(radioFalse, constraints);
        }
        {
            // Integer
            fieldInteger = new GTextField();
            fieldInteger.setPattern(GTextField.PATTERN_INTEGER);
            fieldInteger.setColumns(10);
            panelInteger = new JPanel(new GridBagLayout());
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            panelInteger.add(fieldInteger, constraints);
        }
        {
            // Double
            fieldDouble = new GTextField();
            fieldDouble.setPattern(GTextField.PATTERN_DOUBLE);
            fieldDouble.setColumns(10);
            panelDouble = new JPanel(new GridBagLayout());
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            panelDouble.add(fieldDouble, constraints);
        }
        {
            panelDate = builder.createStandaloneOptionPanel(myDate);
            cardLayoutDefaultValue = new CardLayout();
            panelDefaultValue = new JPanel(cardLayoutDefaultValue);
            panelDefaultValue.add(CustomPropertyClass.TEXT.getJavaClass().getName(), panelText);
            panelDefaultValue.add(CustomPropertyClass.BOOLEAN.getJavaClass().getName(), panelBoolean);
            panelDefaultValue.add(CustomPropertyClass.INTEGER.getJavaClass().getName(), panelInteger);
            panelDefaultValue.add(CustomPropertyClass.DOUBLE.getJavaClass().getName(), panelDouble);
            panelDefaultValue.add(CustomPropertyClass.DATE.getJavaClass().getName(), panelDate);
        }

        Component optionsComponent = builder.buildPlanePage(new GPOptionGroup[] {myOptionGroup});
        result.setLayout(new BorderLayout());
        result.add(optionsComponent, BorderLayout.CENTER);
        result.add(panelDefaultValue, BorderLayout.SOUTH);
        return result;
    } // TODO change the default value for custom columns.

    private void ok() {
        Mediator.getGanttProjectSingleton().setAskForSave(true);
        //String colName = fieldName.getText().trim();
        String colName = myName.getValue();
        String defValue = null;

        if (colName.length() != 0) {
            int colType = getSelectedType(myType.getValue());
            CustomPropertyClass propertyClass = CustomPropertyClass.values()[colType];
            switch (propertyClass) {
            case TEXT:
                defValue = textArea.getText();
                break;
            case INTEGER:
                String ti = fieldInteger.getText();
                if (ti.trim().length() == 0) {
                    ti = "0";
                }
                defValue = ti;
                break;
            case DOUBLE:
                String td = fieldDouble.getText();
                if (td.trim().length() == 0) {
                    td = "0.0";
                }
                defValue = td;
                break;
            case DATE:
                defValue = myDate.getValue()==null ? null : myDate.getPersistentValue();
                break;
            case BOOLEAN:
                defValue = new Boolean(radioTrue.isSelected()).toString();
                break;
            default:
                throw new IllegalStateException();
            }

            CustomPropertyDefinition def = myCustomPropertyManager.createDefinition(propertyClass.getID(), colName, defValue);
            ok(def);
            isOk = true;
        }/* else
        {
            fieldName.requestFocus();
            // nothing (the dialog stays opened)
        }
        */
    }

    protected void ok(CustomPropertyDefinition definition) {

    }

    public boolean isOk() {
        return isOk;
    }
}