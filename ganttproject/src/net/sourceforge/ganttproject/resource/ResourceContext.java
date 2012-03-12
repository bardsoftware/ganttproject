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
package net.sourceforge.ganttproject.resource;

/**
 * This interface represents a kind of 'selection' in the application It is
 * designed for collaboration between actions which do something with selected
 * resources and UI components which implement UI specifics of selection
 * management (e.g. listen mouse events); This interface may be implemented,
 * e.g., by tables, trees and other UI components which allow to select
 * something
 * 
 * @author dbarashev
 */
public interface ResourceContext {
  /** @return Resources selected at the moment */
  public HumanResource[] getResources();
}
