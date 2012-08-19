/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2011 Thomas Alexandre, GanttProject Team

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
package biz.ganttproject.core.time;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import biz.ganttproject.core.time.impl.GPTimeUnitStack;

import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;

/**
 * Class use for calendar
 */
public class GanttCalendar extends java.util.GregorianCalendar {
  public GanttCalendar() {
    super();
    set(Calendar.HOUR_OF_DAY, 0);
    set(Calendar.MINUTE, 0);
    set(Calendar.SECOND, 0);
    set(Calendar.MILLISECOND, 0);
  }

  public GanttCalendar(int year, int month, int date) {
    super(year, month, date);
  }

  public GanttCalendar(GanttCalendar g) {
    super(g.getYear(), g.getMonth(), g.getDate());
  }

  public GanttCalendar(Date date) {
    super();
    setTime(date);
  }

  public static GanttCalendar parseXMLDate(String s) {
    GanttCalendar result = new GanttCalendar();
    result.clear();
    try {
      Date date = DateParser.parse(s);
      result.setTime(date);
    } catch (InvalidDateException e) {
      // Get "/" characters
      int fb = s.indexOf('/');
      int sb = s.indexOf('/', fb + 1);
      // Get all fields
      String d = s.substring(0, fb);
      String m = s.substring(fb + 1, sb);
      String y = s.substring(sb + 1);

      // Set the date
      result.set(Calendar.YEAR, Integer.parseInt(y));
      result.set(Calendar.MONTH, Integer.parseInt(m) - 1);
      result.set(Calendar.DATE, Integer.parseInt(d));
    }
    return result;
  }

  /** @return a copy of the current date */
  @Override
  public GanttCalendar clone() {
    GanttCalendar clone = new GanttCalendar(getYear(), getMonth(), getDay());
    return clone;
  }

  public String toXMLString() {
    return DateParser.getIsoDateNoHours(getTime());
  }

  public int getYear() {
    return this.get(Calendar.YEAR);
  }

  public int getMonth() {
    return this.get(Calendar.MONTH);
  }

  public int getDate() {
    return this.get(Calendar.DATE);
  }

  public int getDay() {
    return this.get(Calendar.DAY_OF_MONTH);
  }

  /**
   * @deprecated (TODO: add what to use/do instead)
   *
   *             Create of copy of the current date and add the specified
   *             (signed) amount of time
   */
  @Deprecated
  public GanttCalendar newAdd(int field, int dayNumber) {
    GanttCalendar gc = clone();
    gc.add(field, dayNumber);
    return gc;
  }

  /**
   * @deprecated Use TimeUnit related methods
   * @returns the difference (in days) between two date
   */
  @Deprecated
  public int diff(GanttCalendar d) {
    int res = 0;
    GanttCalendar d1;
    GanttCalendar d2;

    if (this.compareTo(d) == 0) {
      return res;
    }

    else if (compareTo(d) < 0) {
      d1 = this.clone();
      d2 = new GanttCalendar(d);
    } else {
      d1 = new GanttCalendar(d);
      d2 = this.clone();
    }

    while (d1.compareTo(d2) != 0) {
      d1.add(Calendar.DATE, 1);
      res++;
    }
    return res;
  }

  /** @return the sign represented by an integer */
  private int module(int number) {
    if (number > 0) {
      return 1;
    } else if (number < 0) {
      return -1;
    } else {
      return 0;
    }
  }

  final static private int[] comparissons = { Calendar.YEAR, Calendar.MONTH, Calendar.DATE };

  /**
   * This function compares the calendar date with the given date
   *
   * @return 0 If the two date are equals<br/>
   *         -1 if the date is before 'when'<br/>
   *         1 if the date is after 'when'
   */
  public int compareTo(GanttCalendar when) {
    for (int comparisson : comparissons) {
      switch (module(this.get(comparisson) - when.get(comparisson))) {
      case -1:
        return -1;
      case 1:
        return 1;
      }
    }
    return 0;
  }

  /** @return true if the calendar date equals to 'when' */
  public boolean equals(GanttCalendar when) {
    return getYear() == when.getYear() && getMonth() == when.getMonth() && getDay() == when.getDay();
  }

//  /** @return the actually date */
//  public static String getDateAndTime() {
//    GanttCalendar c = new GanttCalendar();
//    return c.toString() + " - " + GanttLanguage.getInstance().formatTime(c);
//  }

  public static Comparator<GanttCalendar> COMPARATOR = new Comparator<GanttCalendar>() {
    @Override
    public int compare(GanttCalendar o1, GanttCalendar o2) {
      return o1.compareTo(o2);
    }
  };

  public GanttCalendar getDisplayValue() {
    return new GanttCalendar(GPTimeUnitStack.DAY.jumpLeft(getTime()));
  }
}
