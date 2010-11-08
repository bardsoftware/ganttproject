package net.sourceforge.ganttproject.gui.options;

import java.awt.Frame;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.WeekendCalendarImpl;
import net.sourceforge.ganttproject.gui.projectwizard.I18N;
import net.sourceforge.ganttproject.gui.projectwizard.WeekendConfigurationPage;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

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
        bHasChange = calendar.getOnlyShowWeekends() != projectCalendar.getOnlyShowWeekends();
        for(int i = 1; i < 8; i++) {
            if(calendar.getWeekDayType(i) != projectCalendar.getWeekDayType(i)) {
                bHasChange = true;
            }
        }

        if (bHasChange) {
            // apply changes
            if (!askForApply || (askForApply && askForApplyChanges())) {
                for(int i = 1; i < 8; i++) {
                    projectCalendar.setWeekDayType(i, calendar.getWeekDayType(i));
                }
                projectCalendar.setOnlyShowWeekends(calendar.getOnlyShowWeekends());
                // Update tasks for the new weekends
                // By setting their end dates to null it gets recalculated
                TaskContainmentHierarchyFacade c = project.getTaskContainment();
                Task[] tasks = project.getTaskManager().getTasks();
                for(int i = 0; i < tasks.length; i++) {
                    tasks[i].setEnd(null);
                }
                try {
                	TaskManager taskManager = project.getTaskManager();
					taskManager.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
					taskManager.getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().adjustNestedTasks(taskManager.getRootTask());
				} catch (TaskDependencyException e) {
					GPLogger.log(e);
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
        calendar.setOnlyShowWeekends(projectCalendar.getOnlyShowWeekends());
        weekendConfigurationPanel = new WeekendConfigurationPage(calendar, new I18N(), project, false);
        vb.add(weekendConfigurationPanel.getComponent());
    }
}
