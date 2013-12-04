package biz.ganttproject.core.calendar;

import java.util.Date;

import com.google.common.base.Objects;

public class CalendarEvent {
  public final Date date;
  public final boolean isRepeating;
  
  CalendarEvent(Date date, boolean isRepeating) {
    this.date = date;
    this.isRepeating = isRepeating;
  }

  
  @Override
  public int hashCode() {
    return this.date.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (false == obj instanceof CalendarEvent) {
      return false;
    }
    CalendarEvent that = (CalendarEvent) obj;
    return Objects.equal(this.date, that.date) && Objects.equal(this.isRepeating, that.isRepeating);
  }


  @Override
  public String toString() {
    return "Date=" + date + " repeating=" + isRepeating;
  }
  
  
}