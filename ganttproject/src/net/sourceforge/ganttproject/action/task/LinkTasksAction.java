/*
 * Created on 23.10.2005
 */
package net.sourceforge.ganttproject.action.task;

import java.util.List;

import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

public class LinkTasksAction extends TaskActionBase {
    public LinkTasksAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade) {
        super(taskManager, selectionManager, uiFacade);
    }
    
    protected String getIconFilePrefix() {
        return "link_";
    }

    protected String getLocalizedName() {
        return getI18n("link");
    }


    protected void run(List<Task> selection) throws TaskDependencyException {
        for (int i=0; i<selection.size()-1; i++) {
            Task dependant = selection.get(i+1);
            Task dependee = selection.get(i);
            if (getTaskManager().getDependencyCollection().canCreateDependency(dependant, dependee)) {
                getTaskManager().getDependencyCollection().createDependency(dependant, dependee);                
            }
        }
        // Update (un)link buttons
        setEnabled(false);
        UnlinkTasksAction unlinkTasksAction = (UnlinkTasksAction) myUIFacade.getTaskTree().getUnlinkTasksAction();
        unlinkTasksAction.setEnabled(unlinkTasksAction.isEnabled(selection));
    }                

    protected boolean isEnabled(List<Task> selection) {
        if(selection.size() <= 1) {
            return false;
        }
        for (int i = 0; i < selection.size(); i++) {
            Task nextTask = selection.get(i);
            if (nextTask.getDependencies().hasLinks(selection) == false ) {
                return true;
            }
        }
        return false;
    }
}
