package net.sourceforge.ganttproject.chart.timeline;

import java.util.Date;

import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitText;

public interface TimeFormatter {
    TimeUnitText[] format(TimeUnit timeUnit, Date baseDate);
}
