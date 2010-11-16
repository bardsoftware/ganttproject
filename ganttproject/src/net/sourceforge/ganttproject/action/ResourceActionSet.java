package net.sourceforge.ganttproject.action;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.ResourceContext;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;

public class ResourceActionSet {
	private final DeleteHumanAction myDeleteHumanAction;

    private final RoleManager myRoleManager;

    private final HumanResourceManager myManager;

    private final GanttProject myProjectFrame;

    private AbstractAction[] myActions;
	
	public ResourceActionSet(IGanttProject project, ResourceContext context,
            GanttProject projectFrame, UIFacade uiFacade) {
        myManager = project.getHumanResourceManager();
        myRoleManager = project.getRoleManager();
        myProjectFrame = projectFrame;
        myDeleteHumanAction = new DeleteHumanAction(myManager, context, myProjectFrame, uiFacade);
    }

    public AbstractAction[] getActions() {
        if (myActions == null) {
			myActions = new AbstractAction[] {
					new NewHumanAction(myManager, myRoleManager,
							myProjectFrame, myProjectFrame),
					myDeleteHumanAction };
        }
        return myActions;
    }

    public Action getDeleteHumanAction() {
    	return myDeleteHumanAction;
    }
}
