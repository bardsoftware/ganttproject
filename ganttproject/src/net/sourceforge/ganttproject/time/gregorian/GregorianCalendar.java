/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2011 Danilo Castilho, GanttProject Team

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
package net.sourceforge.ganttproject.time.gregorian;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Overrides the original java.util.GregorianCalendar class, to solve the
 * October duplicated day bug
 */
public class GregorianCalendar extends java.util.GregorianCalendar {
    public GregorianCalendar() {
        super();
    }

    public GregorianCalendar(int year, int month, int date) {
        super(year, month, date);
    }

    public GregorianCalendar(int year, int month, int date, int hour, int minute) {
        super(year, month, date, hour, minute);
    }

    public GregorianCalendar(int year, int month, int date, int hour,
            int minute, int second) {
        super(year, month, date, hour, minute, second);
    }

    public GregorianCalendar(Locale aLocale) {
        super(aLocale);
    }


    public GregorianCalendar(TimeZone zone) {
        super(zone);
    }

    public GregorianCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
    }

    @Override
    public void add(int field, int value) {
        if (field == Calendar.DATE) {
            this.add(Calendar.HOUR, value * 24);
        } else {
            super.add(field, value);
        }
    }
}
