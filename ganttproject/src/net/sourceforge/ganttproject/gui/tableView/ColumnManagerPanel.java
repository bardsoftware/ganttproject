/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.gui.tableView;

import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.DefaultStringOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.ValidationException;
import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.table.ColumnList.Column;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import net.sourceforge.ganttproject.CustomPropertyClass;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.DefaultCustomPropertyDefinition;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent.SelectionListener;
import net.sourceforge.ganttproject.gui.EditableList;
import net.sourceforge.ganttproject.gui.ListAndFieldsPanel;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.CustomPropertyDefaultValueAdapter;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * UI component inside  "Custom Fields Manager" dialog which allows for showing/hiding
 * table columns and for the management of custom fields.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class ColumnManagerPanel {
  private IsVisibleOption myIsVisibleOption = new IsVisibleOption();
  private NameOption myNameOption = new NameOption();
  private DefaultValueOption myEnableDefaultValueOption = new DefaultValueOption();
  private PropertyClassOption myType = new PropertyClassOption();
  private GPOption<?> myDefaultValueOption;

  private JPanel panelDefaultValue = null;

  private CardLayout cardLayoutDefaultValue = null;
  private final CustomPropertyManager myManager;
  private final ColumnList myVisibleFields;
  private JComponent myFields;

  public ColumnManagerPanel(CustomPropertyManager columnManager, ColumnList visibleFields) {
    myManager = columnManager;
    myVisibleFields = visibleFields;
  }

  public void commitCustomPropertyEdit() {
    myIsVisibleOption.commit();
    myNameOption.commit();
    myEnableDefaultValueOption.commit();
    myType.commit();
  }

  public Component createComponent() {
    List<CustomPropertyDefinition> emptyList = Collections.emptyList();
    List<CustomPropertyDefinition> defs = new ArrayList<CustomPropertyDefinition>();
    createDefaultFieldDefinitions(myVisibleFields, myManager.getDefinitions(), defs);
    final Set<CustomPropertyDefinition> standardDefs = new HashSet<CustomPropertyDefinition>(defs);
    defs.addAll(myManager.getDefinitions());
    final EditableList<CustomPropertyDefinition> props = new EditableList<CustomPropertyDefinition>(defs, emptyList) {
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
        if ("".equals(String.valueOf(editValue).trim())) {
          throw new ValidationException("Please type column name");
        }
        return new DefaultCustomPropertyDefinition(String.valueOf(editValue));
      }

      @Override
      protected CustomPropertyDefinition createValue(CustomPropertyDefinition prototype) {
        final CustomPropertyDefinition def = myManager.createDefinition(CustomPropertyClass.TEXT.getID(),
            prototype.getName(), null);
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            myVisibleFields.add(def.getID(), -1, -1);
            getTableComponent().requestFocus();
            setSelectedDefinition(def);
          }
        });
        return def;
      }

      @Override
      protected void deleteValue(CustomPropertyDefinition value) {
        myManager.deleteDefinition(value);
      }

      @Override
      protected CustomPropertyDefinition updateValue(CustomPropertyDefinition newValue,
          CustomPropertyDefinition curValue) {
        curValue.setName(newValue.getName());
        return curValue;
      }

      @Override
      protected Component getTableCellRendererComponent(DefaultTableCellRenderer defaultRenderer,
          CustomPropertyDefinition def, boolean isSelected, boolean hasFocus, int row) {
        StringBuffer value = new StringBuffer("<html>");
        Column column = myIsVisibleOption.findColumn(def);
        if (column != null && !column.isVisible()) {
          value.append("<font color=#999999>{0}</font>");
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

      @Override
      public void selectionChanged(List<CustomPropertyDefinition> selection) {
        mySelection = selection;
        if (!isShow) {
          for (int i = 0; i < myVisibleFields.getSize(); i++) {
            final Column it = myVisibleFields.getField(i);
            if (it.isVisible()) {
              boolean isSelected = !Collections2.filter(selection, new Predicate<CustomPropertyDefinition>() {
                @Override
                public boolean apply(@Nullable CustomPropertyDefinition def) {
                  return Objects.equal(def.getName(), it.getName());
                }
              }).isEmpty();
              if (!isSelected) {
                setEnabled(true);
                return;
              }
            }
          }
          setEnabled(false);
        } else {
          setEnabled(true);
        }
      }

      @Override
      public void actionPerformed(ActionEvent e) {
        for (CustomPropertyDefinition def : mySelection) {
          myIsVisibleOption.setVisible(def, isShow);
        }
      }
    }
    props.getTableAndActions().addAction(new ShowHideSelectionAction(true, "customPropertyDialog.showSelected"));
    props.getTableAndActions().addAction(new ShowHideSelectionAction(false, "customPropertyDialog.hideSelected"));
    myIsVisibleOption.setVisibleFields(myVisibleFields);
    ChangeValueListener defaultValuePanelEnabler = new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        setDefaultValuePanelEnabled(myEnableDefaultValueOption.isChecked());
      }
    };
    myEnableDefaultValueOption.addChangeValueListener(defaultValuePanelEnabler);
    myType.addChangeValueListener(defaultValuePanelEnabler);
    myFields = getFieldsComponent();
    ListAndFieldsPanel<CustomPropertyDefinition> listAndFields = new ListAndFieldsPanel<CustomPropertyDefinition>(
        props, myFields);

    Function<List<CustomPropertyDefinition>, Boolean> isDeleteEnabled = new Function<List<CustomPropertyDefinition>, Boolean>() {

      @Override
      public Boolean apply(List<CustomPropertyDefinition> input) {
        for (CustomPropertyDefinition def : input) {
          if (standardDefs.contains(def)) {
            return false;
          }
        }
        return !input.isEmpty();
      }
    };
    props.getTableAndActions().getDeleteItemAction().putValue(AbstractTableAndActionsComponent.PROPERTY_IS_ENABLED_FUNCTION, isDeleteEnabled);
    props.getTableAndActions().addSelectionListener(new SelectionListener<CustomPropertyDefinition>() {
      @Override
      public void selectionChanged(List<CustomPropertyDefinition> selection) {
        if (selection.size() != 1) {
          UIUtil.setEnabledTree(myFields, false);
        } else {
          commitCustomPropertyEdit();
          CustomPropertyDefinition selectedElement = selection.get(0);
          setSelectedDefinition(selectedElement);
        }
      }
    });
    return listAndFields.getComponent();
  }

  private void setSelectedDefinition(CustomPropertyDefinition selected) {
    UIUtil.setEnabledTree(myFields, isEditable(selected));
    myNameOption.reloadValue(selected);
    myType.reloadValue(selected);
    myEnableDefaultValueOption.reloadValue(selected);
  }

  /**
   * This method returns default columns (such as name, Id, etc.) in {@code output} list.
   * It does it by scanning the tableHeader where all columns live and filtering out
   * the real custom columns.
   */
  private void createDefaultFieldDefinitions(ColumnList tableHeader,
      List<CustomPropertyDefinition> customFields, List<CustomPropertyDefinition> output) {
    LinkedHashMap<String, Column> name2column = new LinkedHashMap<String, Column>();
    for (int i = 0; i < tableHeader.getSize(); i++) {
      Column column = tableHeader.getField(i);
      name2column.put(column.getName(), column);
    }
    for (CustomPropertyDefinition def : customFields) {
      name2column.remove(def.getName());
    }
    for (Column column : name2column.values()) {
      output.add(new DefaultCustomPropertyDefinition(column.getName()));
    }
  }

  protected boolean isEditable(CustomPropertyDefinition def) {
    return myManager.getCustomPropertyDefinition(def.getID()) != null;
  }

  protected void setDefaultValuePanelEnabled(boolean enabled) {
    UIUtil.setEnabledTree(panelDefaultValue, enabled);
  }

  private JComponent getFieldsComponent() {
    OptionsPageBuilder builder = new OptionsPageBuilder();
    {
      cardLayoutDefaultValue = new CardLayout();
      panelDefaultValue = new JPanel(cardLayoutDefaultValue);
      myType.setUIControls(cardLayoutDefaultValue, panelDefaultValue);
    }

    Component optionsComponent = builder.createGroupComponent(null, myNameOption, myType);

    Box result = Box.createVerticalBox();
    result.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    // result.setLayout(new BorderLayout());
    result.add(optionsComponent);
    result.add(Box.createVerticalStrut(10));
    GPOptionGroup defaultValueGroup = new GPOptionGroup("customPropertyDialog.defaultValue",
        new GPOption[] { myEnableDefaultValueOption });
    defaultValueGroup.setTitled(false);
    result.add(builder.createGroupComponent(defaultValueGroup, myEnableDefaultValueOption));
    result.add(panelDefaultValue);
    setDefaultValuePanelEnabled(false);
    return result;
  }

  class IsVisibleOption extends DefaultBooleanOption {
    private ColumnList myVisibleFields;

    IsVisibleOption() {
      super("customPropertyDialog.isVisible");
    }

    public void setVisibleFields(ColumnList visibleFields) {
      myVisibleFields = visibleFields;
    }

    @Override
    public void commit() {
      if (isChanged()) {
        super.commit();
      }
    }

    void setVisible(CustomPropertyDefinition def, boolean isVisible) {
      Column column = findColumn(def);
      if (column != null) {
        column.setVisible(isVisible);
      }
    }

    Column findColumn(CustomPropertyDefinition def) {
      for (int i = 0; i < myVisibleFields.getSize(); i++) {
        Column nextColumn = myVisibleFields.getField(i);
        if (nextColumn.getName().equals(def.getName())) {
          return nextColumn;
        }
      }
      return null;
    }
  }

  /**
   * Option class controlling changes of a custom field name
   */
  static class NameOption extends DefaultStringOption {
    CustomPropertyDefinition myDefinition;

    NameOption() {
      super("customPropertyDialog.name");
    }

    public void reloadValue(CustomPropertyDefinition selectedElement) {
      myDefinition = selectedElement;
      resetValue(myDefinition.getName(), true);
    }

    @Override
    public void commit() {
      if (isChanged()) {
        super.commit();
        myDefinition.setName(getValue());
      }
    }
  }

  /**
   * Option class controlling changes of a custom field default value
   */
  class DefaultValueOption extends DefaultBooleanOption {
    CustomPropertyDefinition myDefinition;

    DefaultValueOption() {
      super("customPropertyDialog.defaultValue");
    }

    @Override
    public void setValue(Boolean value) {
      super.setValue(value);
      UIUtil.setEnabledTree(panelDefaultValue, value);
    }

    @Override
    public void commit() {
      if (isChanged()) {
        super.commit();
        if (!getValue()) {
          myDefinition.setDefaultValueAsString(null);
        }
      }
    }

    public void reloadValue(CustomPropertyDefinition selectedElement) {
      myDefinition = selectedElement;
      resetValue(myDefinition.getDefaultValue() != null, true);
    }
  }

  /**
   * Option class controlling changes of a custom field type
   */
  class PropertyClassOption extends DefaultEnumerationOption<CustomPropertyClass> {
    private CardLayout myCardLayout;
    private JPanel myCardPanel;
    // private Map<CustomPropertyClass, Component> myDefaultValueEditors =
    // new HashMap<CustomPropertyClass, Component>();
    private CustomPropertyDefinition myDefinition;
    private CustomPropertyDefinition myDefinitionRO;

    public PropertyClassOption() {
      super("taskProperties.customColumn.type", CustomPropertyClass.values());
    }

    @Override
    protected String objectToString(CustomPropertyClass value) {
      return value.getDisplayName();
    }

    @Override
    protected void resetValue(String value, boolean resetInitial, Object triggerId) {
      CustomPropertyClass propertyClass = getCustomPropertyClass(value);
      myDefinition.setPropertyClass(propertyClass);
      // Component defaultValueEditor =
      // myDefaultValueEditors.get(propertyClass);
      // System.err.println("property class option setValue: class=" +
      // propertyClass + " defaultValueEditor=" + defaultValueEditor);
      // if (defaultValueEditor == null) {
      myDefaultValueOption = CustomPropertyDefaultValueAdapter.createDefaultValueOption(propertyClass, myDefinition);
      OptionsPageBuilder builder = new OptionsPageBuilder();
      Component defaultValueEditor = builder.createOptionComponent(null, myDefaultValueOption);
      JPanel defaultValuePanel = new JPanel(new BorderLayout());
      defaultValuePanel.add(defaultValueEditor, BorderLayout.NORTH);
      // myDefaultValueEditors.put(propertyClass, defaultValuePanel);
      myCardPanel.add(defaultValuePanel, propertyClass.getDisplayName());
      // }

      myCardLayout.show(myCardPanel, value);

      super.resetValue(value, resetInitial, triggerId);
    }

    @Override
    public void commit() {
      if (isChanged()) {
        super.commit();
        myDefinitionRO.setPropertyClass(getCustomPropertyClass(getValue()));
      }
      if (myEnableDefaultValueOption.getValue() && myDefaultValueOption != null && myDefaultValueOption.isChanged()) {
        myDefaultValueOption.commit();
        myDefinitionRO.setDefaultValueAsString(myDefinition.getDefaultValueAsString());
      }
    }

    private CustomPropertyClass getCustomPropertyClass(String value) {
      CustomPropertyClass newPropertyClass = null;
      for (CustomPropertyClass propertyClass : CustomPropertyClass.values()) {
        if (propertyClass.getDisplayName().equals(value)) {
          newPropertyClass = propertyClass;
          break;
        }
      }
      assert newPropertyClass != null;
      return newPropertyClass;
    }

    void setUIControls(CardLayout layout, JPanel panel) {
      myCardLayout = layout;
      myCardPanel = panel;
    }

    public void reloadValue(CustomPropertyDefinition def) {
      myDefinitionRO = def;
      myDefinition = new DefaultCustomPropertyDefinition(def.getName(), def.getID(), def);
      resetValue(def.getPropertyClass().getDisplayName(), true);
    }
  }

}
