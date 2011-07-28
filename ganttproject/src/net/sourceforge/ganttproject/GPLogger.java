/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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
    private static Map<Class<?>, Logger> ourClass_Logger = new HashMap<Class<?>, Logger>();

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

	public static Logger getLogger(Class<?> clazz) {
        Logger logger = ourClass_Logger.get(clazz);
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
