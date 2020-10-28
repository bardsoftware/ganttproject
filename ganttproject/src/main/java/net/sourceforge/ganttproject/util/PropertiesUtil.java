/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.util;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import net.sourceforge.ganttproject.GPLogger;

import org.eclipse.core.runtime.Platform;

public class PropertiesUtil {
  public static void loadProperties(Properties result, String resource) {
    URL url = PropertiesUtil.class.getResource(resource);
    if (url == null) {
      return;
    }
    URL resolvedUrl;
    try {
      resolvedUrl = Platform.resolve(url);
      result.load(resolvedUrl.openStream());
    } catch (IOException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
    }
  }

}
