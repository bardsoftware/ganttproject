package net.sourceforge.ganttproject.time.gregorian;

import java.text.MessageFormat;
import java.util.Date;

import net.sourceforge.ganttproject.time.TextFormatter;
import net.sourceforge.ganttproject.time.TimeUnitText;

public class DayTextFormatter extends CachingTextFormatter implements TextFormatter {
//    /** cache for holding formatted day names * */
//    private final HashMap<Date, String> textCache = new HashMap<Date, String>();

    @Override
    protected TimeUnitText createTimeUnitText(Date adjustedLeft) {
        return new TimeUnitText(MessageFormat.format("{0}", new Object[] { ""
              + adjustedLeft.getDate() }));
    }

//    public TimeUnitText format(TimeUnit timeUnit, Date baseDate) {
//        String result = null;
//        if (timeUnit instanceof DateFrameable) {
//            Date adjustedLeft = ((DateFrameable) timeUnit).adjustLeft(baseDate);
//            result = (String) textCache.get(adjustedLeft);
//            if (result == null) {
//                result = MessageFormat.format("{0}", new Object[] { ""
//                        + adjustedLeft.getDate() });
//                textCache.put(adjustedLeft, result);
//            }
//        }
//        return new TimeUnitText(result);
//    }

}
