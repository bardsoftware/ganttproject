package net.sourceforge.ganttproject.gui.taskproperties;

/**
 * Created by IntelliJ IDEA. User: Administrator Date: 07.10.2004 Time: 2:44:24
 * To change this template use Options | File Templates.
 */
public interface InternalStateListener {
    void nameChanged(String newName);

    void durationChanged(int newDuration);
}
