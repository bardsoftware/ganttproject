/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010 Dmitry Barashev

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
package biz.ganttproject.impex.msproject2;

import java.util.Arrays;
import java.util.Locale;

import biz.ganttproject.core.option.DefaultEnumerationOption;

import net.sf.mpxj.mpx.MPXWriter;
import net.sourceforge.ganttproject.language.GanttLanguage;

class LocaleOption extends DefaultEnumerationOption<Object> {
  private static final Locale[] LOCALES;
  private static final String[] LOCALE_DISPLAY_NAMES;

  static {
    MPXWriter writer = new MPXWriter();
    LOCALES = writer.getSupportedLocales();
    LOCALE_DISPLAY_NAMES = new String[LOCALES.length];
    for (int i = 0; i < LOCALES.length; i++) {
      LOCALE_DISPLAY_NAMES[i] = LOCALES[i].getDisplayLanguage(GanttLanguage.getInstance().getLocale());
    }
    Arrays.sort(LOCALE_DISPLAY_NAMES);
  }

  LocaleOption() {
    super("impex.msproject.mpx.language", LOCALE_DISPLAY_NAMES);
  }

  Locale getSelectedLocale() {
    for (Locale l : LOCALES) {
      if (l.getDisplayLanguage(GanttLanguage.getInstance().getLocale()).equals(getValue())) {
        return l;
      }
    }
    return null;
  }

  void setSelectedLocale(Locale locale) {
    setValue(locale.getDisplayLanguage(GanttLanguage.getInstance().getLocale()));
  }
}
