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
 * @author dncastilho
 */
public class GregorianCalendar extends java.util.GregorianCalendar {

    /**
     * Overrides the original, to solve the october duplicated day bug
     */
    public void add(int field, int value) {
        if (field == Calendar.DATE)
            this.add(Calendar.HOUR, value * 24);
        else
            super.add(field, value);
    }

    /**
     * From super
     */
    public GregorianCalendar() {
        super();
    }

    /**
     * From super
     * 
     * @param year
     * @param month
     * @param date
     */
    public GregorianCalendar(int year, int month, int date) {
        super(year, month, date);
    }

    /**
     * From super
     * 
     * @param year
     * @param month
     * @param date
     * @param hour
     * @param minute
     */
    public GregorianCalendar(int year, int month, int date, int hour, int minute) {
        super(year, month, date, hour, minute);
    }

    /**
     * From super
     * 
     * @param year
     * @param month
     * @param date
     * @param hour
     * @param minute
     * @param second
     */
    public GregorianCalendar(int year, int month, int date, int hour,
            int minute, int second) {
        super(year, month, date, hour, minute, second);
    }

    /**
     * From super
     * 
     * @param aLocale
     */
    public GregorianCalendar(Locale aLocale) {
        super(aLocale);
    }

    /**
     * From super
     * 
     * @param zone
     */
    public GregorianCalendar(TimeZone zone) {
        super(zone);
    }

    /**
     * From super
     * 
     * @param zone
     * @param aLocale
     */
    public GregorianCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
    }
}
