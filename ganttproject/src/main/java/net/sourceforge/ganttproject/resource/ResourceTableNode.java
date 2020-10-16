/*
Copyright 2014 BarD Software s.r.o

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

import java.util.Set;

import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.ResourceDefaultColumn;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

/**
 * Common superclass for the resource table nodes.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public abstract class ResourceTableNode extends DefaultMutableTreeTableNode {
  private final Set<ResourceDefaultColumn> myColumns;

  protected ResourceTableNode(Object userObject, Set<ResourceDefaultColumn> applicableColumns) {
    super(userObject);
    myColumns = applicableColumns;
  }

  public boolean isEditable(ResourceDefaultColumn column) {
    return myColumns.contains(column) && column.isEditable();
  }

  public abstract void setCustomField(CustomPropertyDefinition def, Object val);
  public abstract Object getCustomField(CustomPropertyDefinition def);

  public abstract Object getStandardField(ResourceDefaultColumn def);
  public abstract void setStandardField(ResourceDefaultColumn resourceDefaultColumn, Object value);
}
