/*
 * Created on 02.04.2005
 */
package net.sourceforge.ganttproject.gui.options;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.GPOptionGroup;

import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author bard
 */
class EnumerationOptionComboBoxModel extends AbstractListModel implements ComboBoxModel {
  private final List<Item> myValues;

  private Item mySelectedItem;

  private final EnumerationOption myOption;

  public EnumerationOptionComboBoxModel(EnumerationOption option, GPOptionGroup group) {
    this(option, geti18nedValues(option, group));
  }

  public EnumerationOptionComboBoxModel(EnumerationOption option, String[] i18nedValues) {
    myOption = option;
    String currentValue = option.getValue();
    Item currentItem = null;
    String[] ids = option.getAvailableValues();

    myValues = new ArrayList<Item>(ids.length);
    for (int i = 0; i < ids.length; i++) {
      Item nextItem = new Item(ids[i], i18nedValues[i]);
      myValues.add(nextItem);
      if (ids[i].equals(currentValue)) {
        currentItem = nextItem;
      }
    }
    if (currentItem != null) {
      setSelectedItem(currentItem);
    }
  }

  @Override
  public void setSelectedItem(Object item) {
    if (item instanceof Item) {
      mySelectedItem = (Item) item;
      myOption.setValue(mySelectedItem.myID);
    }
  }

  @Override
  public Object getSelectedItem() {
    return mySelectedItem;
  }

  @Override
  public int getSize() {
    return myValues.size();
  }

  @Override
  public Object getElementAt(int index) {
    return myValues.get(index);
  }

  static String[] geti18nedValues(EnumerationOption option, GPOptionGroup group) {
    String[] ids = option.getAvailableValues();
    String[] result = new String[ids.length];
    for (int i = 0; i < ids.length; i++) {
      String key = OptionsPageBuilder.I18N.getCanonicalOptionValueLabelKey(ids[i]);
      String value = GanttLanguage.getInstance().getText(key);

      if (value == null && group != null) {
        key = group.getI18Nkey(key);
        if (key != null) {
          value = GanttLanguage.getInstance().getText(key);
        }
      }
      result[i] = value == null ? ids[i] : value;
    }
    return result;
  }

  public void onValueChange() {
    if (myOption.getValue() != null) {
      Item selectedItem = new Item(myOption.getValue(), myOption.getValue());
      int index = myValues.indexOf(selectedItem);
      mySelectedItem = myValues.get(index);
      fireContentsChanged(this, 0, myValues.size() - 1);
    }
  }

  private static class Item {
    private final String myID;

    private final String myDisplayValue;

    public Item(String id, String displayValue) {
      assert id != null : "ID is null for display value=" + displayValue;
      myID = id;
      myDisplayValue = displayValue;
    }

    @Override
    public String toString() {
      return myDisplayValue;
    }

    @Override
    public boolean equals(Object obj) {
      if (false == obj instanceof Item) {
        return false;
      }
      Item rvalue = (Item) obj;
      return this.myID.equals(rvalue.myID);
    }

    @Override
    public int hashCode() {
      return myID.hashCode();
    }
  }
}
