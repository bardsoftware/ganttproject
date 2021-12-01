/*
 * Created on 25.04.2005
 */
package net.sourceforge.ganttproject.application;

import biz.ganttproject.LoggerApi;
import kotlin.Unit;
import net.sourceforge.ganttproject.AppBuilder;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GPVersion;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.document.DocumentCreator;
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

    var appBuilder = new AppBuilder(cmdLine);
    if (appBuilder.getMainArgs().help) {
      appBuilder.getCliParser().usage();
      System.exit(0);
    }
    if (appBuilder.getMainArgs().version) {
      System.out.println(GPVersion.getCurrentVersionNumber());
      System.exit(0);
    }

    appBuilder.withLogging();
    if (!appBuilder.isCli()) {
      appBuilder.withSplash();
      appBuilder.withWindowVisible();
      appBuilder.whenWindowOpened(frame -> {
        DocumentCreator.createAutosaveCleanup().run();
        return Unit.INSTANCE;
      });
    } else {

    }


    Consumer<Boolean> onApplicationQuit = withSystemExit -> {
      synchronized(myLock) {
        myLock.set(withSystemExit);
        myLock.notify();
      }
    };
    GanttProject.setApplicationQuitCallback(onApplicationQuit);
    appBuilder.launch();
    synchronized (myLock) {
      logger.debug("Waiting until main window closes");
      myLock.wait();
      logger.debug("Main window has closed");
    }
    logger.debug("Program terminated");
    GPLogger.close();
    if (myLock.get()) {
      System.exit(0);
    }
    return null;
  }

}
