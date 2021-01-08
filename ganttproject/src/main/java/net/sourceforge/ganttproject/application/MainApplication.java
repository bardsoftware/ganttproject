/*
 * Created on 25.04.2005
 */
package net.sourceforge.ganttproject.application;

import biz.ganttproject.LoggerApi;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttProject;
import org.eclipse.core.runtime.IPlatformRunnable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author bard
 */
public class MainApplication implements IPlatformRunnable {
  private AtomicBoolean myLock = new AtomicBoolean(true);
  private final LoggerApi logger = GPLogger.create("Window");

  // The hack with waiting is necessary because when you
  // launch Runtime Workbench in Eclipse, it exists as soon as
  // GanttProject.main() method exits
  // without Eclipse, Swing thread continues execution. So we wait until main
  // window closes
  @Override
  public Object run(Object args) throws Exception {
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    String[] cmdLine = (String[]) args;
    Consumer<Boolean> onApplicationQuit = new Consumer<Boolean>() {
      public void accept(Boolean withSystemExit) {
        synchronized(myLock) {
          myLock.set(withSystemExit);
          myLock.notify();
        }
      }
    };
    GanttProject.setApplicationQuitCallback(onApplicationQuit);
    if (GanttProject.main(cmdLine)) {
      synchronized (myLock) {
        logger.debug("Waiting until main window closes");
        myLock.wait();
        logger.debug("Main window has closed");
      }
    }
    logger.debug("Program terminated");
    GPLogger.close();
    if (myLock.get()) {
      System.exit(0);
    }
    return null;
  }

}
