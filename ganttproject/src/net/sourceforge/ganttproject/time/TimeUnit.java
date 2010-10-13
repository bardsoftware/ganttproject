package net.sourceforge.ganttproject.time;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 *
 * @author bard Date: 31.01.2004
 */
public interface TimeUnit extends DateFrameable {
    public String getName();

    public boolean isConstructedFrom(TimeUnit unit);

    public int getAtomCount(TimeUnit atomUnit);

    public TimeUnit getDirectAtomUnit();

    public int DAY = 0;

    //
    public void setTextFormatter(TextFormatter formatter);

    public TimeUnitText format(Date baseDate);
}
