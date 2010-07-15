/*
 * Created on 26.02.2005
 */
package net.sourceforge.ganttproject.gui.scrolling;

import java.util.Date;

/**
 * @author bard
 */
public interface ScrollingListener {
    void scrollLeft();

    void scrollRight();

    void scrollLeft(Date date);
}
