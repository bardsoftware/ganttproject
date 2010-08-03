package net.sourceforge.ganttproject.action.resource;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.GanttDialogPerson;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ProjectResource;
import net.sourceforge.ganttproject.resource.ResourceContext;

public class ResourcePropertiesAction extends GPAction {
	private final IGanttProject myProject;
	private final UIFacade myUIFacade;
	private HumanResource mySelectedResource;

	public ResourcePropertiesAction(IGanttProject project, UIFacade uiFacade) {
		myProject = project;
		myUIFacade = uiFacade;
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_DOWN_MASK));
	}
	protected String getLocalizedName() {
		return getI18n("propertiesHuman");
	}

	protected String getTooltipText() {
		return getI18n("propertiesHuman");
	}

	protected String getIconFilePrefix() {
		return "properties_";
	}

	public void actionPerformed(ActionEvent arg0) {
        if (getSelectedResource()!=null) {
            GanttDialogPerson dp = new GanttDialogPerson(getUIFacade(), GanttLanguage.getInstance(), getSelectedResource());
            dp.setVisible(true);
            if (dp.result()) {
                getProject().setModified(true);
            }            
        }
	}

	private IGanttProject getProject() {
		return myProject;
	}
	private UIFacade getUIFacade() {
		return myUIFacade;
	}

	private HumanResource getSelectedResource() {
		return mySelectedResource;
	}

	public void setContext(ResourceContext context) {
		ProjectResource[] resources = context.getResources();
		if (resources.length==1) {
			mySelectedResource = (HumanResource) resources[0];
			setEnabled(true);
		}
		else {
			setEnabled(false);
		}
	}

}
