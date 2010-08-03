/*
 * Created on 27.03.2005
 */
package net.sourceforge.ganttproject.action;

import javax.swing.AbstractAction;

import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author bard
 */
public abstract class CancelAction extends AbstractAction {
    public CancelAction() {
        super(GanttLanguage.getInstance().getText("cancel"));
    }
}
