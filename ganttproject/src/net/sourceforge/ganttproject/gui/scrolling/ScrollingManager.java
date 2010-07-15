/*
 * Created on 26.02.2005
 */
package net.sourceforge.ganttproject.gui.scrolling;

import java.util.Date;

/**
 * @author bard
 */
public interface ScrollingManager {
    void scrollLeft();

    void scrollRight();

    void scrollLeft(Date date);

    void addScrollingListener(ScrollingListener listener);

    void removeScrollingListener(ScrollingListener listener);
}
