/*
 * Created on 25.04.2005
 */
package net.sourceforge.ganttproject.application;

import biz.ganttproject.LoggerApi;
import biz.ganttproject.app.InternationalizationKt;
import biz.ganttproject.storage.AutoSaveKt;
import kotlin.Unit;
import net.sourceforge.ganttproject.AppBuilder;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GPVersion;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.document.DocumentCreator;
import net.sourceforge.ganttproject.export.CommandLineExportApplication;
import org.eclipse.core.runtime.IPlatformRunnable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author bard
 */
public class MainApplication implements IPlatformRunnable {
  private final AtomicBoolean myLock = new AtomicBoolean(true);
  private final LoggerApi<?> logger = GPLogger.create("Window");

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
    try {
      Class.forName("javafx.application.Platform");
    } catch (Exception e) {
      var msg = String.format(
          "GanttProject requires Java Runtime with JavaFX. You are using %s %s %s. " +
              "JavaFX is available in BellSoft Liberica or Azul Zulu Java Runtime.",
          System.getProperty("java.vm.vendor"),
          System.getProperty("java.vm.name"),
          System.getProperty("java.vm.version"));
      System.err.println(msg);
      JOptionPane.showMessageDialog(null, msg, "Inappropriate Java Runtime", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
    if (!appBuilder.isCli()) {
      appBuilder.withSplash();
      appBuilder.withWindowVisible();
      appBuilder.whenWindowOpened(frame -> {
        AutoSaveKt.createAutosaveCleanup().run();
        return Unit.INSTANCE;
      });
      if (appBuilder.getMainArgs().fixMenuBarTitle) {
        appBuilder.runBeforeUi(() -> {
          try {
            var toolkit = Toolkit.getDefaultToolkit();
            var awtAppClassNameField = toolkit.getClass().getDeclaredField("awtAppClassName");
            awtAppClassNameField.setAccessible(true);
            awtAppClassNameField.set(toolkit, InternationalizationKt.getRootLocalizer().formatText("appliTitle"));
          } catch (NoSuchFieldException | IllegalAccessException ex) {
            System.err.println("Can't set awtAppClassName (needed on Linux to show app name in the top panel)");
          }
          return Unit.INSTANCE;
        });
      }
    } else {
      appBuilder.whenDocumentReady(project -> {
        var executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
          var cliApp = new CommandLineExportApplication();
          cliApp.export(appBuilder.getCliArgs(), project, ((GanttProject) project).getUIFacade());
          GanttProject.doQuitApplication(true);
        });
        return Unit.INSTANCE;
      });
    }
    var files = appBuilder.getMainArgs().file;
    if (files != null && !files.isEmpty()) {
      appBuilder.withDocument(files.get(0));
    }


    Consumer<Boolean> onApplicationQuit = withSystemExit -> {
      synchronized(myLock) {
        myLock.set(withSystemExit);
        myLock.notify();
      }
    };
    GanttProject.setApplicationQuitCallback(onApplicationQuit);
    appBuilder.launch();
    try {
      synchronized (myLock) {
        logger.debug("Waiting until main window closes");
        myLock.wait();
        logger.debug("Main window has closed");
      }
    } catch (InterruptedException ex) {
      ex.printStackTrace();
    }
    logger.debug("Program terminated");
    GPLogger.close();
    if (myLock.get()) {
      System.exit(0);
    }
    return null;
  }

}
