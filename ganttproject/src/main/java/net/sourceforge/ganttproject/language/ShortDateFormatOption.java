/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev, GanttProject team

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

import biz.ganttproject.core.option.DefaultStringOption;
import biz.ganttproject.core.option.ValidationException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ShortDateFormatOption extends DefaultStringOption {
  private SimpleDateFormat myDateFormat;

  public ShortDateFormatOption() {
    super("ui.dateFormat.short");

    // Set initial locale
    setSelectedLocale(null);
  }

  @Override
  public void setValue(String value) {
    try {
      myDateFormat = new SimpleDateFormat(value);
      super.setValue(value);
    } catch (IllegalArgumentException e) {
      throw new ValidationException();
    }
  }

  @Override
  public void setValue(String value, Object clientId) {
    try {
      myDateFormat = new SimpleDateFormat(value);
      super.setValue(value, clientId);
    } catch (IllegalArgumentException e) {
      throw new ValidationException();
    }
  }

  @Override
  public void loadPersistentValue(String value) {
    super.loadPersistentValue(value);
    GanttLanguage.getInstance().setShortDateFormat(myDateFormat);
  }

  @Override
  public void commit() {
    super.commit();
    GanttLanguage.getInstance().setShortDateFormat(myDateFormat);
  }

  public void setSelectedLocale(Locale locale) {
    if (locale == null) {
      // Find default locale
      locale = GanttLanguage.getInstance().getLocale();
    }
    myDateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, locale);
    super.resetValue(myDateFormat.toPattern(), true);
  }

  public DateFormat getSelectedValue() {
    return myDateFormat;
  }

  public String formatDate(Date date) {
    return myDateFormat.format(date);
  }
}
