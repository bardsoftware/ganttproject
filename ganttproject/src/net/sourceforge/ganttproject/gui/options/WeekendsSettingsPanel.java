package net.sourceforge.ganttproject.gui.options;

import java.awt.Frame;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.WeekendCalendarImpl;
import net.sourceforge.ganttproject.gui.projectwizard.I18N;
import net.sourceforge.ganttproject.gui.projectwizard.WeekendConfigurationPage;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;

/**
 * @author Maarten Bezemer
 * @description Panel to edit the weekend settings
 */
public class WeekendsSettingsPanel extends GeneralOptionPanel {

    private final IGanttProject project;

    private WeekendConfigurationPage weekendConfigurationPanel;

    private final GPCalendar calendar;

    public WeekendsSettingsPanel(Frame owner, IGanttProject project) {
        super(GanttProject.correctLabel(
                GanttLanguage.getInstance().getText("weekends")),
                GanttLanguage.getInstance().getText("settingsWeekends"), owner);

        this.project = project;
        calendar = new WeekendCalendarImpl();

        weekendConfigurationPanel = null;
    }

    public boolean applyChanges(boolean askForApply) {
        GPCalendar projectCalendar = project.getActiveCalendar();
        bHasChange = false;
        for(int i = 1; i < 8; i++) {
            if(calendar.getWeekDayType(i) != projectCalendar.getWeekDayType(i)) {
                bHasChange = true; {
                }
            }
        }

        if (bHasChange) {
            // apply changes
            if (!askForApply || (askForApply && askForApplyChanges())) {
                for(int i = 1; i < 8; i++) {
                    projectCalendar.setWeekDayType(i, calendar.getWeekDayType(i));
                }

                // Update tasks for the new weekends
                // By setting their end dates to null it gets recalculated
                TaskContainmentHierarchyFacade c = project.getTaskContainment();
                Task[] tasks = c.getDeepNestedTasks(c.getRootTask());
                for(int i = 0; i < tasks.length; i++) {
                    tasks[i].setEnd(null);
                }
            }
        }
        return bHasChange;
    }

    // TODO It would be nicer to just update the checkboxes,
    //      but WeekendConfigurationPage does not allow it ATM
    public void initialize() {
        if(weekendConfigurationPanel != null) {
            vb.remove(weekendConfigurationPanel.getComponent());
        }

        // Make a copy of the WeekDayTypes
        GPCalendar projectCalendar = project.getActiveCalendar();
        for(int i = 1; i < 8; i++) {
            calendar.setWeekDayType(i, projectCalendar.getWeekDayType(i));
        }

        try {
            weekendConfigurationPanel = new WeekendConfigurationPage(calendar, new I18N(), project, false);
            vb.add(weekendConfigurationPanel.getComponent());
        } catch (Exception e) {
        }
    }
}
