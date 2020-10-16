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
package biz.ganttproject.core.option;

/**
 * Event that discribes a change in a value.
 * 
 * @author bbaranne
 * 
 */
public class ChangeValueEvent {

  private final Object myID;

  private final Object myOldValue;

  private final Object myNewValue;

  private final Object myTriggerID;

  public ChangeValueEvent(Object id, Object oldValue, Object newValue, Object triggerId) {
    myID = id;
    myOldValue = oldValue;
    myNewValue = newValue;
    myTriggerID = triggerId;
  }

  public Object getID() {
    return myID;
  }

  public Object getOldValue() {
    return myOldValue;
  }

  public Object getNewValue() {
    return myNewValue;
  }

  public Object getTriggerID() {
    return myTriggerID;
  }
  @Override
  public String toString() {
    return "[id:" + myID + ", old:" + myOldValue + ", new: " + myNewValue + "]";
  }
}
