/***************************************************************************
 GanttLanguage.java  -  description
 -------------------
 begin                : jan 2003
 copyright            : (C) 2003 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package net.sourceforge.ganttproject.language;

import java.awt.ComponentOrientation;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.swing.UIManager;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.time.gregorian.GregorianCalendar;

/**
 * Class for the language
 */
public class GanttLanguage {
    public class Event extends EventObject {
        public Event(GanttLanguage language) {
            super(language);
        }

        public GanttLanguage getLanguage() {
            return (GanttLanguage) getSource();
        }

    }

    public interface Listener extends EventListener {
        public void languageChanged(Event event);
    }

    private static GanttLanguage ganttLanguage = null;

    private ArrayList<Listener> myListeners = new ArrayList<Listener>();

    public static GanttLanguage getInstance() {
        if (ganttLanguage == null) {
            ganttLanguage = new GanttLanguage();
        }
        return ganttLanguage;
    }

    Locale currentLocale = null;
    CharSetMap myCharSetMap;

    ResourceBundle i18n = null;

    SimpleDateFormat currentDateFormat = null;
    public DateFormat getMediumDateFormat() {
    	return currentDateFormat;
    }

    SimpleDateFormat shortCurrentDateFormat = null;
    public DateFormat getShortDateFormat() {
    	return shortCurrentDateFormat;
    }

    SimpleDateFormat myLongFormat;
    public DateFormat getLongDateFormat() {
    	return myLongFormat;
    }

    DateFormat currentTimeFormat = null;
    public static final String MISSING_RESOURCE = "Missing Resource";

    private GanttLanguage() {
        myCharSetMap = new CharSetMap();
        setLocale(Locale.getDefault());
    }

    public void setLocale(Locale locale) {
        currentLocale = locale;
        Locale.setDefault(locale);
        int defaultTimezoneOffset = TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings();

        TimeZone utc = TimeZone.getTimeZone("UTC");
        utc.setRawOffset(defaultTimezoneOffset);
        TimeZone.setDefault(utc);

        currentDateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM,
                currentLocale);
        shortCurrentDateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT,
                currentLocale);
        currentTimeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM,
                currentLocale);
    	myLongFormat = (SimpleDateFormat)DateFormat.getDateInstance(DateFormat.LONG, locale);
        UIManager.put("JXDatePicker.longFormat", myLongFormat.toPattern());
        UIManager.put("JXDatePicker.mediumFormat", currentDateFormat.toPattern());
        UIManager.put("JXDatePicker.shortFormat", shortCurrentDateFormat.toPattern());
        UIManager.put("JXDatePicker.numColumns", new Integer(10));
        String[] dayShortNames = new String[7];
        for (int i=0; i<7; i++) {
        	dayShortNames[i] = getDay(i).substring(0, 1);
        }
        UIManager.put("JXMonthView.daysOfTheWeek", dayShortNames);
        String resourceBase = System.getProperty(
                "org.ganttproject.resourcebase", "language/i18n");
        i18n = ResourceBundle.getBundle(resourceBase, currentLocale);

        fireLanguageChanged();
    }

    /**
     * Return the current Locale.
     *
     * @return The current Locale.
     */
    public Locale getLocale() {
        return currentLocale;
    }

    public String getCharSet() {
        return myCharSetMap.getCharSet(getLocale());
    }
    /**
     * Return the current DateFormat.
     *
     * @return The current DateFormat.
     */
    public DateFormat getDateFormat() {
        return currentDateFormat;
    }

    public String formatDate(GanttCalendar date) {
        return currentDateFormat.format(date.getTime());
    }

    public String formatShortDate(GanttCalendar date)
    {
    	return shortCurrentDateFormat.format(date.getTime());
    }

    public String formatTime(GanttCalendar date) {
        return currentTimeFormat.format(date.getTime());
    }

    public GanttCalendar parseDate(String date) throws ParseException {
        Calendar tmp = Calendar.getInstance(currentLocale);
        tmp.setTime(currentDateFormat.parse(date));
        return new GanttCalendar(tmp.get(Calendar.YEAR), tmp
                .get(Calendar.MONTH), tmp.get(Calendar.DATE));
    }

    public String getMonth(int m) {
        GregorianCalendar month = new GregorianCalendar(2000, m, 1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM",
                this.currentLocale);
        StringBuffer result = new StringBuffer();
        result = dateFormat.format(month.getTime(), result, new FieldPosition(
                DateFormat.MONTH_FIELD));
        return result.toString();
    }

    public String getDay(int d) {
        GregorianCalendar day = new GregorianCalendar(2000, 1, 1);
        while (day.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            day.add(Calendar.DATE, 1);
        }
        day.add(Calendar.DATE, d);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE",
                this.currentLocale);
        StringBuffer result = new StringBuffer();
        result = dateFormat.format(day.getTime(), result, new FieldPosition(
                DateFormat.DAY_OF_WEEK_FIELD));
        return result.toString();
    }

    public String getText(String key) {
        try {
            return i18n.getString(key);
        } catch (MissingResourceException e) {
            //return MISSING_RESOURCE + " '" + key + "'";
            return null;
        }
    };

    public ComponentOrientation getComponentOrientation() {
        return ComponentOrientation.getOrientation(currentLocale);
    }

    public void addListener(Listener listener) {
        myListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        myListeners.remove(listener);
    }

    private void fireLanguageChanged() {
        Event event = new Event(this);
        for (int i = 0; i < myListeners.size(); i++) {
            Listener next = myListeners.get(i);
            next.languageChanged(event);
        }
    }

    public SimpleDateFormat createDateFormat(String string) {
        return new SimpleDateFormat(string, currentLocale);
    }

    public String correctLabel(String label) {
        int index = label.indexOf('$');
        if (index != -1 && label.length() - index > 1) {
            label = label.substring(0, index).concat(label.substring(++index));
        }
        return label;
    }

    public Calendar newCalendar() {
    	return (Calendar) Calendar.getInstance(currentLocale).clone();
    }
}
