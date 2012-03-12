/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * This class groups static methods together to handle dates.
 * 
 * @author bbaranne (Benoit Baranne)
 */
public class DateUtils {
  /**
   * This method tries to parse the given date according to the given locale.
   * Actually, this method tries to parse the given string with several
   * DateFormat : Short, Medium, Long and Full. Normally if you give an
   * appropriate locale in relation with the string, this method will return the
   * correct date.
   * 
   * @param date
   *          String representation of a date.
   * @param locale
   *          Locale use to parse the date with.
   * @return A date object.
   * @throws ParseException
   *           Exception thrown if parsing is fruitless.
   */
  public static Date parseDate(String date) throws ParseException {
    DateFormat[] formats = new DateFormat[] { GanttLanguage.getInstance().getShortDateFormat(),
        GanttLanguage.getInstance().getMediumDateFormat(), GanttLanguage.getInstance().getLongDateFormat() };
    // DateFormat dfFull = DateFormat.getDateInstance(DateFormat.FULL, locale);
    for (int i = 0; i < formats.length; i++) {
      try {
        return formats[i].parse(date);
      } catch (ParseException e) {
        if (i + 1 == formats.length) {
          throw e;
        }
      } catch (IllegalArgumentException e) {
        if (i + 1 == formats.length) {
          throw e;
        }

      }
    }
    throw new ParseException("Failed to parse date=" + date, 0);
  }
}
