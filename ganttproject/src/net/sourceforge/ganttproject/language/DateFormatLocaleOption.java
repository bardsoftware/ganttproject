/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev, GanttProject team

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
package net.sourceforge.ganttproject.language;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;

public class DateFormatLocaleOption extends DefaultEnumerationOption<LanguageAndDateFormat> {
    private static final Map<String, LanguageAndDateFormat> ourValues = createLanguageAndFormatValues();

    private final Map<String, LanguageAndDateFormat> myDisplayStringToLdf = new HashMap<String, LanguageAndDateFormat>();
    public DateFormatLocaleOption() {
        super("ui.dateFormat.choice", ourValues.values().toArray(new LanguageAndDateFormat[0]));
        for (LanguageAndDateFormat ldf : ourValues.values()) {
            myDisplayStringToLdf.put(ldf.formatLocales(), ldf);

        }
    }

    @Override
    protected String objectToString(LanguageAndDateFormat obj) {
        return obj.formatLocales();
    }

    @Override
    protected LanguageAndDateFormat stringToObject(String value) {
        return myDisplayStringToLdf.get(value);
    }

    private static Map<String, LanguageAndDateFormat> createLanguageAndFormatValues() {
        Locale[] availableLocales = Locale.getAvailableLocales();
        Arrays.sort(availableLocales, GanttLanguage.LEXICOGRAPHICAL_LOCALE_COMPARATOR);
        Map<String, LanguageAndDateFormat> ldfs = new LinkedHashMap<String, LanguageAndDateFormat>();
        for (Locale l : availableLocales) {
            String key = new LanguageAndDateFormat(l).toString();
            LanguageAndDateFormat ldf = ldfs.get(key);
            if (ldf == null) {
                ldf = new LanguageAndDateFormat(l);
                ldfs.put(key, ldf);
            }
            ldf.addLocale(l);
        }
        return ldfs;
    }

    public void setSelectedLocale(Locale l) {
        String key = new LanguageAndDateFormat(l).toString();
        LanguageAndDateFormat ldf = ourValues.get(key);
        if (ldf != null) {
            setSelectedValue(ldf);
        }
    }
}
