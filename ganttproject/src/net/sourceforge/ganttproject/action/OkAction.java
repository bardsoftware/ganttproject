/*
 * Created on 27.03.2005
 */
package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author bard
 */
public abstract class OkAction extends AbstractAction {
    public OkAction() {
        super(GanttLanguage.getInstance().getText("ok"));
    }
    public static OkAction EMPTY = new OkAction() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
        }
    };
}
