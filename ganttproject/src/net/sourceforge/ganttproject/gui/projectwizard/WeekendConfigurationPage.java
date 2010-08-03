/*
 * Created on 06.01.2005
 */
package net.sourceforge.ganttproject.gui.projectwizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Calendar;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.XMLCalendarOpen;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author bard
 */
public class WeekendConfigurationPage implements WizardPage, ActionListener {
    private final Box myBox = Box.createVerticalBox();

    private final GPCalendar myCalendar;

    private final JPanel myPanel;

    private final JComboBox myCalendarField;

    private final I18N myI18N;

    private final URL[] calendars;
    private final GanttProject myProject;

    public WeekendConfigurationPage(GPCalendar calendar, I18N i18n,
            IGanttProject project, boolean showPublicHolidays) throws Exception {
        JLabel choosePublicHoliday;
        JLabel chooseWeekend;
        JCheckBox renderWeekend;

        myCalendar = calendar;
        myCalendar.getPublicHolidays().clear();
        myProject = (GanttProject) project;
        myI18N = i18n;
        String[] dayNames = myI18N.getDayNames();
        myPanel = new JPanel(new BorderLayout());
        if (showPublicHolidays) {
            choosePublicHoliday = new JLabel(GanttLanguage.getInstance().getText("choosePublicHoliday"));
            myCalendarField = new JComboBox();
            myCalendarField.addItem(GanttLanguage.getInstance().getText("none"));
            XMLCalendarOpen open = new XMLCalendarOpen();
            open.setCalendars();
            String[] labels = open.getLabels();
            calendars = open.getCalendarResources();
            for (int i = 0; i < labels.length; i++) {
                myCalendarField.addItem(labels[i]);
            }
            myCalendarField.addActionListener(this);

            JPanel publicHolidayPanel = new JPanel(new BorderLayout());
            publicHolidayPanel.add(choosePublicHoliday, BorderLayout.WEST);
            publicHolidayPanel.add(myCalendarField);
            myBox.add(publicHolidayPanel);
            myBox.add(new JPanel());
        } else {
            myCalendarField = null;
            calendars = null;
        }

        Box cb = Box.createVerticalBox();
        /*
         * Table to keep all the JCheckBoxes with days of the week.
         * It is used to check if in project creation dialog all days are marked as weekend. If they are, day selected
         * last will be unmarked. See class CheckBoxAction.
         *
         * If you know better solution, do not hesitate to replace this code.
         */
        chooseWeekend = new JLabel(GanttLanguage.getInstance().getText("chooseWeekend"));
        JCheckBox[] allCheckBoxes = new JCheckBox[7];
        cb.add(chooseWeekend);
        int nextDay = Calendar.MONDAY;
        for (int i = 0; i < 7; i++) {
            JCheckBox nextCheckBox = new JCheckBox();
            nextCheckBox
                    .setSelected(calendar.getWeekDayType(nextDay) == GPCalendar.DayType.WEEKEND);
            nextCheckBox.setAction(new CheckBoxAction(nextDay,
                    dayNames[nextDay - 1], nextCheckBox.getModel(), allCheckBoxes));
            cb.add(nextCheckBox);
            allCheckBoxes[i] = nextCheckBox;
            if (++nextDay >= 8) {
                nextDay = 1;
            }
        }

        cb.add(Box.createVerticalStrut(15));

        renderWeekend = new JCheckBox();
        renderWeekend.setSelected(myCalendar.getOnlyShowWeekends());
        renderWeekend.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                myCalendar.setOnlyShowWeekends(((JCheckBox ) e.getSource()).isSelected());
            }
        });
        renderWeekend.setText(GanttLanguage.getInstance().getText("onlyShowWeekends"));
        cb.add(renderWeekend);

        JPanel weekendPanel = new JPanel(new BorderLayout());
        weekendPanel.add(cb, BorderLayout.WEST);
        myBox.add(weekendPanel);

        JPanel projectPanel = new JPanel(new BorderLayout());
        projectPanel.add(myBox, BorderLayout.NORTH);
        myPanel.add(projectPanel);
    }

    public String getTitle() {
        return myI18N.getProjectWeekendPageTitle();
    }

    public Component getComponent() {
        return myPanel;
    }

    public void setActive(boolean b) {
    }

    private class CheckBoxAction extends AbstractAction {
        private int myDay;
        private ButtonModel myModelButton;
        private JCheckBox[] myCheckBoxes;

        CheckBoxAction(int day, String dayName, ButtonModel model, JCheckBox[] allCheckBoxes) {
            super(dayName);
            myDay = day;
            myModelButton = model;
            myCheckBoxes = allCheckBoxes;
        }

        public void actionPerformed(ActionEvent e) {
            // Counting number of selected days of the week.
            int count = 0;
            for (int i=0; i < myCheckBoxes.length; i++) {
                if (myCheckBoxes[i].isSelected()) {
                    count++;
                }
            }
            if (count == myCheckBoxes.length) {
                // If all days of the week are marked as weekend unmark selected the last.
                myModelButton.setSelected(false);
            } else {
                WeekendConfigurationPage.this.myCalendar.setWeekDayType(myDay, myModelButton.isSelected() ?
                        GPCalendar.DayType.WEEKEND : GPCalendar.DayType.WORKING);
            }
        }

    }

    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() instanceof JComboBox) {
            if (evt.getSource() == WeekendConfigurationPage.this.myCalendarField) {
                WeekendConfigurationPage.this.myCalendar.setPublicHolidays(getProjectCalendar(),
                        WeekendConfigurationPage.this.myProject);
            }
        }
    }

    public URL getProjectCalendar() {
        if (WeekendConfigurationPage.this.myCalendarField == null) {
            return null;
        }

        int index = WeekendConfigurationPage.this.myCalendarField.getSelectedIndex();
        if (index == 0) {
            return null;
        } else {
            return (WeekendConfigurationPage.this.calendars[index - 1]);
        }
    }

}
