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
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.GPCalendar.DayType;
import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.DefaultEnumerationOption;

import com.google.common.base.Functions;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.CalendarEditorPanel;
import net.sourceforge.ganttproject.calendar.XMLCalendarOpen;
import net.sourceforge.ganttproject.calendar.XMLCalendarOpen.MyException;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
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
      resetValue(i18n("none"), true);
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
    public void setValue(String value) {
      super.setValue(value);
      if (getSelectedUrl() != null) {
        myCalendar.setBaseCalendarID(getSelectedUrl().toString());
        loadCalendar(myCalendar, getSelectedUrl());
      }
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
      resetValue(objectToString(initialValue), true);
    }

    @Override
    protected String objectToString(SchedulingEnum obj) {
      return getID() + "." + obj.name().toLowerCase();
    }
  }

  public WeekendConfigurationPage(final GPCalendarCalc calendar, I18N i18n, IGanttProject project) {
    OptionsPageBuilder builder = new OptionsPageBuilder();

    myI18N = i18n;
    myPanel = new JPanel(new BorderLayout());

    JPanel panel = new JPanel();
    {
      panel.add(new JLabel(GanttLanguage.getInstance().getText("chooseWeekend")));
      Box weekendBox = Box.createHorizontalBox();
      for (JCheckBox checkBox : createWeekendCheckBoxes(calendar, i18n.getDayNames())) {
        weekendBox.add(checkBox);
        weekendBox.add(Box.createHorizontalStrut(10));
      }
      weekendBox.add(Box.createHorizontalGlue());
      panel.add(weekendBox);

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
      panel.add(builder.createOptionLabel(null, myRenderWeekendOption));
      panel.add(builder.createOptionComponent(null, myRenderWeekendOption));
    }
    {
      myCalendarOption = createCalendarOption(calendar);
      panel.add(builder.createOptionLabel(null, myCalendarOption));
      panel.add(builder.createOptionComponent(null, myCalendarOption));
    }
    OptionsPageBuilder.TWO_COLUMN_LAYOUT.layout(panel, 3);
    UIUtil.createTitle(panel, GanttLanguage.getInstance().getText("selectProjectWeekend"));
    myPanel.add(panel, BorderLayout.NORTH);

    final CalendarEditorPanel editorPanel = new CalendarEditorPanel(calendar);
    myCalendarOption.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        editorPanel.reload(calendar);
      }
    });
    myPanel.add(editorPanel.createComponent(), BorderLayout.CENTER);
  }

  private CalendarOption createCalendarOption(GPCalendar calendar) {
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
    return new CalendarOption(calendar, Lists.newArrayList(sortedCalendars.values()), Lists.newArrayList(sortedCalendars.keySet()));
  }

  private List<JCheckBox> createWeekendCheckBoxes(final GPCalendar calendar, String[] names) {
    Supplier<Integer> counter = new Supplier<Integer>() {
      public Integer get() {
        int count = 0;
        for (int i = 1; i <= 7; i++) {
          if (calendar.getWeekDayType(i) == DayType.WEEKEND) {
            count++;
          }
        }
        return count;
      }
    };
    List<JCheckBox> result = Lists.newArrayListWithExpectedSize(7);
    int day = Calendar.MONDAY;
    for (int i = 0; i < 7; i++) {
      JCheckBox nextCheckBox = new JCheckBox();
      nextCheckBox.setSelected(calendar.getWeekDayType(day) == GPCalendar.DayType.WEEKEND);
      nextCheckBox.setAction(new CheckBoxAction(calendar, day, names[day - 1], nextCheckBox.getModel(), counter));
      result.add(nextCheckBox);
      if (++day >= 8) {
        day = 1;
      }
    }
    return result;
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
    private final GPCalendar myCalendar;
    private final Supplier<Integer> myCounter;

    CheckBoxAction(GPCalendar calendar, int day, String dayName, ButtonModel model, Supplier<Integer> checkedDaysCounter) {
      super(dayName);
      myCalendar = calendar;
      myDay = day;
      myModelButton = model;
      myCounter = checkedDaysCounter;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (myCounter.get() == 7) {
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
