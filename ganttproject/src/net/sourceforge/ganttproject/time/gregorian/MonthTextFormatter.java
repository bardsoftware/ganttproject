package net.sourceforge.ganttproject.time.gregorian;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.time.TextFormatter;
import net.sourceforge.ganttproject.time.TimeUnitText;

public class MonthTextFormatter extends CachingTextFormatter implements
        TextFormatter {
    public MonthTextFormatter() {
        initFormats();
    }

    protected TimeUnitText createTimeUnitText(Date adjustedLeft) {
        TimeUnitText result;
        String longText = MessageFormat.format("{0}",
                new Object[] { myLongFormat.format(adjustedLeft) });
        String mediumText = MessageFormat.format("{0}",
                new Object[] { myMediumFormat.format(adjustedLeft) });
        String shortText = MessageFormat.format("{0}",
                new Object[] { myShortFormat.format(adjustedLeft) });
        result = new TimeUnitText(longText, mediumText, shortText);
        return result;
    }

    private void initFormats() {
        myLongFormat = GanttLanguage.getInstance()
                .createDateFormat("MMMM yyyy");
        myMediumFormat = GanttLanguage.getInstance().createDateFormat(
                "MMM - yy");
        myShortFormat = GanttLanguage.getInstance().createDateFormat("MM - yy");
    }

    public void languageChanged(Event event) {
        super.languageChanged(event);
        initFormats();
    }

    private SimpleDateFormat myLongFormat;

    private SimpleDateFormat myMediumFormat;

    private SimpleDateFormat myShortFormat;
}
