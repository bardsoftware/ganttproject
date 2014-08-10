/*
 * Created on 09.11.2004
 */
package biz.ganttproject.core.time;


import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

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

  TimeDuration createDuration(TimeUnit timeUnit, int count);
  TimeDuration createDuration(TimeUnit timeUnit, Date startDate, Date endDate);
  
  TimeDuration parseDuration(String duration) throws ParseException;
  class Util {
    /**
     * @return a common TimeUnit for the given units or null if none if found
     *         (should not happen since all should be derived from atom)
     */
    public static TimeUnit findCommonUnit(TimeUnit unit1, TimeUnit unit2) {

      // Create (cache) list with TimeUnits which can be derived from unit1
      ArrayList<TimeUnit> units1 = new ArrayList<TimeUnit>();
      TimeUnit current = unit1;
      do {
        units1.add(current);
      } while ((current = current.getDirectAtomUnit()) != null);

      // Now compare lists to find a common unit
      current = unit2;
      while (current != null) {
        Iterator<TimeUnit> u1Iterator = units1.iterator();
        while (u1Iterator.hasNext()) {
          TimeUnit nextU1 = u1Iterator.next();
          if (current.equals(nextU1)) {
            return current;
          }
        }
        current = current.getDirectAtomUnit();
      }
      return null;
    }
  }
}
