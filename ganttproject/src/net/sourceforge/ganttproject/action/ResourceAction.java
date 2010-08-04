package net.sourceforge.ganttproject.action;

import javax.swing.AbstractAction;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.ResourceManager;

/**
 * Special action for resources
 */
abstract class ResourceAction extends AbstractAction {
    public ResourceAction(ResourceManager hrManager) {
        myManager = hrManager;
    }

    protected ResourceManager getManager() {
        return myManager;
    }

    protected GanttLanguage getLanguage() {
        return GanttLanguage.getInstance();
    }

    private ResourceManager myManager;
}
