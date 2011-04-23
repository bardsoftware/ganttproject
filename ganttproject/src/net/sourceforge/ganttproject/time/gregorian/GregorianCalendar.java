/***************************************************************************
 GregorianCalendar.java  -  description
 -------------------
 begin                : dec 2002
 copyright            : (C) 2004 by Danilo Castilho
 email                : dncastilho@yahoo.com.br
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

/*
 * Solves the duplicated days (generally in all october's second weeks)
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
    
    public void add(int field, int value) {
        if (field == Calendar.DATE) {
            this.add(Calendar.HOUR, value * 24);
        } else {
            super.add(field, value);
        }
    }
}
