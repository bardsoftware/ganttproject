package net.sourceforge.ganttproject.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIFacade.Choice;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceContext;

/**
 * Action connected to the menu item for delete a resource
 */
public class DeleteHumanAction extends ResourceAction {
    private final UIFacade myUIFacade;

    private final ResourceContext myContext;

    private static final String ICON_URL = "icons/delete_16.gif";

    private final int MENU_MASK = Toolkit.getDefaultToolkit()
            .getMenuShortcutKeyMask();

    private GanttProject myProjectFrame;

	public DeleteHumanAction(HumanResourceManager hrManager,
            ResourceContext context, GanttProject projectFrame, UIFacade uiFacade) {
        super(hrManager);
        myUIFacade = uiFacade;
        myProjectFrame = projectFrame;
        this.putValue(AbstractAction.NAME, GanttProject
                .correctLabel(getLanguage().getText("deleteHuman")));
        this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                KeyEvent.VK_J, MENU_MASK));
        URL iconUrl = this.getClass().getClassLoader().getResource(ICON_URL);
        if (iconUrl != null) {
            this.putValue(Action.SMALL_ICON, new ImageIcon(iconUrl));
        }
        myContext = context;
    }

	public void actionPerformed(ActionEvent event) {
		final HumanResource[] context = getContext().getResources();
		if (context.length > 0) {
			final String message = getLanguage().getText("msg6") + " "
					+ getDisplayName(context) + "?";
			final String title = getLanguage().getText("question");
			Choice choice = myUIFacade.showConfirmationDialog(message, title);
			if (choice == Choice.YES) {
				myUIFacade.getUndoManager().undoableEdit("Resource removed",
						new Runnable() {
							public void run() {
								deleteResources(context);
								getProjectFrame().repaint2();
							}
						});
			}
		}
	}

    private GanttProject getProjectFrame() {
        return myProjectFrame;
    }

    private void deleteResources(HumanResource[] resources) {
        for (int i = 0; i < resources.length; i++) {
            resources[i].delete();
        }
    }

    private String getDisplayName(HumanResource[] resources) {
        if (resources.length == 1) {
            return resources[0].toString();
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < resources.length; i++) {
            result.append(resources[i].toString());
            if (i < resources.length - 1) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    private ResourceContext getContext() {
        return myContext;
    }
}
