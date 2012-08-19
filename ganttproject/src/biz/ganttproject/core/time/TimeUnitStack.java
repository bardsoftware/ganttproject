/*
 * Created on 09.11.2004
 */
package biz.ganttproject.core.time;


import java.text.DateFormat;
import java.util.Date;

/**
 * @author bard
 */
public interface TimeUnitStack {
  TimeUnit getDefaultTimeUnit();

  TimeUnitPair[] getTimeUnitPairs();

  String getName();

  DateFormat[] getDateFormats();

  DateFormat getTimeFormat();

  TimeUnit findTimeUnit(String code);

  String encode(TimeUnit timeUnit);

  TimeDuration createDuration(TimeUnit timeUnit, Date startDate, Date endDate);
}
