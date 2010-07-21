/*
 * Created on 03.10.2005
 */
package net.sourceforge.ganttproject;

public interface ProjectEventListener {
    void projectModified();
    void projectSaved();
    void projectClosed();
}
