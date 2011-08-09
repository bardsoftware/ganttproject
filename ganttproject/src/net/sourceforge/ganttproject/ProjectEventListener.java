/*
 * Created on 03.10.2005
 */
package net.sourceforge.ganttproject;

public interface ProjectEventListener {
    void projectModified();
    void projectSaved();
    void projectClosed();
    void projectOpened();

    class Stub implements ProjectEventListener {
        public void projectModified() {
        }
        public void projectSaved() {
        }
        public void projectClosed() {
        }
        public void projectOpened() {
        }
    }
}
