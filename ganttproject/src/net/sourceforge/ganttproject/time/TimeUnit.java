package net.sourceforge.ganttproject.time;

/**
 * Created by IntelliJ IDEA.
 *
 * @author bard Date: 31.01.2004
 */
public interface TimeUnit extends DateFrameable {
    public String getName();

    public boolean isConstructedFrom(TimeUnit unit);

    /**
     * @return number of atoms used to create current TimeUnit
     * @throws UnsupportedOperationException
     *             if current TimeUnit does not have constant number of atoms
     */
    public int getAtomCount(TimeUnit atomUnit);

    /** @return the TimeUnit which is used to build the current TimeUnit */
    public TimeUnit getDirectAtomUnit();

    public int DAY = 0;
}
