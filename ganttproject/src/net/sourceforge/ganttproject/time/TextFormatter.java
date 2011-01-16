package net.sourceforge.ganttproject.time;

import java.util.Date;

public interface TextFormatter {
    public TimeUnitText format(TimeUnit timeUnit, Date baseDate);
}
