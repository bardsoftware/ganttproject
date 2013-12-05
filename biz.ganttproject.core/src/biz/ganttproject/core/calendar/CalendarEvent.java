package biz.ganttproject.core.calendar;

import java.util.Date;

import com.google.common.base.Objects;

public class CalendarEvent {
  public static enum Type {
    HOLIDAY, WORKING_DAY, NEUTRAL
  }
  public final Date myDate;
  public final boolean isRecurring;
  private final Type myType;
  private final String myTitle;
  
  public static CalendarEvent newEvent(Date date, boolean isRecurring, Type type, String title) {
    return new CalendarEvent(date, isRecurring, type, title);
  }
  
  CalendarEvent(Date date, boolean recurring, Type type, String title) {
    myDate = date;
    isRecurring = recurring;
    myType = type;
    myTitle = title;
  }

  public String getTitle() {
    return myTitle;
  }

  public Type getType() {
    return myType;
  }
  
  @Override
  public int hashCode() {
    return this.myDate.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (false == obj instanceof CalendarEvent) {
      return false;
    }
    CalendarEvent that = (CalendarEvent) obj;
    return Objects.equal(this.myDate, that.myDate) && Objects.equal(this.isRecurring, that.isRecurring);
  }


  @Override
  public String toString() {
    return "Date=" + myDate + " repeating=" + isRecurring;
  }
  
  
}