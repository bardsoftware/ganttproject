/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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
package net.sourceforge.ganttproject.action.task;

import java.util.List;

import javax.swing.SwingUtilities;

import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.GanttDialogProperties;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

public class TaskPropertiesAction extends TaskActionBase {

    private final IGanttProject myProject;

    public TaskPropertiesAction(IGanttProject project, TaskSelectionManager selectionManager, UIFacade uiFacade) {
        super("task.properties", project.getTaskManager(), selectionManager, uiFacade, null);
        myProject = project;
    }

    @Override
    protected boolean isEnabled(List<Task> selection) {
        return selection.size() == 1;
    }

    @Override
    protected void run(List<Task> selection) throws Exception {
        if (selection.size() != 1) {
            return;
        }
        final GanttTask[] tasks = new GanttTask[] {(GanttTask)selection.get(0)};
        GanttDialogProperties pd = new GanttDialogProperties(tasks);
        getSelectionManager().setUserInputConsumer(pd);
        pd.show(myProject, getUIFacade());
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getSelectionManager().clear();
                getSelectionManager().addTask(tasks[0]);
            }
        });
    }

    @Override
    protected String getIconFilePrefix() {
        return "properties_";
    }
}
