/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
        super(language.getCorrectedLabel("editPublicHolidays"));
        myProject = project;
        myUIFacade = uiFacade;
        this.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource(
                "/icons/holidays_16.gif")));
    }

    public void actionPerformed(ActionEvent arg0) {
        final GanttDialogPublicHoliday dialog = new GanttDialogPublicHoliday(myProject);
        Component dialogContent = dialog.getContentPane();
        myUIFacade.createDialog(dialogContent, new Action[] { new OkAction() {
            public void actionPerformed(ActionEvent e) {
                updateHolidays(dialog.getHolidays());
                myProject.setModified();
                try {
                    myProject.getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
                } catch (TaskDependencyException e1) {
                    GPLogger.getLogger(PublicHolidayDialogAction.class).log(Level.SEVERE,
                            "Exception after changing holidays", e1);
                }
                myUIFacade.getActiveChart().reset();
            }
        }, CancelAction.EMPTY }, "").show();
    }

    private void updateHolidays(List<GanttCalendar> holidays) {
        myProject.getActiveCalendar().clearPublicHolidays();
        for (GanttCalendar holiday : holidays) {
            myProject.getActiveCalendar().setPublicHoliDayType(
                    holiday.getTime());
        }
    }
}
