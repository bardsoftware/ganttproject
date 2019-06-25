/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

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

package net.sourceforge.ganttproject.language;

import biz.ganttproject.core.option.GPAbstractOption;
import biz.ganttproject.core.time.CalendarFactory;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.util.PropertiesUtil;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EventListener;
import java.util.EventObject;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;

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

  public static Comparator<Locale> LEXICOGRAPHICAL_LOCALE_COMPARATOR = new Comparator<Locale>() {
    @Override
    public int compare(Locale o1, Locale o2) {
      return (o1.getDisplayLanguage(Locale.US) + o1.getDisplayCountry(Locale.US)).compareTo(o2.getDisplayLanguage(Locale.US)
          + o2.getDisplayCountry(Locale.US));
    }
  };

  private static class CalendarFactoryImpl extends CalendarFactory implements CalendarFactory.LocaleApi {
    static void setLocaleImpl() {
      CalendarFactory.setLocaleApi(new CalendarFactoryImpl());
    }

    @Override
    public Locale getLocale() {
      return GanttLanguage.getInstance().getLocale();
    }

    @Override
    public DateFormat getShortDateFormat() {
      return GanttLanguage.getInstance().getShortDateFormat();
    }
  }

  private static final GanttLanguage ganttLanguage = new GanttLanguage();

  private final SimpleDateFormat myRecurringDateFormat = new SimpleDateFormat("MMM dd");

  private ArrayList<Listener> myListeners = new ArrayList<Listener>();

  private Locale currentLocale = null;

  private final CharSetMap myCharSetMap;

  private ResourceBundle i18n = null;

  private SimpleDateFormat currentDateFormat = null;

  private SimpleDateFormat shortCurrentDateFormat = null;

  private SimpleDateFormat myLongFormat;

  private DateFormat currentTimeFormat = null;

  private DateFormat currentDateTimeFormat = null;

  private List<String> myDayShortNames;

  private Locale myDateFormatLocale;

  private Properties myExtraLocales = new Properties();

  private GanttLanguage() {
    new GPAbstractOption.I18N() {
      {
        setI18N(this);
      }
      @Override
      protected String i18n(String key) {
        return getText(key);
      }
    };
    Properties charsets = new Properties();
    PropertiesUtil.loadProperties(charsets, "/charsets.properties");
    myCharSetMap = new CharSetMap(charsets);
    setLocale(Locale.getDefault());
    PropertiesUtil.loadProperties(myExtraLocales, "/language/extra.properties");
  }

  public static GanttLanguage getInstance() {
    return ganttLanguage;
  }

  public SimpleDateFormat getMediumDateFormat() {
    return currentDateFormat;
  }

  public SimpleDateFormat getShortDateFormat() {
    return shortCurrentDateFormat;
  }

  public SimpleDateFormat getRecurringDateFormat() {
    return myRecurringDateFormat;
  }
  public SimpleDateFormat getLongDateFormat() {
    return myLongFormat;
  }
  public Locale getDateFormatLocale() {
    return myDateFormatLocale;
  }

  private void applyDateFormatLocale(Locale locale) {
    myDateFormatLocale = locale;
    setShortDateFormat((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, locale));
    currentDateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
    currentTimeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale);
    currentDateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
    myLongFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.LONG, locale);
    UIManager.put("JXDatePicker.longFormat", myLongFormat.toPattern());
    UIManager.put("JXDatePicker.mediumFormat", currentDateFormat.toPattern());
    UIManager.put("JXDatePicker.numColumns", new Integer(10));
    myDayShortNames = getShortDayNames(locale);
    UIManager.put("JXMonthView.daysOfTheWeek", myDayShortNames.toArray(new String[7]));
  }

  public void setShortDateFormat(SimpleDateFormat dateFormat) {
    shortCurrentDateFormat = dateFormat;
    UIManager.put("JXDatePicker.shortFormat", shortCurrentDateFormat.toPattern());
    fireLanguageChanged();
  }

  public void setLocale(Locale locale) {
    currentLocale = locale;
    CalendarFactoryImpl.setLocaleImpl();
    Locale.setDefault(locale);
    int defaultTimezoneOffset = TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings();

    TimeZone utc = TimeZone.getTimeZone("UTC");
    utc.setRawOffset(defaultTimezoneOffset);
    TimeZone.setDefault(utc);

    applyDateFormatLocale(getDateFormatLocale(locale));
    i18n = getResourceBundle(locale);
    fireLanguageChanged();
  }

  private static ResourceBundle getResourceBundle(Locale locale) {
    IConfigurationElement[] l10nExtensions = Platform.getExtensionRegistry().getConfigurationElementsFor("net.sourceforge.ganttproject.l10n");
    List<ResourceBundle> bundles = new ArrayList<>();
    for (IConfigurationElement l10nConfig : l10nExtensions) {
      String path = l10nConfig.getAttribute("path");
      Bundle pluginBundle = Platform.getBundle(l10nConfig.getDeclaringExtension().getNamespaceIdentifier());
      assert (pluginBundle != null) : "Can't find plugin bundle for extension=" + l10nConfig.getName();
      try {
        ResourceBundle resourceBundle = ResourceBundle.getBundle(path, locale, pluginBundle.getBundleClassLoader());
        bundles.add(resourceBundle);
      } catch (MissingResourceException ex) {
        ex.printStackTrace();
        GPLogger.logToLogger(String.format("Can't find bundle: path=%s locale=%s plugin bundle=%s", path, locale, pluginBundle));
      }
    }
    assert bundles.isEmpty() == false : "Can't find any resource bundles";
    return bundles.get(0);
  }

  private Locale getDateFormatLocale(Locale baseLocale) {
    String dateFormatLocale = myExtraLocales.getProperty(baseLocale.getLanguage() + ".dateFormatLocale", null);
    if (dateFormatLocale == null) {
      return baseLocale;
    }
    return new Locale(dateFormatLocale);
  }

  public List<Locale> getAvailableLocales() {
    Set<Locale> removeLangOnly = new HashSet<Locale>();
    Set<Locale> result = new HashSet<Locale>();
    for (Locale l : Locale.getAvailableLocales()) {
      if (GanttLanguage.class.getResource("/language/i18n_" + l.getLanguage() + "_" + l.getCountry() + ".properties") != null) {
        removeLangOnly.add(new Locale(l.getLanguage()));
        result.add(new Locale(l.getLanguage(), l.getCountry()));
      } else if (GanttLanguage.class.getResource("/language/i18n_" + l.getLanguage() + ".properties") != null) {
        result.add(new Locale(l.getLanguage()));
      }
    }

    String[] locales = myExtraLocales.getProperty("_").split(",");
    for (String l : locales) {
      if (!myExtraLocales.containsKey(l + ".lang")) {
        continue;
      }
      String langCode = myExtraLocales.getProperty(l + ".lang");
      String countryCode = myExtraLocales.getProperty(l + ".country", "");
      String regionCode = myExtraLocales.getProperty(l + ".region", "");
      Locale locale = new Locale(langCode, countryCode, regionCode);
      result.add(locale);
    }

    result.removeAll(removeLangOnly);
    result.add(Locale.ENGLISH);

    List<Locale> result1 = new ArrayList<Locale>(result);
    Collections.sort(result1, LEXICOGRAPHICAL_LOCALE_COMPARATOR);
    return result1;
  }

  public String formatLanguageAndCountry(Locale locale) {
    String englishName = locale.getDisplayLanguage(Locale.US);
    String localName = locale.getDisplayLanguage(locale);
    String currentLocaleName = locale.getDisplayLanguage(getLocale());
    if ("en".equals(locale.getLanguage()) || "zh".equals(locale.getLanguage()) || "pt".equals(locale.getLanguage())) {
      if (!locale.getCountry().isEmpty()) {
        englishName += " - " + locale.getDisplayCountry(Locale.US);
        localName += " - " + locale.getDisplayCountry(locale);
      }
    }
    if (localName.equals(englishName) && currentLocaleName.equals(englishName)) {
      return englishName;
    }
    StringBuilder builder = new StringBuilder(englishName);
    builder.append(" (");
    boolean hasLocal = false;
    if (!localName.equals(englishName)) {
      builder.append(localName);
      hasLocal = true;
    }
    if (!currentLocaleName.equals(localName) && !currentLocaleName.equals(englishName)) {
      if (hasLocal) {
        builder.append(", ");
      }
      builder.append(currentLocaleName);
    }
    builder.append(")");
    return builder.toString();
  }

  /** @return The current Locale */
  public Locale getLocale() {
    return currentLocale;
  }

  public String getCharSet() {
    return myCharSetMap.getCharSet(getLocale());
  }

  public String getDay(int day) {
    return myDayShortNames.get(day);
  }

  /** @return The current DateFormat */
  public DateFormat getDateFormat() {
    return currentDateFormat;
  }

  public String formatDate(Calendar date) {
    return currentDateFormat.format(date.getTime());
  }

  public String formatShortDate(Calendar date) {
    return shortCurrentDateFormat.format(date.getTime());
  }

  public String formatTime(Calendar date) {
    return currentTimeFormat.format(date.getTime());
  }

  public String formatDateTime(Date date) {
    return currentDateTimeFormat.format(date);
  }



  public Date parseDate(String dateString) {
    if (dateString == null) {
      return null;
    }
    try {
      Date parsed = getShortDateFormat().parse(dateString);
      if (getShortDateFormat().format(parsed).equals(dateString)) {
        return parsed;
      }
    } catch (ParseException e) {
      GPLogger.logToLogger(e);
    }
    return null;
  }

  public String getMonth(int m) {
    GregorianCalendar month = new GregorianCalendar(2000, m, 1);
    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM", myDateFormatLocale);
    StringBuffer result = new StringBuffer();
    result = dateFormat.format(month.getTime(), result, new FieldPosition(DateFormat.MONTH_FIELD));
    return result.toString();
  }

  private static List<String> getShortDayNames(Locale locale) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE", locale);
    List<String> result = new ArrayList<String>();
    for (int i = 0; i < 7; i++) {
      GregorianCalendar day = new GregorianCalendar(2000, 1, 1);
      while (day.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
        day.add(Calendar.DATE, 1);
      }
      day.add(Calendar.DATE, i);

      StringBuffer formattedDay = new StringBuffer();
      formattedDay = dateFormat.format(day.getTime(), formattedDay, new FieldPosition(DateFormat.DAY_OF_WEEK_FIELD));
      result.add(formattedDay.toString());
    }
    return result;
  }

  /** @return the text in the current language for the given key */
  public String getText(String key) {
    try {
      return i18n.getString(key);
    } catch (MissingResourceException e) {
      return null;
    }
  }

  public String getText(String key, Locale locale) {
    try {
      return getResourceBundle(locale).getString(key);
    } catch (MissingResourceException e) {
      return getText(key);
    }
  }

  /**
   * @return the text suitable for labels in the current language for the given
   *         key (all $ characters are removed from the original text)
   * @see #GanttLagetText()
   * @see #correctLabel()
   */
  public String getCorrectedLabel(String key) {
    String label = getText(key);
    return label == null ? null : correctLabel(label);
  }

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
    return new SimpleDateFormat(string, myDateFormatLocale);
  }

  /** @return label with the $ removed from it (if it was included) */
  public String correctLabel(String label) {
    if (label == null) {
      return null;
    }

    int index = label.indexOf('$');
    if (index != -1 && label.length() - index > 1) {
      label = label.substring(0, index).concat(label.substring(++index));
    }
    return label;
  }

  public String formatText(String key, Object... values) {
    String message = getText(key);
    return message == null ? key : MessageFormat.format(message, values);
  }
}
