/*
 * Created on 26.02.2005
 */
package net.sourceforge.ganttproject.gui.scrolling;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import biz.ganttproject.core.time.TimeDuration;


/**
 * @author bard
 */
public class ScrollingManagerImpl implements ScrollingManager {

  public ScrollingManagerImpl() {
  }

  @Override
  public void scrollBy(TimeDuration duration) {
    for (ScrollingListener l : myListeners) {
      l.scrollBy(duration);
    }
  }

  @Override
  public void scrollBy(int pixels) {
    for (ScrollingListener l : myListeners) {
      l.scrollBy(pixels);
    }
  }

  @Override
  public void scrollTo(Date date) {
    for (ScrollingListener l : myListeners) {
      l.scrollTo(date);
    }
  }

  @Override
  public void addScrollingListener(ScrollingListener listener) {
    myListeners.add(listener);
  }

  @Override
  public void removeScrollingListener(ScrollingListener listener) {
    myListeners.remove(listener);
  }

  List<ScrollingListener> myListeners = new ArrayList<ScrollingListener>();
}
