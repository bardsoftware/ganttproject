/*
 * Created on 23.10.2005
 */
package net.sourceforge.ganttproject.action.task;

import java.util.List;

import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

public class UnlinkTasksAction extends TaskActionBase {

    public UnlinkTasksAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade) {
        super(taskManager, selectionManager, uiFacade);
    }

    protected String getIconFilePrefix() {
        return "unlink_";
    }

    protected String getLocalizedName() {
        return getI18n("unlink");
    }

    protected boolean isEnabled(List<Task> selection) {
        for (int i = 0; i < selection.size(); i++) {
            Task nextTask = selection.get(i);
            if (nextTask.getDependencies().hasLinks(selection)) {
                return true;
            }
        }
        return false;
    }

    protected void run(List<Task> selection) throws Exception {
        for (int i=0; i<selection.size(); i++) {
            Task nextTask = selection.get(i);
            nextTask.getDependencies().clear(selection);
        }
        // Update (un)link buttons
        setEnabled(false);
        LinkTasksAction linkTasksAction = (LinkTasksAction) myUIFacade.getTaskTree().getLinkTasksAction();
        linkTasksAction.setEnabled(linkTasksAction.isEnabled(selection));
    }

}
