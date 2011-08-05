/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2011 Thomas Alexandre, GanttProject Team

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
package net.sourceforge.ganttproject;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.time.gregorian.GregorianCalendar;

import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;

/**
 * Class use for calendar
 */
public class GanttCalendar extends GregorianCalendar {
    private final GanttLanguage language = GanttLanguage.getInstance();

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
            result.set(Calendar.YEAR, new Integer(y).hashCode());
            result.set(Calendar.MONTH, new Integer(m).hashCode() - 1);
            result.set(Calendar.DATE, new Integer(d).hashCode());
        }
        return result;
    }

    /** @return a copy of the current date */
    public GanttCalendar clone() {
        GanttCalendar clone = new GanttCalendar(getYear(), getMonth(), getDay());
        return clone;
    }

    /** @return the date to as a string */
    public String toString() {
        return language.formatShortDate(this);
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

    public int getDayWeek() {
        return this.get(Calendar.DAY_OF_WEEK);
    }

    public int getWeek() {
        return this.get(Calendar.WEEK_OF_YEAR);
    }

    /** Create of copy of the current date and add the specified (signed) amount of time */
    @Deprecated
    public GanttCalendar newAdd(int field, int dayNumber) {
        GanttCalendar gc = clone();
        gc.add(field, dayNumber);
        return gc;
    }

    /**
     * @deprecated Use TimeUnit related methods
     * @returns the difference (in days) between two date */
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
            default:
                break;
            }
        }
        return 0;
    }

    /** @return true if the calendar date equals to 'when' */
    public boolean equals(GanttCalendar when) {
        return getYear() == when.getYear() && getMonth() == when.getMonth()
                && getDay() == when.getDay();
    }

    /** @return the actually date */
    public static String getDateAndTime() {
        GanttCalendar c = new GanttCalendar();
        return c.toString() + " - " + GanttLanguage.getInstance().formatTime(c);
    }

    public static Comparator<GanttCalendar> COMPARATOR = new Comparator<GanttCalendar>() {
        public int compare(GanttCalendar o1, GanttCalendar o2) {
            return o1.compareTo(o2);
        }
    };
}
