/**
 * 
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.GanttDialogPublicHoliday;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

/**
 * @author nbohn
 */
public class PublicHolidayDialogAction extends AbstractAction {

    private IGanttProject myProject;

    private UIFacade myUIFacade;

    static GanttLanguage language = GanttLanguage.getInstance();

    public PublicHolidayDialogAction(IGanttProject project, UIFacade uiFacade) {
        super(GanttProject.correctLabel(language.getText("editPublicHolidays")));
        myProject = project;
        myUIFacade = uiFacade;
        this.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource(
                "/icons/holidays_16.gif")));
    }

    public void actionPerformed(ActionEvent arg0) {

        // myUIFacade.showDialog(createDialogComponent(), new Action[]{okAction,
        // cancelAction});
        final GanttDialogPublicHoliday dialog = new GanttDialogPublicHoliday(myProject);
        Component dialogContent = dialog.getContentPane();
        myUIFacade.showDialog(dialogContent, new Action[] {
                new OkAction() {
                    public void actionPerformed(ActionEvent e) {
                        updateHolidays(dialog.getHolidays());
                        myProject.setModified();
                        try {
                            myProject.getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
                        } catch (TaskDependencyException e1) {
                            GPLogger.getLogger(PublicHolidayDialogAction.class).log(
                                    Level.SEVERE, "Exception after changing holidays", e1);
                        }
                        myUIFacade.getActiveChart().reset();
                    }
                }, 
                new CancelAction() {
                    public void actionPerformed(ActionEvent e) {
                    }
                }
        });
    }

    private void updateHolidays(List<GanttCalendar> holidays) {
        myProject.getActiveCalendar().getPublicHolidays().clear();
        for (int i = 0; i < holidays.size(); i++) {
            myProject.getActiveCalendar().setPublicHoliDayType(
                    holidays.get(i)
                            .getTime());
        }
    }
}
