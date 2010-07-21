/*
 * Created on 27.03.2005
 */
package net.sourceforge.ganttproject.action;

import javax.swing.AbstractAction;

import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author bard
 */
public abstract class OkAction extends AbstractAction {
    public OkAction() {
        super(GanttLanguage.getInstance().getText("ok"));
    }

}
