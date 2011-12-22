package net.sourceforge.ganttproject.chart.timeline;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.time.TimeUnitText;

public class MonthTextFormatter extends CachingTextFormatter implements
        TimeFormatter {
    public MonthTextFormatter() {
        initFormats();
    }

    @Override
    protected TimeUnitText[] createTimeUnitText(Date adjustedLeft) {
        TimeUnitText result;
        String longText = myLongFormat.format(adjustedLeft);
        String mediumText = myMediumFormat.format(adjustedLeft);
        String shortText = myShortFormat.format(adjustedLeft);
        result = new TimeUnitText(longText, mediumText, shortText);
        return new TimeUnitText[] {result};
    }

    private void initFormats() {
        myLongFormat = GanttLanguage.getInstance()
                .createDateFormat("MMMM yyyy");
        myMediumFormat = GanttLanguage.getInstance().createDateFormat(
                "MMM''yy");
        myShortFormat = GanttLanguage.getInstance().createDateFormat("MM''yy");
    }

    @Override
    public void languageChanged(Event event) {
        super.languageChanged(event);
        initFormats();
    }

    private SimpleDateFormat myLongFormat;

    private SimpleDateFormat myMediumFormat;

    private SimpleDateFormat myShortFormat;
}
