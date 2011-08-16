package net.sourceforge.ganttproject.gui.options.model;

/**
 * The classes that implements this interface should dispatch all
 * <code>ChangeValueEvent</code> to their <code>ChangeValueListener</code>.
 *
 * @author bbaranne
 *
 */
public interface ChangeValueDispatcher {
    public void addChangeValueListener(ChangeValueListener listener);

    //public void fireChangeValueEvent(ChangeValueEvent event);
}
