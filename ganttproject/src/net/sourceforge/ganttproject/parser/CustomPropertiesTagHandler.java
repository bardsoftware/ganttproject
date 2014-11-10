/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.task.CustomColumnsException;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;
import org.xml.sax.Attributes;

import biz.ganttproject.core.time.CalendarFactory;

/**
 * @author bbaranne Mar 10, 2005
 */
public class CustomPropertiesTagHandler extends AbstractTagHandler implements ParsingListener {
  private TaskManager taskManager = null;

  private ParsingContext parsingContext = null;

  private List<CustomPropertiesStructure> listStructure = null;

  public CustomPropertiesTagHandler(ParsingContext context, TaskManager taskManager) {
    super("customproperty");
    this.taskManager = taskManager;
    this.parsingContext = context;
    this.listStructure = new ArrayList<CustomPropertiesStructure>();
  }

  @Override
  protected boolean onStartElement(Attributes attrs) {
    loadProperty(attrs);
    return true;
  }

  /**
   * @see net.sourceforge.ganttproject.parser.ParsingListener#parsingStarted()
   */
  @Override
  public void parsingStarted() {
    // nothing to do.
  }

  /**
   * @see net.sourceforge.ganttproject.parser.ParsingListener#parsingFinished()
   */
  @Override
  public void parsingFinished() {
    Iterator<CustomPropertiesStructure> it = this.listStructure.iterator();

    while (it.hasNext()) {
      CustomPropertiesStructure cps = it.next();
      Task task = taskManager.getTask(cps.taskID);
      CustomPropertyDefinition cc = taskManager.getCustomPropertyManager().getCustomPropertyDefinition(
          cps.taskPropertyID);
      String valueStr = cps.value;
      Object value = null;
      Class<?> cla = cc.getType();

      if (valueStr != null) {
        if (cla.equals(String.class)) {
          value = valueStr.toString();
        } else if (cla.equals(Boolean.class)) {
          value = Boolean.valueOf(valueStr);
        } else if (cla.equals(Integer.class)) {
          value = Integer.valueOf(valueStr);
        } else if (cla.equals(Double.class)) {
          value = Double.valueOf(valueStr);
        } else if (GregorianCalendar.class.isAssignableFrom(cla)) {
          try {
            value = CalendarFactory.createGanttCalendar(DateParser.parse(valueStr));
          } catch (InvalidDateException e) {
            if (!GPLogger.log(e)) {
              e.printStackTrace(System.err);
            }
          }
        }
      }
      try {
        // System.out.println(task.getName());
        task.getCustomValues().setValue(cc, value);
      } catch (CustomColumnsException e) {
        if (!GPLogger.log(e)) {
          e.printStackTrace(System.err);
        }
      }
    }
  }

  private void loadProperty(Attributes attrs) {
    if (attrs != null) {
      // System.out.println(parsingContext.getTaskID());
      CustomPropertiesStructure cps = new CustomPropertiesStructure();
      cps.setTaskID(this.parsingContext.peekTask().getTaskID());
      cps.setTaskPropertyID(attrs.getValue("taskproperty-id"));
      cps.setValue(attrs.getValue("value"));

      this.listStructure.add(cps);
    }
  }

  private class CustomPropertiesStructure {
    public int taskID;

    public String taskPropertyID = null;

    public String value = null;

    public CustomPropertiesStructure() {
    }

    public void setTaskID(int taskID) {
      this.taskID = taskID;
    }

    public void setTaskPropertyID(String propertyID) {
      this.taskPropertyID = propertyID;
    }

    public void setValue(String val) {
      this.value = val;
    }
  }
}
