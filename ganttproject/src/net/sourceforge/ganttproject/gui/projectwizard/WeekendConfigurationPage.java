/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Alexandre Thomas, Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.gui.projectwizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import biz.ganttproject.core.calendar.GPCalendar;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.XMLCalendarOpen;
import net.sourceforge.ganttproject.calendar.XMLCalendarOpen.MyException;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.parser.HolidayTagHandler;

/**
 * @author bard
 */
public class WeekendConfigurationPage implements WizardPage {
  private final Box myBox = Box.createVerticalBox();

  private final JPanel myPanel;

  private final I18N myI18N;

  private final CalendarOption myCalendarOption;

  private final WeekendSchedulingOption myRenderWeekendOption;

  static class CalendarOption extends DefaultEnumerationOption<URL> {

    private final List<URL> myUrls;
    private final List<String> myLabels;
    private final GPCalendar myCalendar;

    private static List<String> append(List<String> list, String s) {
      ArrayList<String> result = new ArrayList<String>(list);
      result.add(s);
      return result;
    }

    public CalendarOption(GPCalendar calendar, List<URL> urls, List<String> labels) {
      super("project.calendar", append(labels, i18n("none")));
      setValue(i18n("none"), true);
      myUrls = urls;
      myLabels = labels;
      myCalendar = calendar;
      if (calendar.getBaseCalendarID() != null) {
        int idx = Lists.transform(urls, Functions.toStringFunction()).indexOf(calendar.getBaseCalendarID());
        if (idx >= 0) {
          setValue(labels.get(idx));
        }
      }
      assert myUrls.size() == myLabels.size();
    }

    private URL getSelectedUrl() {
      if (i18n("none").equals(getValue())) {
        return null;
      }
      int idx = myLabels.indexOf(getValue());
      assert idx >= 0 && idx < myUrls.size();
      return myUrls.get(idx);
    }

    @Override
    public void commit() {
      super.commit();
      myCalendar.setBaseCalendarID(getSelectedUrl().toString());
      loadCalendar(myCalendar, getSelectedUrl());
    }

    private static void loadCalendar(GPCalendar calendar, URL url) {
      XMLCalendarOpen opener = new XMLCalendarOpen();

      HolidayTagHandler tagHandler = new HolidayTagHandler(calendar);

      opener.addTagHandler(tagHandler);
      opener.addParsingListener(tagHandler);
      try {
        opener.load(url.openStream());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  static enum SchedulingEnum {
    SCHEDULE_NONE, SCHEDULE_ALL
  }

  static class WeekendSchedulingOption extends DefaultEnumerationOption<SchedulingEnum> {
    WeekendSchedulingOption(SchedulingEnum initialValue) {
      super("project.weekendScheduling", SchedulingEnum.values());
      setValue(objectToString(initialValue), true);
    }

    @Override
    protected String objectToString(SchedulingEnum obj) {
      return getID() + "." + obj.name().toLowerCase();
    }
  }

  public WeekendConfigurationPage(final GPCalendar calendar, I18N i18n, IGanttProject project,
      boolean showPublicHolidays) {
    OptionsPageBuilder builder = new OptionsPageBuilder();

    myI18N = i18n;
    String[] dayNames = myI18N.getDayNames();
    myPanel = new JPanel(new BorderLayout());
    if (showPublicHolidays) {
      XMLCalendarOpen open = new XMLCalendarOpen();
      URL[] calendarUrls = null;
      String[] calendarLabels = null;
      try {
        open.setCalendars();
        calendarLabels = open.getLabels();
        calendarUrls = open.getCalendarResources();
      } catch (MyException e1) {
        GPLogger.log(e1);
      }

      SortedMap<String, URL> sortedCalendars = new TreeMap<String, URL>();
      for (int i = 0; i < calendarLabels.length; i++) {
    	  sortedCalendars.put(calendarLabels[i], calendarUrls[i]);
      }
      myCalendarOption = new CalendarOption(calendar, Lists.newArrayList(sortedCalendars.values()), Lists.newArrayList(sortedCalendars.keySet()));
      myBox.add(builder.createLabeledComponent(myCalendarOption));
    } else {
      myCalendarOption = null;
    }

    Box cb = Box.createVerticalBox();
    cb.add(Box.createVerticalStrut(15));
    /*
     * Table to keep all the JCheckBoxes with days of the week. It is used to
     * check if in project creation dialog all days are marked as weekend. If
     * they are, day selected last will be unmarked. See class CheckBoxAction.
     *
     * If you know better solution, do not hesitate to replace this code.
     */
    JCheckBox[] allCheckBoxes = new JCheckBox[7];
    cb.add(new JLabel(GanttLanguage.getInstance().getText("chooseWeekend")));
    cb.add(Box.createVerticalStrut(5));
    int nextDay = Calendar.MONDAY;
    for (int i = 0; i < 7; i++) {
      JCheckBox nextCheckBox = new JCheckBox();
      nextCheckBox.setSelected(calendar.getWeekDayType(nextDay) == GPCalendar.DayType.WEEKEND);
      nextCheckBox.setAction(new CheckBoxAction(calendar, nextDay, dayNames[nextDay - 1], nextCheckBox.getModel(),
          allCheckBoxes));
      cb.add(nextCheckBox);
      allCheckBoxes[i] = nextCheckBox;
      if (++nextDay >= 8) {
        nextDay = 1;
      }
    }

    cb.add(Box.createVerticalStrut(15));
    JPanel weekendPanel = new JPanel(new BorderLayout());
    weekendPanel.add(cb, BorderLayout.WEST);
    myBox.add(weekendPanel);

    myRenderWeekendOption = new WeekendSchedulingOption(calendar.getOnlyShowWeekends() ? SchedulingEnum.SCHEDULE_ALL
        : SchedulingEnum.SCHEDULE_NONE) {
      @Override
      public void commit() {
        super.commit();
        if (stringToObject(getValue()) == SchedulingEnum.SCHEDULE_ALL) {
          calendar.setOnlyShowWeekends(true);
        } else {
          calendar.setOnlyShowWeekends(false);
        }
      }
    };
    myBox.add(builder.createLabeledComponent(myRenderWeekendOption));

    JPanel projectPanel = new JPanel(new BorderLayout());
    projectPanel.add(myBox, BorderLayout.NORTH);
    myPanel.add(projectPanel);
  }

  @Override
  public String getTitle() {
    return myI18N.getProjectWeekendPageTitle();
  }

  @Override
  public Component getComponent() {
    return myPanel;
  }

  @Override
  public void setActive(boolean active) {
    if (!active) {
      myCalendarOption.commit();
      myRenderWeekendOption.commit();
    }
  }

  public boolean isChanged() {
    return myCalendarOption.isChanged() || myRenderWeekendOption.isChanged();
  }

  private static class CheckBoxAction extends AbstractAction {
    private final int myDay;
    private final ButtonModel myModelButton;
    private final JCheckBox[] myCheckBoxes;
    private final GPCalendar myCalendar;

    CheckBoxAction(GPCalendar calendar, int day, String dayName, ButtonModel model, JCheckBox[] allCheckBoxes) {
      super(dayName);
      myCalendar = calendar;
      myDay = day;
      myModelButton = model;
      myCheckBoxes = allCheckBoxes;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      // Counting number of selected days of the week.
      int count = 0;
      for (int i = 0; i < myCheckBoxes.length; i++) {
        if (myCheckBoxes[i].isSelected()) {
          count++;
        }
      }
      if (count == myCheckBoxes.length) {
        // If all days of the week are marked as weekend unmark selected the
        // last.
        myModelButton.setSelected(false);
      } else {
        myCalendar.setWeekDayType(myDay, myModelButton.isSelected() ? GPCalendar.DayType.WEEKEND
            : GPCalendar.DayType.WORKING);
      }
    }
  }
}
