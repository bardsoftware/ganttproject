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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.ValidationException;

public class ShortDateFormatOption extends DefaultStringOption {
    private SimpleDateFormat myDateFormat;

    public ShortDateFormatOption() {
        super("ui.dateFormat.short");
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
    public void commit() {
        super.commit();
        GanttLanguage.getInstance().setShortDateFormat(myDateFormat);
    }

    public void setSelectedLocale(Locale locale) {
        myDateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, locale);
        super.setValue(myDateFormat.toPattern(), true);
    }

    public DateFormat getSelectedValue() {
        return myDateFormat;
    }

    public String formatDate(Date date) {
        return myDateFormat.format(date);
    }
}
