/*
 * Created on 06.03.2005
 */
package net.sourceforge.ganttproject.chart.timeline;

import java.util.Date;
import java.util.HashMap;

import biz.ganttproject.core.chart.text.TimeUnitText;
import biz.ganttproject.core.time.DateFrameable;
import biz.ganttproject.core.time.TimeUnit;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.language.GanttLanguage.Listener;

/**
 * @author bard
 */
public abstract class CachingTextFormatter implements Listener {
  private final HashMap<Date, TimeUnitText[]> myTextCache = new HashMap<Date, TimeUnitText[]>();

  protected CachingTextFormatter() {
    GanttLanguage.getInstance().addListener(this);
  }

  public TimeUnitText[] format(TimeUnit timeUnit, Date baseDate) {
    TimeUnitText[] result = null;
    Date adjustedLeft = ((DateFrameable) timeUnit).adjustLeft(baseDate);
    result = getCachedText(adjustedLeft);
    if (result == null) {
      result = createTimeUnitText(adjustedLeft);
      myTextCache.put(adjustedLeft, result);
    }

    return result;
  }

  protected TimeUnitText[] getCachedText(Date startDate) {
    return myTextCache.get(startDate);
  }

  @Override
  public void languageChanged(Event event) {
    myTextCache.clear();
  }

  protected abstract TimeUnitText[] createTimeUnitText(Date adjustedLeft);

}
