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
package net.sourceforge.ganttproject.io;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import net.sourceforge.ganttproject.GanttPreviousState;
import net.sourceforge.ganttproject.GanttPreviousStateTask;

public class HistorySaver extends SaverBase {

  void save(List<GanttPreviousState> history, TransformerHandler handler) throws SAXException,
      ParserConfigurationException, IOException {
    startElement("previous", handler);
    for (GanttPreviousState baseline : history) {
      saveBaseline(baseline, handler);
    }
    endElement("previous", handler);
  }

  public void saveBaseline(GanttPreviousState nextState, TransformerHandler handler) throws SAXException {
    saveBaseline(nextState.getName(), nextState.load(), handler);
  }

  public void saveBaseline(String name, List<GanttPreviousStateTask> tasks, TransformerHandler handler)
      throws SAXException {
    AttributesImpl attrs = new AttributesImpl();
    addAttribute("name", name, attrs);
    startElement("previous-tasks", attrs, handler);
    for (GanttPreviousStateTask task : tasks) {
      addAttribute("id", task.getId(), attrs);
      addAttribute("start", task.getStart().toXMLString(), attrs);
      addAttribute("duration", task.getDuration(), attrs);
      addAttribute("meeting", task.isMilestone(), attrs);
      addAttribute("super", task.hasNested(), attrs);
      emptyElement("previous-task", attrs, handler);
    }
    endElement("previous-tasks", handler);

  }
}
