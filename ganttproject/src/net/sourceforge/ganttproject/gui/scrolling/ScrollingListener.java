/*
 * Created on 26.02.2005
 */
package net.sourceforge.ganttproject.gui.scrolling;

import java.util.Date;

/**
 * @author bard
 */
public interface ScrollingListener {
    /**
     * Scrolls the view to the left
     * @param days are the number of days to scroll
     */
    void scrollLeft(int days);

    /**
     * Scrolls the view to the right
     * @param days are the number of days to scroll
     */
    void scrollRight(int days);

    void scrollLeft(Date date);
}
