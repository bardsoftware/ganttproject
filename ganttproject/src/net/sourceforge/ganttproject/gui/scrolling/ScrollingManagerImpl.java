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

    public void scrollLeft() {
        for (int i = 0; i < myListeners.size(); i++) {
            ScrollingListener nextListener = myListeners
                    .get(i);
            nextListener.scrollLeft();
        }
    }

    public void scrollRight() {
        for (int i = 0; i < myListeners.size(); i++) {
            ScrollingListener nextListener = myListeners
                    .get(i);
            nextListener.scrollRight();
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
