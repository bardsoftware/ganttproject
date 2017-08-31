package net.sourceforge.ganttproject.action.resource;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.GanttDialogImportLdap;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.TaskManager;

public class ResourceImportLdapAction extends ResourceAction {

	 private final TaskManager myTaskManager;

	  private final RoleManager myRoleManager;

	  private final UIFacade myUiFacade;

	  private final HumanResourceManager myHRManager;

	public ResourceImportLdapAction(HumanResourceManager hrManager,TaskManager taskManager, RoleManager roleManager,
		      GanttProject project) {
		super("resource.importLDAP", hrManager);

		myTaskManager = taskManager;
		myRoleManager = roleManager;
		myUiFacade = project.getUIFacade();
		myHRManager = hrManager;


		// TODO Auto-generated constructor stub
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		GanttDialogImportLdap gd = new GanttDialogImportLdap(myUiFacade,myHRManager);
		gd.setVisible(true);

	}


}
