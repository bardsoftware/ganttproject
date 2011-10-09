package net.sourceforge.ganttproject.time;

import java.util.Date;

import net.sourceforge.ganttproject.time.TimeUnitGraph.Composition;

/**
 * @author bard Date: 01.02.2004
 */
public class TimeUnitImpl implements TimeUnit {
    private final String myName;

    private final TimeUnitGraph myGraph;

    private final TimeUnit myDirectAtomUnit;

    private TextFormatter myTextFormatter;

    public TimeUnitImpl(String name, TimeUnitGraph graph,
            TimeUnit directAtomUnit) {
        myName = name;
        myGraph = graph;
        myDirectAtomUnit = directAtomUnit;
    }

    public String getName() {
        return myName;
    }

    public boolean isConstructedFrom(TimeUnit atomUnit) {
        return myGraph.getComposition(this, atomUnit) != null;
    }

    public int getAtomCount(TimeUnit atomUnit) {
        Composition composition = myGraph.getComposition(this, atomUnit);
        if (composition == null) {
            throw new RuntimeException(
                    "Failed to find a composition of time unit=" + this
                            + " from time unit=" + atomUnit);
        }
        return composition.getAtomCount();
    }

    public TimeUnit getDirectAtomUnit() {
        return myDirectAtomUnit;
    }

    @Override
    public String toString() {
        return getName() + " hash=" + hashCode();
    }

    public void setTextFormatter(TextFormatter formatter) {
        myTextFormatter = formatter;
    }

    public TimeUnitText format(Date baseDate) {
        return myTextFormatter == null ? new TimeUnitText("") : myTextFormatter
                .format(this, baseDate);
    }

    protected TextFormatter getTextFormatter() {
        return myTextFormatter;
    }

    public Date adjustRight(Date baseDate) {
        throw new UnsupportedOperationException("Time unit=" + this
                + " doesnt support this operation");
    }

    public Date adjustLeft(Date baseDate) {
        throw new UnsupportedOperationException("Time unit=" + this
                + " doesnt support this operation");
    }

    public Date jumpLeft(Date baseDate) {
        throw new UnsupportedOperationException("Time unit=" + this
                + " doesnt support this operation");
    }

    @Override
    public boolean equals(Object obj) {
        if (false == obj instanceof TimeUnitImpl) {
            return false;
        }
        TimeUnitImpl that = (TimeUnitImpl) obj;
        return this.myName.equals(that.myName);
    }

    @Override
    public int hashCode() {
        return myName.hashCode();
    }


}
