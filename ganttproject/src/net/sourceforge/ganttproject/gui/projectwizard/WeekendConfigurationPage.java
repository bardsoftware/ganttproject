/*
Copyright 2014 BarD Software s.r.o
Copyright 2003-2013 GanttProject Team

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
package net.sourceforge.ganttproject.gui.projectwizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Calendar;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.CalendarEditorPanel;
import net.sourceforge.ganttproject.calendar.GPCalendarProvider;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.language.GanttLanguage;
import biz.ganttproject.core.calendar.AlwaysWorkingTimeCalendarImpl;
import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.calendar.GPCalendar.DayType;
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.DefaultEnumerationOption;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

/**
 * This page provides UI for configuring project weekend days and public holidays
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class WeekendConfigurationPage implements WizardPage {
  private final Box myBox = Box.createVerticalBox();

  private final JPanel myPanel;

  private final I18N myI18N;

  private final CalendarOption myCalendarOption;

  private final WeekendSchedulingOption myRenderWeekendOption;

  static class CalendarOption extends DefaultEnumerationOption<GPCalendar> {

    private final GPCalendar myCalendar;

    private static <T> List<T> append(List<T> list, T s) {
      List<T> result = Lists.newArrayList(list);
      result.add(s);
      return result;
    }

    public CalendarOption(GPCalendar calendar, List<GPCalendar> allCalendars, GPCalendar emptyCalendar) {
      super("project.calendar", append(allCalendars, emptyCalendar).toArray(new GPCalendar[0]));
      resetValue(i18n("none"), true);
      myCalendar = calendar;
      if (calendar.getBaseCalendarID() != null) {
        GPCalendar baseCalendar = stringToObject(calendar.getBaseCalendarID());
        if (baseCalendar != null) {
          setSelectedValue(baseCalendar);
        }
      }
    }

    @Override
    protected String objectToString(GPCalendar obj) {
      return obj.getName();
    }

    @Override
    public void setValue(String value) {
      super.setValue(value);
      if (getSelectedValue() != null) {
        myCalendar.setBaseCalendarID(getSelectedValue().getID());
        loadCalendar(myCalendar, getSelectedValue());
      }
    }

    private static void loadCalendar(GPCalendar calendar, GPCalendar baseCalendar) {
      calendar.setPublicHolidays(baseCalendar.getPublicHolidays());
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
    AlwaysWorkingTimeCalendarImpl emptyCalendar = new AlwaysWorkingTimeCalendarImpl();
    emptyCalendar.setName(GanttLanguage.getInstance().getText("none"));
    return new CalendarOption(calendar, GPCalendarProvider.getInstance().getCalendars(), emptyCalendar);
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
