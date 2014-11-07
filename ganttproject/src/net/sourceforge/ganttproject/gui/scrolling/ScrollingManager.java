/*
 * Created on 26.02.2005
 */
package net.sourceforge.ganttproject.gui.scrolling;

import java.util.Date;

import biz.ganttproject.core.time.TimeDuration;


/**
 * @author bard
 */
public interface ScrollingManager {
  /**
   * Scrolls the view by a number of days
   * 
   * @param taskLength
   *          are the number of days to scroll. If days < 0 it scrolls to the
   *          right otherwise to the left.
   */
  void scrollBy(TimeDuration taskLength);

  void scrollBy(int pixels);

  /** Scrolls the view to the given Date */
  void scrollTo(Date date);

  void addScrollingListener(ScrollingListener listener);

  void removeScrollingListener(ScrollingListener listener);
}
