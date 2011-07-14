/* LICENSE: GPL2
Copyright (C) 2009 Dmitry Barashev

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
package net.sourceforge.ganttproject.gui.tableView;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableCellRenderer;

import net.sourceforge.ganttproject.CustomPropertyClass;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.DefaultCustomPropertyDefinition;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade.Column;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueEvent;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueListener;
import net.sourceforge.ganttproject.gui.options.model.CustomPropertyDefaultValueAdapter;
import net.sourceforge.ganttproject.gui.options.model.DateOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultDateOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.EditableList;
import net.sourceforge.ganttproject.gui.ListAndFieldsPanel;
import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent.SelectionListener;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class ColumnManagerPanel {
    private IsVisibleOption myIsVisibleOption = new IsVisibleOption();
    private NameOption myNameOption = new NameOption();
    private DefaultValueOption myDefaultValueOption = new DefaultValueOption();
    private DateOption myDate = new DefaultDateOption("taskProperties.customColumn.defaultDate");
    private PropertyClassOption myType = new PropertyClassOption();
    private JPanel panelDefaultValue = null;

    private CardLayout cardLayoutDefaultValue = null;
    private final CustomPropertyManager myManager;
    private final TableHeaderUIFacade myVisibleFields;

    public ColumnManagerPanel(CustomPropertyManager columnManager, TableHeaderUIFacade visibleFields) {
        myManager = columnManager;
        myVisibleFields = visibleFields;
    }
    public Component createComponent() {
        List<CustomPropertyDefinition> emptyList = Collections.emptyList();
        List<CustomPropertyDefinition> defs = new ArrayList<CustomPropertyDefinition>();
        createDefaultFieldDefinitions(
               myVisibleFields, myManager.getDefinitions(), defs);
        defs.addAll(myManager.getDefinitions());
        EditableList<CustomPropertyDefinition> props = new EditableList<CustomPropertyDefinition>(
                defs, emptyList) {
            @Override
            protected boolean isEditable(CustomPropertyDefinition t) {
                return ColumnManagerPanel.this.isEditable(t);
            }
            @Override
            protected String getStringValue(CustomPropertyDefinition value) {
                return value.getName();
            }
            @Override
            protected CustomPropertyDefinition createPrototype(Object editValue) {
                return new DefaultCustomPropertyDefinition(String.valueOf(editValue));
            }
            @Override
            protected CustomPropertyDefinition createValue(CustomPropertyDefinition prototype) {
                return myManager.createDefinition(CustomPropertyClass.TEXT.getID(), prototype.getName(), null);
            }
            @Override
            protected void deleteValue(CustomPropertyDefinition value) {
                myManager.deleteDefinition(value);
            }
            @Override
            protected CustomPropertyDefinition updateValue(
                    CustomPropertyDefinition newValue, CustomPropertyDefinition curValue) {
                curValue.setName(newValue.getName());
                return curValue;
            }
            protected Component getTableCellRendererComponent(
                    DefaultTableCellRenderer defaultRenderer, CustomPropertyDefinition def,
                    boolean isSelected, boolean hasFocus, int row) {
                StringBuffer value = new StringBuffer("<html>");
                Column column = myIsVisibleOption.findColumn(def);
                if (column!=null && !column.isVisible()) {
                    value.append("<font color=#cccccc>{0}</font>");
                } else {
                    value.append("{0}");
                }
                value.append("</html>");
                defaultRenderer.setText(MessageFormat.format(value.toString(), getStringValue(def)));
                return defaultRenderer;
            }
        };

        class ShowHideSelectionAction extends GPAction implements SelectionListener<CustomPropertyDefinition> {
            private boolean isShow;
            private List<CustomPropertyDefinition> mySelection;
            public ShowHideSelectionAction(boolean isShow, String name) {
                super(name);
                this.isShow = isShow;
            }
            public void selectionChanged(List<CustomPropertyDefinition> selection) {
                mySelection = selection;
            }
            public void actionPerformed(ActionEvent e) {
                for (CustomPropertyDefinition def: mySelection) {
                    myIsVisibleOption.setVisible(def, isShow);
                }
            }
            @Override
            protected String getIconFilePrefix() {
                return null;
            }
        }
        props.getTableAndActions().addAction(new ShowHideSelectionAction(true, "$Show selected"));
        props.getTableAndActions().addAction(new ShowHideSelectionAction(false, "$Hide selected"));
        myIsVisibleOption.setVisibleFields(myVisibleFields);
        ChangeValueListener defaultValuePanelEnabler = new ChangeValueListener() {
            public void changeValue(ChangeValueEvent event) {
                setDefaultValuePanelEnabled(myDefaultValueOption.isChecked());
            }
        };
        myDefaultValueOption.addChangeValueListener(defaultValuePanelEnabler);
        myType.addChangeValueListener(defaultValuePanelEnabler);
        final JComponent fields = getFieldsComponent();
        ListAndFieldsPanel<CustomPropertyDefinition> listAndFields =
            new ListAndFieldsPanel<CustomPropertyDefinition>(props, fields);
        props.getTableAndActions().addSelectionListener(new SelectionListener<CustomPropertyDefinition>() {
            public void selectionChanged(List<CustomPropertyDefinition> selection) {
                if (selection.size()!=1) {
                    UIUtil.setEnabledTree(fields, false);
                }
                else {
                    CustomPropertyDefinition selectedElement = selection.get(0);
                    UIUtil.setEnabledTree(fields, isEditable(selectedElement));
                    myNameOption.reloadValue(selectedElement);
                    myType.reloadValue(selectedElement);
                    myDefaultValueOption.reloadValue(selectedElement);
                    myIsVisibleOption.reloadValue(selectedElement);
                }
            }
        });
        return listAndFields.getComponent();
    }

    private void createDefaultFieldDefinitions(
            TableHeaderUIFacade tableHeader, List<CustomPropertyDefinition> customFields,
            List<CustomPropertyDefinition> output) {
        LinkedHashMap<String,Column> name2column = new LinkedHashMap<String, Column>();
        for (int i=0; i<tableHeader.getSize(); i++) {
            Column column = tableHeader.getField(i);
            name2column.put(column.getName(), column);
        }
        for (CustomPropertyDefinition def: customFields) {
            name2column.remove(def.getName());
        }
        for (Column column: name2column.values()) {
            output.add(new DefaultCustomPropertyDefinition(column.getName()));
        }
    }

    protected boolean isEditable(CustomPropertyDefinition def) {
        return myManager.getCustomPropertyDefinition(def.getID())!=null;
    }

    protected void setDefaultValuePanelEnabled(boolean enabled) {
        UIUtil.setEnabledTree(panelDefaultValue, enabled);
    }

    private JComponent getFieldsComponent() {
        OptionsPageBuilder builder = new OptionsPageBuilder();
        {
            cardLayoutDefaultValue = new CardLayout();
            panelDefaultValue = new JPanel(cardLayoutDefaultValue);
            panelDefaultValue.setPreferredSize(new Dimension(200, 200));
            myType.setUIControls(cardLayoutDefaultValue, panelDefaultValue);
        }

        Component optionsComponent = builder.createGroupComponent(new GPOptionGroup("customPropertyDialog", new GPOption[] {
            myIsVisibleOption, myNameOption, myType}));

        Box result = Box.createVerticalBox();
        result.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        //result.setLayout(new BorderLayout());
        result.add(optionsComponent);
        result.add(Box.createVerticalStrut(10));
        GPOptionGroup defaultValueGroup = new GPOptionGroup(
            "customPropertyDialog.defaultValue", new GPOption[] {myDefaultValueOption});
        defaultValueGroup.setTitled(false);
        result.add(builder.createGroupComponent(defaultValueGroup));
        result.add(panelDefaultValue);
        setDefaultValuePanelEnabled(false);
        return result;
    }

    static class IsVisibleOption extends DefaultBooleanOption {
        private TableHeaderUIFacade myVisibleFields;
        private Column myColumn;

        IsVisibleOption() {
            super("customPropertyDialog.isVisible");
        }

        public void setVisibleFields(TableHeaderUIFacade visibleFields) {
            myVisibleFields = visibleFields;
        }
        public void reloadValue(CustomPropertyDefinition selectedElement) {
            myColumn = findColumn(selectedElement);
            assert myColumn!=null;
            setValue(myColumn.isVisible(), true);
        }

        @Override
        public void setValue(Boolean value) {
            super.setValue(value);
            if (isChanged()) {
                myColumn.setVisible(isChecked());
            }
        }

        void setVisible(CustomPropertyDefinition def, boolean isVisible) {
            Column column = findColumn(def);
            if (column!=null) {
                column.setVisible(isVisible);
            }
        }

        Column findColumn(CustomPropertyDefinition def) {
            for (int i=0; i<myVisibleFields.getSize(); i++) {
                Column nextColumn = myVisibleFields.getField(i);
                if (nextColumn.getName().equals(def.getName())) {
                    return nextColumn;
                }
            }
            return null;
        }
    }

    static class NameOption extends DefaultStringOption {
        CustomPropertyDefinition myDefinition;
        NameOption() {
            super("customPropertyDialog.name");
        }

        public void reloadValue(CustomPropertyDefinition selectedElement) {
            myDefinition = selectedElement;
            setValue(myDefinition.getName(), true);
        }

        @Override
        public void setValue(String value) {
            super.setValue(value);
            if (isChanged()) {
                myDefinition.setName(value);
            }
        }

    }
    static class DefaultValueOption extends DefaultBooleanOption {
        CustomPropertyDefinition myDefinition;
        DefaultValueOption() {
            super("customPropertyDialog.defaultValue");
        }

        public void reloadValue(CustomPropertyDefinition selectedElement) {
            myDefinition = selectedElement;
            setValue(myDefinition.getDefaultValue()!=null, true);
        }
    }

    static class PropertyClassOption extends DefaultEnumerationOption<CustomPropertyClass> {
        private CardLayout myCardLayout;
        private JPanel myCardPanel;
        private Map<CustomPropertyClass, Component> myDefaultValueEditors =
            new HashMap<CustomPropertyClass, Component>();
        private CustomPropertyDefinition myDefinition;
        public PropertyClassOption() {
            super("taskProperties.customColumn.type", CustomPropertyClass.values());
        }
        @Override
        protected String objectToString(CustomPropertyClass value) {
            return value.getDisplayName();
        }

        @Override
        protected void setValue(String value, boolean resetInitial) {
            CustomPropertyClass newPropertyClass = null;
            for (CustomPropertyClass propertyClass : CustomPropertyClass.values()) {
                if (propertyClass.getDisplayName().equals(value)) {
                    newPropertyClass = propertyClass;
                    break;
                }
            }
            assert newPropertyClass!=null;

            myDefinition.setPropertyClass(newPropertyClass);
            Component defaultValueEditor = myDefaultValueEditors.get(newPropertyClass);
            if (defaultValueEditor == null) {
                GPOption defaultValueOption = CustomPropertyDefaultValueAdapter.createDefaultValueOption(myDefinition);
                OptionsPageBuilder builder = new OptionsPageBuilder();
                defaultValueEditor = builder.createStandaloneOptionPanel(defaultValueOption);
                myDefaultValueEditors.put(newPropertyClass, defaultValueEditor);
                myCardPanel.add(defaultValueEditor, newPropertyClass.getDisplayName());
            }

            myCardLayout.show(myCardPanel, value);

            super.setValue(value, resetInitial);
        }
        void setUIControls(CardLayout layout, JPanel panel) {
            myCardLayout = layout;
            myCardPanel = panel;
        }
        public void reloadValue(CustomPropertyDefinition selectedElement) {
            myDefinition = selectedElement;
            setValue(selectedElement.getPropertyClass().getDisplayName(), true);
        }
    }

}