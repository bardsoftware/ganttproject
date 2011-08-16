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
    private final TaskSelectionManager mySelectionManager;

    public TaskPropertiesAction(IGanttProject project, TaskSelectionManager selectionManager, UIFacade uiFacade) {
        super("propertiesTask", project.getTaskManager(), selectionManager, uiFacade);
        myProject = project;
        mySelectionManager = selectionManager;
    }

    protected boolean isEnabled(List<Task> selection) {
        return selection.size() == 1;
    }

    protected void run(List<Task> selection) throws Exception {
        if (selection.size() != 1) {
            return;
        }
        final GanttTask[] tasks = new GanttTask[] {(GanttTask)selection.get(0)};
        GanttDialogProperties pd = new GanttDialogProperties(tasks);
        mySelectionManager.setUserInputConsumer(pd);
        pd.show(myProject, getUIFacade());
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mySelectionManager.clear();
                mySelectionManager.addTask(tasks[0]);
            }
        });
    }

    protected String getIconFilePrefix() {
        return "properties_";
    }
}
