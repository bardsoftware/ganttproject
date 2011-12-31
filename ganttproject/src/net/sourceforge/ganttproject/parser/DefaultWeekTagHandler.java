package net.sourceforge.ganttproject.parser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import net.sourceforge.ganttproject.calendar.GPCalendar;

import org.xml.sax.Attributes;

public class DefaultWeekTagHandler implements TagHandler {

    private GPCalendar myGPCalendar;

    private Calendar myCalendar = GregorianCalendar.getInstance(Locale.ENGLISH);

    private SimpleDateFormat myShortFormat = new SimpleDateFormat("EEE",
            Locale.ENGLISH);

    public DefaultWeekTagHandler(GPCalendar calendar) {
        myGPCalendar = calendar;
        for (int i = 1; i <= 7; i++) {
            myGPCalendar.setWeekDayType(i, GPCalendar.DayType.WORKING);
        }
    }

    @Override
    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) throws FileFormatException {
        if ("default-week".equals(qName)) {
            loadCalendar(attrs);
        }
    }

    private void loadCalendar(Attributes attrs) {
        for (int i = 1; i <= 7; i++) {
            String nextDayName = getShortDayName(i);
            String nextEncodedType = attrs.getValue(nextDayName);
            if ("1".equals(nextEncodedType)) {
                myGPCalendar.setWeekDayType(i, GPCalendar.DayType.WEEKEND);
            }
        }

    }

    private String getShortDayName(int i) {
        myCalendar.set(Calendar.DAY_OF_WEEK, i);
        return myShortFormat.format(myCalendar.getTime()).toLowerCase();
    }

    @Override
    public void endElement(String namespaceURI, String sName, String qName) {
    }

}
