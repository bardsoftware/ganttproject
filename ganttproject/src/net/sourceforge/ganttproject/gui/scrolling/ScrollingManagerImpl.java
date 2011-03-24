/*
 * Created on 26.02.2005
 */
package net.sourceforge.ganttproject.gui.scrolling;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.task.TaskLength;


/**
 * @author bard
 */
public class ScrollingManagerImpl implements ScrollingManager {

    public ScrollingManagerImpl() {
    }

    public void scrollBy(TaskLength duration) {
        for (ScrollingListener l : myListeners) {
            l.scrollBy(duration);
        }
    }
    public void scrollBy(int pixels) {
        for (ScrollingListener l : myListeners) {
            l.scrollBy(pixels);
        }
    }

    public void scrollTo(Date date) {
        for (ScrollingListener l : myListeners) {
            l.scrollTo(date);
        }
    }

    public void addScrollingListener(ScrollingListener listener) {
        myListeners.add(listener);
    }

    public void removeScrollingListener(ScrollingListener listener) {
        myListeners.remove(listener);
    }

    List<ScrollingListener> myListeners = new ArrayList<ScrollingListener>();
}
