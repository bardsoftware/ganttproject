/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject;

import biz.ganttproject.LoggerApi;
import biz.ganttproject.LoggingKt;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import net.sourceforge.ganttproject.gui.UIFacade;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GPLogger {
  //private static final Logger ourLogger = Logger.getLogger("net.sourceforge.ganttproject");
  private static UIFacade ourUIFacade;
  private static final Map<String, Logger> ourLoggers = new HashMap<>();
  private static String ourLogFileName;
  private static PrintStream ourStderr;
  private static final Map<String, LoggerApi> ourLoggersApi = new HashMap<>();

  static {
//    ourHandler = new ConsoleHandler();
//    ourLogger.addHandler(ourHandler);
//    ourLogger.setLevel(Level.ALL);
//    ourHandler.setFormatter(new java.util.logging.SimpleFormatter());
    init();
  }

  public static void init() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  public static boolean log(Throwable e) {
    //e.printStackTrace();
    if (ourUIFacade != null) {
      if (e instanceof NullPointerException) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        if ("initiateToolTip".equals(stackTrace[0].getMethodName()) && "javax.swing.ToolTipManager".equals(stackTrace[0].getClassName())) {
        	// We will not show that stupid NPEs from TooltipManager
        	return true;
        }
      }
      try {
        ourUIFacade.showErrorDialog(e);
      } catch (Throwable t) {
        return false;
      }
    }
    return logToLogger(e);
  }

  public static boolean logToLogger(String message) {
    create("App").warn(message, new Object[0], null);
    return true;
  }

  public static boolean logToLogger(Throwable e) {
    var msg = e.getMessage();
    create("App").warn(msg == null ? "" : msg, new Object[0], e);
    return true;
  }

  public static void log(String message) {
    create("App").info(message);
  }

  public static LoggerApi<org.slf4j.Logger> create(String name) {
    return ourLoggersApi.computeIfAbsent(name, LoggingKt::createLogger);
  }

  public static Logger getLogger(String name) {
    Logger logger = ourLoggers.get(name);
    if (logger == null) {
      logger = Logger.getLogger(name);
//      logger.addHandler(ourHandler);
      ourLoggers.put(name, logger);
    }
    return logger;
  }

  public static Logger getLogger(Class<?> clazz) {
    return getLogger(clazz.getName());
  }

  public static void setUIFacade(UIFacade uifacade) {
    ourUIFacade = uifacade;
  }

  public static void debug(Logger logger, String format, Object... args) {
    if (logger.isLoggable(Level.FINE)) {
      logger.fine(String.format(format, args));
    }
  }

  public static void printLogLocation() {
    (ourStderr == null ? System.err : ourStderr).printf("The log was written to %s%n", ourLogFileName);
  }

  public static String getLogFile() {
    return ourLogFileName;
  }

  public static void setLogFile(String logFileName) {
    try {
      ourLogFileName = logFileName;
      System.setProperty("log_path", logFileName);
      LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
      lc.putProperty("log_path", logFileName);
      ContextInitializer ci = new ContextInitializer(lc);
      lc.reset();
      try {
        ci.autoConfig();
      } catch (JoranException e) {
        // StatusPrinter will try to log this
        e.printStackTrace();
      }
      StatusPrinter.printInCaseOfErrorsOrWarnings(lc);

//      Handler fileHandler = new FileHandler(logFileName, true);
//      fileHandler.setFormatter(new java.util.logging.SimpleFormatter());
//      ourLogger.removeHandler(ourHandler);
//      ourLogger.addHandler(fileHandler);
//      ourHandler = fileHandler;
//      ourLogFileName = logFileName;
      var logFile = new File(logFileName);
      ourStderr = System.err;
      System.setErr(new PrintStream(new FileOutputStream(logFile)));
//      ourLoggers.values().forEach(logger -> {
//        logger.removeHandler(ourHandler);
//        logger.addHandler(fileHandler);
//      });
//
//
//      ourLogbackAppender = new FileAppender();
//      ourLogbackAppender.setFile(logFileName);
//
//      ((AppenderAttachable<ILoggingEvent>) LoggerFactory.getLogger("ROOT")).addAppender(ourLogbackAppender);
//      for (LoggerImpl pendingLogger : ourPendingLoggers) {
//        ((AppenderAttachable<ILoggingEvent>) pendingLogger.delegate()).addAppender(ourLogbackAppender);
//      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public static String readLog() {
    if (ourLogFileName != null) {
      //ourHandler.flush();
      File f = new File(ourLogFileName);
      try {
        if (!f.exists()) {
          return "Log file not found at " + f.getAbsolutePath()
              + " Check that you have appropriate access permissions for writing and reading this file";
        }
        if (!f.canRead()) {
          return "Can't read log file. Try reading it manually from " + f.getAbsolutePath();
        }
        BufferedReader reader = new BufferedReader(new FileReader(f));
        StringBuilder buffer = new StringBuilder(f.getAbsolutePath());
        buffer.append("\n\n");
        for (String s = reader.readLine(); s != null; s = reader.readLine()) {
          buffer.append(s).append("\n");
        }
        return buffer.toString();
      } catch (FileNotFoundException e) {
        log(e);
        return "Log file not found at " + f.getAbsolutePath()
            + " Check that you have appropriate access permissions for writing and reading this file";
      } catch (IOException e) {
        log(e);
        return "Can't read log file. Try reading it manually from " + f.getAbsolutePath();
      }
    }
    return "Log to file has not been configured, sorry. If you started GanttProject from console, try looking there";
  }

  private static final String[] SYSTEM_PROPERTIES = new String[] { "java.class.path", "java.home", "java.ext.dirs", "java.io.tmpdir",
      "java.runtime.version", "java.vendor", "java.vm.name", "java.vm.vendor", "java.vm.version", "os.arch", "os.name",
      "os.version", "sun.java.command", "user.country", "user.dir", "user.home", "user.language", "user.timezone" };

  public static void logSystemInformation() {
    StringBuilder result = new StringBuilder();
    result.append("GanttProject ").append(GPVersion.getCurrentVersionNumber()).append("\n");
    File optionsFile = GanttOptions.getOptionsFile();
    result.append("Settings file:\n");
    result.append("\tlocation: ").append(optionsFile.getAbsolutePath()).append("\n");
    result.append("\tsize:").append(optionsFile.length()).append("\n");
    result.append("\tis readable: ").append(optionsFile.canRead()).append("\n");
    for (String name : SYSTEM_PROPERTIES) {
      result.append(name).append(": ").append(System.getProperty(name)).append("\n");
    }

    System.err.println(result);
  }

  public static void close() {
//    ourHandler.flush();
  }
}
