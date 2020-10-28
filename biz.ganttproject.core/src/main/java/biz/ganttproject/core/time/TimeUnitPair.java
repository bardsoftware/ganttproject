/*
 * Created on 09.11.2004
 */
package biz.ganttproject.core.time;

/**
 * @author bard
 */
public class TimeUnitPair {
  private final TimeUnit myBottomTimeUnit;

  private final TimeUnit myTopTimeUnit;

  private final TimeUnitStack myTimeUnitStack;

  /** Used scale for this TimeUnit */
  private final int myDefaultUnitWidth;

  public TimeUnitPair(TimeUnit topUnit, TimeUnit bottomUnit, TimeUnitStack timeUnitStack, int defaultUnitWidth) {
    myTopTimeUnit = topUnit;
    myBottomTimeUnit = bottomUnit;
    myTimeUnitStack = timeUnitStack;
    myDefaultUnitWidth = defaultUnitWidth;
  }

  public TimeUnit getTopTimeUnit() {
    return myTopTimeUnit;
  }

  public TimeUnit getBottomTimeUnit() {
    return myBottomTimeUnit;
  }

  public TimeUnitStack getTimeUnitStack() {
    return myTimeUnitStack;
  }

  /** @return the scale for this TimeUnit */
  public int getDefaultUnitWidth() {
    return myDefaultUnitWidth;
  }
}
