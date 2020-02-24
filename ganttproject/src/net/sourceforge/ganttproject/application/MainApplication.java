/*
 * Created on 25.04.2005
 */
package net.sourceforge.ganttproject.application;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttProject;

import org.eclipse.core.runtime.IPlatformRunnable;

/**
 * @author bard
 */
public class MainApplication implements IPlatformRunnable {
  private Object myLock = new Object();

  // The hack with waiting is necessary because when you
  // launch Runtime Workbench in Eclipse, it exists as soon as
  // GanttProject.main() method exits
  // without Eclipse, Swing thread continues execution. So we wait until main
  // window closes
  @Override
  public Object run(Object args) throws Exception {
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    String[] cmdLine = (String[]) args;
    Runnable onApplicationQuit = new Runnable() {
      public void run() {
        synchronized(myLock) {
          myLock.notify();
        }
      }
    };
    GanttProject.setApplicationQuitCallback(onApplicationQuit);
    if (GanttProject.main(cmdLine)) {
      synchronized (myLock) {
        GPLogger.log("Waiting until main window closes");
        myLock.wait();
        GPLogger.log("Main window has closed");
      }
    }
    GPLogger.log("Program terminated");
    GPLogger.close();
    System.exit(0);
    return null;
  }

}
