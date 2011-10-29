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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LanguageAndDateFormat {
    static final Date NOW = new Date();
    public final String language;
    public final DateFormat dateFormat;
    private final List<Locale> myLocales = new ArrayList<Locale>();
    private String dateFormatSample;

    LanguageAndDateFormat(Locale l) {
        this.language = l.getLanguage();
        this.dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, l);
        this.dateFormatSample = dateFormat.format(NOW);
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof LanguageAndDateFormat) {
            return toString().equals(that.toString());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return language + ";" + dateFormatSample;
    }

    void addLocale(Locale l) {
        myLocales.add(l);
    }

    String formatLocales() {
        StringBuilder result = new StringBuilder();
        Locale pure = new Locale(language);
        result.append(pure.getDisplayLanguage(Locale.US));
        String countryList = getCountryList();
        if (!countryList.isEmpty()) {
            result.append("(").append(countryList).append(")");
        }
        return result.toString();
    }

    private String getCountryList() {
        StringBuilder result = new StringBuilder();
        String delimiter = "";
        for (Locale l : myLocales) {
            result.append(delimiter);
            String country = l.getDisplayCountry(l);
            if (!country.isEmpty()) {
                result.append(country);
                delimiter = ", ";
            }
        }
        return result.toString();
    }
    public String formatDate(Date date) {
        return dateFormat.format(date);
    }

}
