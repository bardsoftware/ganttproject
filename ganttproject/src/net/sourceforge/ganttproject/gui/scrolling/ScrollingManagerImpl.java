/*
 * Created on 26.02.2005
 */
package net.sourceforge.ganttproject.gui.scrolling;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @author bard
 */
public class ScrollingManagerImpl implements ScrollingManager {

    public ScrollingManagerImpl() {
    }

    public void scrollLeft(int days) {
        for (int i = 0; i < myListeners.size(); i++) {
            ScrollingListener nextListener = myListeners
                    .get(i);
            nextListener.scrollLeft(days);
        }
    }

    public void scrollRight(int days) {
        for (int i = 0; i < myListeners.size(); i++) {
            ScrollingListener nextListener = myListeners
                    .get(i);
            nextListener.scrollRight(days);
        }
    }

    public void scrollLeft(Date date) {
        for (int i = 0; i < myListeners.size(); i++) {
            ScrollingListener nextListener = myListeners
                    .get(i);
            nextListener.scrollLeft(date);
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
