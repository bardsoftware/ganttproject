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

    public TimeUnitImpl(String name, TimeUnitGraph graph,
            TimeUnit directAtomUnit) {
        myName = name;
        myGraph = graph;
        myDirectAtomUnit = directAtomUnit;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public boolean isConstructedFrom(TimeUnit atomUnit) {
        return myGraph.getComposition(this, atomUnit) != null;
    }

    @Override
    public int getAtomCount(TimeUnit atomUnit) {
        Composition composition = myGraph.getComposition(this, atomUnit);
        if (composition == null) {
            throw new RuntimeException(
                    "Failed to find a composition of time unit=" + this
                            + " from time unit=" + atomUnit);
        }
        return composition.getAtomCount();
    }

    @Override
    public TimeUnit getDirectAtomUnit() {
        return myDirectAtomUnit;
    }

    @Override
    public String toString() {
        return getName() + " hash=" + hashCode();
    }

    @Override
    public Date adjustRight(Date baseDate) {
        throw new UnsupportedOperationException("Time unit=" + this
                + " doesnt support this operation");
    }

    @Override
    public Date adjustLeft(Date baseDate) {
        throw new UnsupportedOperationException("Time unit=" + this
                + " doesnt support this operation");
    }

    @Override
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
