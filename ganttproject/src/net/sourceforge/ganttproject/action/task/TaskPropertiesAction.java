package net.sourceforge.ganttproject.action.task;

import java.util.List;

import javax.swing.SwingUtilities;

import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.GanttDialogProperties;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

public class TaskPropertiesAction extends TaskActionBase {

	private RoleManager myRoleManager;
	private HumanResourceManager myHumanManager;
	private IGanttProject myProject;
    private final TaskSelectionManager mySelectionManager;

	public TaskPropertiesAction(IGanttProject project, TaskSelectionManager selectionManager, UIFacade uiFacade) {
		super(project.getTaskManager(), selectionManager, uiFacade);
		myProject = project;
        mySelectionManager = selectionManager;
		myHumanManager = (HumanResourceManager) project.getHumanResourceManager();
		myRoleManager = project.getRoleManager();
	}

	protected boolean isEnabled(List selection) {
		return selection.size()==1;
	}

	protected void run(List/*<Task>*/ selection) throws Exception {
        if (selection.size()!=1) {
            return;
        }
		final GanttTask[] tasks = new GanttTask[] {(GanttTask)selection.get(0)};
        GanttDialogProperties pd = new GanttDialogProperties(tasks);
        mySelectionManager.setUserInputConsumer(pd);
        pd.show(myProject, getUIFacade());
        if (pd.change) {
            myProject.setModified(true);
//            setRowHeight(rowHeight);
//            getResourcePanel().getResourceTreeTableModel()
//                    .updateResources();
        }
        SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				mySelectionManager.clear();
				mySelectionManager.addTask(tasks[0]);
			}
        });

	}

	protected String getLocalizedName() {
		return getI18n("propertiesTask");
	}

	protected String getIconFilePrefix() {
		return "properties_";
	}

}
