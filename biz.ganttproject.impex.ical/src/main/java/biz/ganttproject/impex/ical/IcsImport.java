/*
 * Copyright 2013-2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.impex.ical;

import biz.ganttproject.LoggerApi;
import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import com.google.common.collect.Lists;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.transform.recurrence.Frequency;
import net.fortuna.ical4j.util.CompatibilityHints;
import net.sourceforge.ganttproject.GPLogger;
import org.w3c.util.DateParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.*;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class IcsImport {
  static List<CalendarEvent> readEvents(InputStream input) throws ParserException, IOException {
    CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
    CalendarBuilder builder = new CalendarBuilder();
    List<CalendarEvent> gpEvents = Lists.newArrayList();
    Calendar c = builder.build(new UnfoldingReader(new InputStreamReader(input)));
    for (CalendarComponent comp : c.getComponents()) {
      if (comp instanceof VEvent) {
        VEvent event = (VEvent) comp;

        if (event.getDateTimeStart().isEmpty()) {
          LOGGER.debug("No start date found, ignoring. Event={}", new Object[] {event}, Collections.emptyMap());
          continue;
        }
        var eventStartDate = event.getDateTimeStart().get().getDate();
        if (event.getDateTimeEnd().isEmpty()) {
          LOGGER.debug("No end date found, using start date instead. Event={}", new Object[] {event}, Collections.emptyMap());
        }
        var eventEndDate = event.getDateTimeEnd().map(DateProperty::getDate).orElse(eventStartDate);
        TimeDuration oneDay = GPTimeUnitStack.createLength(GPTimeUnitStack.DAY, 1);
        if (eventEndDate != null) {
          java.util.Date startDate = GPTimeUnitStack.DAY.adjustLeft(getDate(eventStartDate));
          java.util.Date endDate = GPTimeUnitStack.DAY.adjustLeft(getDate(eventEndDate));
          var recursYearly = new AtomicBoolean(false);
          event.getProperty(Property.RRULE).ifPresent(it -> {
            if (it instanceof RRule<?> recurrenceRule) {
              var frequency = recurrenceRule.getRecur().getFrequency();
              var interval = recurrenceRule.getRecur().getInterval();
              recursYearly.set(frequency == Frequency.YEARLY && interval == 1);
            }
          });
          while (startDate.compareTo(endDate) < 0) {
            var summary = event.getSummary().map(Summary::getValue).orElse("");

            gpEvents.add(CalendarEvent.newEvent(
              startDate, recursYearly.get(), CalendarEvent.Type.HOLIDAY,
              summary.trim(),
              null));
            startDate = GPCalendarCalc.PLAIN.shiftDate(startDate, oneDay);
          }
        }
      }
    }
    return gpEvents;

  }

  private static java.util.Date getDate(Temporal t) {
    if (t instanceof LocalDate localDate) {
      return DateParser.toJavaDate(localDate);
    }
    if (t instanceof Instant instant) {
      return java.util.Date.from(instant);
    }
    if (t instanceof LocalDateTime localDateTime) {
      return DateParser.toJavaDate(localDateTime.toLocalDate());
    }
    if (t instanceof ZonedDateTime zonedDateTime) {
      return DateParser.toJavaDate(zonedDateTime.toLocalDate());
    }
    if (t instanceof OffsetDateTime offsetDateTime) {
      return DateParser.toJavaDate(offsetDateTime.toLocalDate());
    }
    throw new IllegalArgumentException("Unsupported temporal type: " + t.getClass());
  }

  private static final LoggerApi LOGGER = GPLogger.create("Import.Ics");

}