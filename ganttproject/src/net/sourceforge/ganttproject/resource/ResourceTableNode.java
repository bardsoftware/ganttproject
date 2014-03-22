package net.sourceforge.ganttproject.resource;

import java.util.Set;

import net.sourceforge.ganttproject.ResourceDefaultColumn;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

public abstract class ResourceTableNode extends DefaultMutableTreeTableNode {
  private final Set<ResourceDefaultColumn> myColumns;

  protected ResourceTableNode(Object userObject, Set<ResourceDefaultColumn> applicableColumns) {
    super(userObject);
    myColumns = applicableColumns;
  }

  public boolean isEditable(ResourceDefaultColumn column) {
    return myColumns.contains(column) && column.isEditable();
  }
}
