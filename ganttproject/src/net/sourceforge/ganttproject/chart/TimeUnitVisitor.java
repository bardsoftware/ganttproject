package net.sourceforge.ganttproject.chart;

import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * Created by IntelliJ IDEA. User: Administrator Date: 02.10.2004 Time: 16:06:14
 * To change this template use Options | File Templates.
 */
public interface TimeUnitVisitor {
    void beforeProcessingTimeFrames();

    void afterProcessingTimeFrames();

    void startTimeFrame(TimeFrame timeFrame);

    void endTimeFrame(TimeFrame timeFrame);

    void startUnitLine(TimeUnit timeUnit);

    void endUnitLine(TimeUnit timeUnit);

    void nextTimeUnit(int unitIndex);

    /**
     * @return
     */
    boolean isEnabled();

    void setEnabled(boolean enabled);

}
