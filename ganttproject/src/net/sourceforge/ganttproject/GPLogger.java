package net.sourceforge.ganttproject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.sourceforge.ganttproject.gui.UIFacade;


public class GPLogger {
	private static Logger ourLogger = Logger.getLogger("org.ganttproject");
	private static Handler ourHandler;
	private static UIFacade ourUIFacade;
    private static Map ourClass_Logger = new HashMap();

	static {
        ourHandler = new ConsoleHandler();
		ourLogger.addHandler(ourHandler);
		ourLogger.setLevel(Level.ALL);
		ourHandler.setFormatter(new java.util.logging.SimpleFormatter());

	}

	public static boolean log(Throwable e) {
		if (ourHandler == null) {
			return false;
		}
		ourLogger.log(Level.WARNING, e.getMessage(), e);
		if (ourUIFacade != null) {
			ourUIFacade.logErrorMessage(e);
		}
		return true;
	}

	public static void log(String message) {
		ourLogger.log(Level.INFO, message);
	}

	public static Logger getLogger(Object o) {
	    assert o!=null;
	    return getLogger(o.getClass());
	}

	public static Logger getLogger(Class clazz) {
        Logger logger = (Logger) ourClass_Logger.get(clazz);
        if (logger == null) {
            logger = Logger.getLogger(clazz.getName());
            logger.addHandler(ourHandler);
            ourClass_Logger.put(clazz, logger);
        }
        return logger;
	}
	public static void setUIFacade(UIFacade uifacade) {
		ourUIFacade = uifacade;
	}

    public static void setLogFile(String logFileName) {
        try {
            Handler fileHandler = new FileHandler(logFileName, true);
            fileHandler.setFormatter(new java.util.logging.SimpleFormatter());
            ourLogger.removeHandler(ourHandler);
            ourLogger.addHandler(fileHandler);
            ourHandler = fileHandler;
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
}
