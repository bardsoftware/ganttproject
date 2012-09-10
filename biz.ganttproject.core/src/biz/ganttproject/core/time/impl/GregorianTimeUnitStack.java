/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.core.time.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import biz.ganttproject.core.time.DateFrameable;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.core.time.TimeUnitFunctionOfDate;
import biz.ganttproject.core.time.TimeUnitGraph;
import biz.ganttproject.core.time.TimeUnitPair;
import biz.ganttproject.core.time.TimeUnitStack;


/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 01.02.2004
 */
public class GregorianTimeUnitStack implements TimeUnitStack {
  private static TimeUnitGraph ourGraph = new TimeUnitGraph();

  private static final DateFrameable DAY_FRAMER = new FramerImpl(Calendar.DATE);

  private static final DateFrameable MONTH_FRAMER = new FramerImpl(Calendar.MONTH);

  private static final DateFrameable HOUR_FRAMER = new FramerImpl(Calendar.HOUR);

  private static final DateFrameable MINUTE_FRAMER = new FramerImpl(Calendar.MINUTE);

  public static final TimeUnit SECOND;// =
                                      // ourGraph.createAtomTimeUnit("second");

  public static final TimeUnit MINUTE;// = ourGraph.createTimeUnit("minute",
                                      // SECOND, 60);

  public static final TimeUnit HOUR;// = ourGraph.createTimeUnit("hour", MINUTE,
                                    // 60);

  public static final TimeUnit DAY;

  public static final TimeUnitFunctionOfDate MONTH;

  private static final HashMap<TimeUnit, Integer> ourUnit2field = new HashMap<TimeUnit, Integer>();
  static {
    SECOND = ourGraph.createAtomTimeUnit("second");
    MINUTE = ourGraph.createDateFrameableTimeUnit("minute", SECOND, 60, MINUTE_FRAMER);
    HOUR = ourGraph.createDateFrameableTimeUnit("hour", MINUTE, 60, HOUR_FRAMER);

    DAY = ourGraph.createDateFrameableTimeUnit("day", HOUR, 24, DAY_FRAMER);
    MONTH = ourGraph.createTimeUnitFunctionOfDate("month", DAY, MONTH_FRAMER);
    ourUnit2field.put(DAY, new Integer(Calendar.DAY_OF_MONTH));
    ourUnit2field.put(HOUR, new Integer(Calendar.HOUR_OF_DAY));
    ourUnit2field.put(MINUTE, new Integer(Calendar.MINUTE));
    ourUnit2field.put(SECOND, new Integer(Calendar.SECOND));
  }

  public GregorianTimeUnitStack() {

  }

  @Override
  public TimeUnit getDefaultTimeUnit() {
    return DAY;
  }

  @Override
  public TimeUnitPair[] getTimeUnitPairs() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public DateFormat[] getDateFormats() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DateFormat getTimeFormat() {
    return null;
  }

  @Override
  public String encode(TimeUnit timeUnit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TimeDuration createDuration(TimeUnit timeUnit, Date startDate, Date endDate) {
    return null; // To change body of implemented methods use File | Settings |
                 // File Templates.
  }

  
  @Override
  public TimeDuration createDuration(TimeUnit timeUnit, int count) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TimeUnit findTimeUnit(String code) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TimeDuration parseDuration(String duration) throws ParseException {
    // TODO Auto-generated method stub
    return null;
  }
  
  
}
