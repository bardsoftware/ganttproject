package net.sourceforge.ganttproject.action;

import javax.swing.AbstractAction;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;

/**
 * Special action for resources
 */
abstract class ResourceAction extends AbstractAction {
    private HumanResourceManager myManager;

    public ResourceAction(HumanResourceManager hrManager) {
        myManager = hrManager;
    }

    protected HumanResourceManager getManager() {
        return myManager;
    }

    protected GanttLanguage getLanguage() {
        return GanttLanguage.getInstance();
    }
}
