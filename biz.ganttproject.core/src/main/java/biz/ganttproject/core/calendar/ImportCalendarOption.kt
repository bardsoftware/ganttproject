package biz.ganttproject.core.calendar;

import biz.ganttproject.core.option.DefaultEnumerationOption;

public class ImportCalendarOption extends DefaultEnumerationOption<ImportCalendarOption.Values> {
  public static enum Values {
    NO, REPLACE, MERGE;

    @Override
    public String toString() {
      return "importCalendar_" + name().toLowerCase();
    }
  }
  
  public ImportCalendarOption() {
    super("impex.importCalendar", Values.values());
  }
  
  public ImportCalendarOption(Values initialValue) {
    super("impex.importCalendar", Values.values());
    setSelectedValue(initialValue);
  }
}