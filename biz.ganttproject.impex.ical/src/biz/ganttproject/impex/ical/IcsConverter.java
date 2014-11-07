/*
Copyright 2012 GanttProject Team

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
package biz.ganttproject.impex.ical;

import java.io.FileReader;
import java.util.List;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * Converts ics file to GanttProject calendar format.
 * 
 * @author Dmitry Barashev
 */
public class IcsConverter {
  public static class Args {
    @Parameter(description = "Input file name", required = true)
    public List<String> file = null;
  }

  public static void main(String[] args) throws Exception {
    Args mainArgs = new Args();
    try {
      new JCommander(new Object[] { mainArgs }, args);
    } catch (Throwable e) {
      e.printStackTrace();
      return;
    }
    if (mainArgs.file.size() != 1) {
      System.err.println("Only 1 input file should be specified");
      return;
    }
    CalendarBuilder builder = new CalendarBuilder();
    Calendar c = builder.build(new UnfoldingReader(new FileReader(mainArgs.file.get(0))));
    for (Component comp : (List<Component>)c.getComponents()) {
      System.err.println(comp);
    }
  }
}
