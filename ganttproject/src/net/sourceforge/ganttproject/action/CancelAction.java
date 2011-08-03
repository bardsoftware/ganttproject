/*
 * Created on 27.03.2005
 */
package net.sourceforge.ganttproject.action;

/**
 * @author bard
 */
public abstract class CancelAction extends GPAction {
    public CancelAction() {
        this("cancel");
    }
    protected CancelAction(String key) {
        super(key);
    }
}
