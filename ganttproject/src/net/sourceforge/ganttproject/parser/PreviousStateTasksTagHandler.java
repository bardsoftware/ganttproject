/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttPreviousState;
import net.sourceforge.ganttproject.GanttPreviousStateTask;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import biz.ganttproject.core.time.GanttCalendar;

/**
 * @author nbohn
 */
public class PreviousStateTasksTagHandler extends DefaultHandler implements TagHandler {
  private String myName = "";

  private GanttPreviousState previousState;

  private final List<GanttPreviousState> myPreviousStates;

  private ArrayList<GanttPreviousStateTask> tasks = new ArrayList<GanttPreviousStateTask>();

  public PreviousStateTasksTagHandler() {
    this(null);
  }

  public PreviousStateTasksTagHandler(List<GanttPreviousState> previousStates) {
    myPreviousStates = previousStates;
  }

  @Override
  public void startElement(String namespaceURI, String sName, String qName, Attributes attrs) {
    if (qName.equals("previous-tasks")) {
      setName(attrs.getValue("name"));
      tasks = new ArrayList<GanttPreviousStateTask>();
    } else if (qName.equals("previous-task")) {
      loadPreviousTask(attrs);
    }
  }

  @Override
  public void endElement(String namespaceURI, String sName, String qName) {
    if (qName.equals("previous-tasks") && myPreviousStates != null) {
      try {
        previousState = new GanttPreviousState(myName, tasks);
        previousState.init();
        previousState.saveFile();
        myPreviousStates.add(previousState);
      } catch (IOException e) {
        if (!GPLogger.log(e)) {
          e.printStackTrace(System.err);
        }
      }
    }
  }

  private void setName(String name) {
    myName = name;
  }

  private void loadPreviousTask(Attributes attrs) {

    String id = attrs.getValue("id");

    boolean meeting = Boolean.parseBoolean(attrs.getValue("meeting"));

    String start = attrs.getValue("start");

    String duration = attrs.getValue("duration");

    boolean nested = Boolean.parseBoolean(attrs.getValue("super"));

    GanttPreviousStateTask task = new GanttPreviousStateTask(new Integer(id).intValue(),
        GanttCalendar.parseXMLDate(start), new Integer(duration).intValue(), meeting, nested);
    tasks.add(task);
  }

  public String getName() {
    return myName;
  }

  public ArrayList<GanttPreviousStateTask> getTasks() {
    return tasks;
  }

  public boolean hasCdata() {
    return false;
  }

  public void appendCdata(String cdata) {
  }
}
