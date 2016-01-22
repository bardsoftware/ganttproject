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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessControlException;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

import net.sourceforge.ganttproject.gui.UIFacade;

public class GPLogger {
  private static final Logger ourLogger = Logger.getLogger("net.sourceforge.ganttproject");
  private static Handler ourHandler;
  private static UIFacade ourUIFacade;
  private static Map<String, Logger> ourLoggers = Maps.newHashMap();
  private static String ourLogFileName;

  static {
    ourHandler = new ConsoleHandler();
    ourLogger.addHandler(ourHandler);
    ourLogger.setLevel(Level.ALL);
    ourHandler.setFormatter(new java.util.logging.SimpleFormatter());
  }

  public static boolean log(Throwable e) {
    if (ourUIFacade != null) {
      if (e instanceof NullPointerException) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        if ("initiateToolTip".equals(stackTrace[0].getMethodName()) && "javax.swing.ToolTipManager".equals(stackTrace[0].getClassName())) {
        	// We will not show that stupid NPEs from TooltipManager
        	return true;
        }
      }
      ourUIFacade.showErrorDialog(e);
      return true;
    }
    return logToLogger(e);
  }

  public static boolean logToLogger(String message) {
    if (ourHandler == null) {
      return false;
    }
    ourLogger.log(Level.WARNING, message);
    return true;
  }

  public static boolean logToLogger(Throwable e) {
    if (ourHandler == null) {
      return false;
    }
    ourLogger.log(Level.WARNING, e.getMessage(), e);
    return true;
  }

  public static void log(String message) {
    ourLogger.log(Level.INFO, message);
  }

  public static Logger getLogger(Object o) {
    assert o != null;
    return getLogger(o.getClass());
  }

  public static Logger getLogger(String name) {
    Logger logger = ourLoggers.get(name);
    if (logger == null) {
      logger = Logger.getLogger(name);
      logger.addHandler(ourHandler);
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

  public static void setLogFile(String logFileName) {
    try {
      Handler fileHandler = new FileHandler(logFileName, true);
      fileHandler.setFormatter(new java.util.logging.SimpleFormatter());
      ourLogger.removeHandler(ourHandler);
      ourLogger.addHandler(fileHandler);
      ourHandler = fileHandler;
      ourLogFileName = logFileName;
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void readConfiguration(URL configuration) throws IOException {
    InputStream input = configuration.openStream();
    LogManager.getLogManager().readConfiguration(input);
  }

  public static String readLog() {
    if (ourLogFileName != null) {
      ourHandler.flush();
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

  private static String[] SYSTEM_PROPERTIES = new String[] { "java.class.path", "java.home", "java.ext.dirs", "java.io.tmpdir",
      "java.runtime.version", "java.vendor", "java.vm.name", "java.vm.vendor", "java.vm.version", "os.arch", "os.name",
      "os.version", "sun.java.command", "user.country", "user.dir", "user.home", "user.language", "user.timezone" };

  public static void logSystemInformation() {
    try {
      StringBuilder result = new StringBuilder();
      result.append("GanttProject " + GPVersion.CURRENT).append("\n");
      File optionsFile = GanttOptions.getOptionsFile();
      result.append("Settings file:\n");
      result.append("\tlocation: ").append(optionsFile.getAbsolutePath()).append("\n");
      result.append("\tsize:").append(optionsFile.length()).append("\n");
      result.append("\tis readable: ").append(optionsFile.canRead()).append("\n");
      for (String name : SYSTEM_PROPERTIES) {
        result.append(name).append(": ").append(System.getProperty(name)).append("\n");
      }

      System.err.println(result.toString());
    } catch (AccessControlException e) {
      // This can happen when running in a sandbox (Java WebStart)
      System.err.println(e + ": " + e.getMessage());
    }

  }

  public static void close() {
    ourHandler.flush();
  }
}
