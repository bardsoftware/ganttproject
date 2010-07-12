package net.sourceforge.ganttproject.util;

import java.text.AttributedCharacterIterator;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

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
     * appropriate locale in relation with the string, this method will return
     * the correct date.
     * 
     * @param date
     *            String representation of a date.
     * @param locale
     *            Locale use to parse the date with.
     * @return A date object.
     * @throws ParseException
     *             Exception thrown if parsing is fruitless.
     */
    public static Date parseDate(String date)
            throws ParseException {
        DateFormat[] formats = new DateFormat[] {
        		GanttLanguage.getInstance().getShortDateFormat(),
        		GanttLanguage.getInstance().getMediumDateFormat(),
        		GanttLanguage.getInstance().getLongDateFormat()
        };
        //DateFormat dfFull = DateFormat.getDateInstance(DateFormat.FULL, locale);
        for (int i=0; i<formats.length; i++) {
        	try {
        		return formats[i].parse(date);
        	}
        	catch (ParseException e) {
        		if (i+1 == formats.length) {
        			throw e;
        		}
        	}
        	catch (IllegalArgumentException e) {
        		if (i+1 == formats.length) {
        			throw e;
        		}
        		
        	}
        }
        throw new ParseException("Failed to parse date="+date, 0);
    }
}
