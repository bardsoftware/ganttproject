/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.task;

import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.Role;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

class ResourceAssignmentCollectionImpl implements ResourceAssignmentCollection {
  private final Map<HumanResource, ResourceAssignment> myAssignments = new LinkedHashMap<HumanResource, ResourceAssignment>();

  private final TaskImpl myTask;

  private Supplier<HumanResourceManager> myResourceManager;

  public ResourceAssignmentCollectionImpl(TaskImpl task, Supplier<HumanResourceManager> resourceManager) {
    myTask = task;
    myResourceManager = resourceManager;
  }

  private ResourceAssignmentCollectionImpl(ResourceAssignmentCollectionImpl collection) {
    myTask = collection.myTask;
    ResourceAssignment[] assignments = collection.getAssignments();
    for (int i = 0; i < assignments.length; i++) {
      ResourceAssignment next = assignments[i];
      ResourceAssignment copy = new ResourceAssignmentImpl(next.getResource());
      copy.setLoad(next.getLoad());
      copy.setCoordinator(next.isCoordinator());
      copy.setBlocking(next.isBlocking());
      copy.setNoEffort(next.isNoEffort());
      copy.setRoleForAssignment(next.getRoleForAssignment());
      copy.setActualEffort(next.getActualEffort());
      addAssignment(copy);
    }
  }

  @Override
  public void clear() {
    ResourceAssignment[] assignments = getAssignments();
    for (int i = 0; i < assignments.length; i++) {
      assignments[i].delete();
    }
  }

  @Override
  public ResourceAssignment[] getAssignments() {
    return myAssignments.values().toArray(new ResourceAssignment[myAssignments.size()]);
  }

  @Override
  public ResourceAssignment getAssignment(HumanResource resource) {
    return myAssignments.get(resource);
  }

  @Override
  public ResourceAssignmentMutator createMutator() {
    return new ResourceAssignmentMutatorImpl();
  }

  ResourceAssignmentCollectionImpl copy() {
    return new ResourceAssignmentCollectionImpl(this);
  }

  @Override
  public ResourceAssignment addAssignment(HumanResource resource) {
    return auxAddAssignment(resource);
  }

  @Override
  public void deleteAssignment(HumanResource resource) {
    myAssignments.remove(resource);
    // The resource side may have been deleted and reported before this task let go of the assignment.
    HumanResourceManager manager = myResourceManager == null ? null : myResourceManager.get();
    if (manager != null) {
      manager.fireAssignmentsChanged(resource);
    }
  }

  private ResourceAssignment auxAddAssignment(HumanResource resource) {
    final ResourceAssignment result = new ResourceAssignmentImpl(resource);
    addAssignment(result);
    return result;
  }

  private void addAssignment(ResourceAssignment assignment) {
    myAssignments.put(assignment.getResource(), assignment);
  }

  /**
   * Removes the assignments related to the given resource.
   *
   * @param resource
   *          Assigned resource
   */
  public void removeAssignment(HumanResource resource) {
    new ResourceAssignmentImpl(resource).delete();
  }

  private Task getTask() {
    return myTask;
  }

  private class ResourceAssignmentImpl implements ResourceAssignment {
    private ResourceAssignment myAssignmentToResource;

    public ResourceAssignmentImpl(HumanResource resource) {
      myAssignmentToResource = resource.createAssignment(this);
      // resource.setAssignmentCollection(ResourceAssignmentCollectionImpl.this);
    }

    @Override
    public Task getTask() {
      return ResourceAssignmentCollectionImpl.this.getTask();
    }

    @Override
    public HumanResource getResource() {
      return myAssignmentToResource.getResource();
    }

    @Override
    public float getLoad() {
      return myAssignmentToResource.getLoad();
    }

    // TODO transaction
    @Override
    public void setLoad(float load) {
      myAssignmentToResource.setLoad(load);
    }

    /**
     * Deletes all the assignments and all the related assignments
     */
    @Override
    public void delete() {
      ResourceAssignmentCollectionImpl.this.deleteAssignment(getResource());
      myAssignmentToResource.delete();
    }

    @Override
    public void setCoordinator(boolean responsible) {
      myAssignmentToResource.setCoordinator(responsible);
    }

    @Override
    public boolean isCoordinator() {
      return myAssignmentToResource.isCoordinator();
    }

    @Override
    public void setBlocking(boolean blocking) {
      myAssignmentToResource.setBlocking(blocking);
    }

    @Override
    public boolean isBlocking() {
      return myAssignmentToResource.isBlocking();
    }

    @Override
    public void setNoEffort(boolean noEffort) {
      myAssignmentToResource.setNoEffort(noEffort);
    }

    @Override
    public boolean isNoEffort() {
      return myAssignmentToResource.isNoEffort();
    }

    @Override
    public Role getRoleForAssignment() {
      return myAssignmentToResource.getRoleForAssignment();
    }

    @Override
    public void setRoleForAssignment(Role role) {
      myAssignmentToResource.setRoleForAssignment(role);

    }

    @Override
    public BigDecimal getActualEffort() {
      return myAssignmentToResource.getActualEffort();
    }

    @Override
    public void setActualEffort(BigDecimal actualEffort) {
      myAssignmentToResource.setActualEffort(actualEffort);
    }

    @Override
    public String toString() {
      return this.getResource().getName() + " -> " + this.getTask().getName();
    }
  }

  private class ResourceAssignmentStub implements ResourceAssignment {
    private final HumanResource myResource;
    private final Runnable myOnDelete;

    private float myLoad;

    private boolean myCoordinator;

    private boolean myBlocking;

    private boolean myNoEffort;

    private Role myRoleForAssignment;

    private BigDecimal myActualEffort;

    public ResourceAssignmentStub(HumanResource resource, Runnable onDelete) {
      myResource = resource;
      myOnDelete = onDelete;
    }

    @Override
    public Task getTask() {
      return ResourceAssignmentCollectionImpl.this.getTask();
    }

    @Override
    public HumanResource getResource() {
      return myResource;
    }

    @Override
    public float getLoad() {
      return myLoad;
    }

    @Override
    public void setLoad(float load) {
      myLoad = load;
    }

    @Override
    public void delete() {
      myOnDelete.run();
    }

    @Override
    public void setCoordinator(boolean responsible) {
      myCoordinator = responsible;
    }

    @Override
    public boolean isCoordinator() {
      return myCoordinator;
    }

    @Override
    public void setBlocking(boolean blocking) {
      myBlocking = blocking;
    }

    @Override
    public boolean isBlocking() {
      return myBlocking;
    }

    @Override
    public void setNoEffort(boolean noEffort) {
      myNoEffort = noEffort;
    }

    @Override
    public boolean isNoEffort() {
      return myNoEffort;
    }

    @Override
    public Role getRoleForAssignment() {

      return myRoleForAssignment;
    }

    @Override
    public void setRoleForAssignment(Role role) {
      myRoleForAssignment = role;

    }

    @Override
    public BigDecimal getActualEffort() {
      return myActualEffort;
    }

    @Override
    public void setActualEffort(BigDecimal actualEffort) {
      myActualEffort = actualEffort;
    }

    @Override
    public String toString() {
      return this.getResource().getName() + " -> " + this.getTask().getName();
    }
  }

  private class ResourceAssignmentMutatorImpl implements ResourceAssignmentMutator {
    private Map<HumanResource, MutationInfo> myQueue = new HashMap<HumanResource, MutationInfo>();

    @Override
    public ResourceAssignment addAssignment(final HumanResource resource) {
      ResourceAssignment result = new ResourceAssignmentStub(resource, new Runnable() {
        @Override
        public void run() {
          myQueue.remove(resource);
        }
      });
      myQueue.put(resource, new MutationInfo(result, MutationInfo.ADD));
      return result;
    }

    @Override
    public void deleteAssignment(HumanResource resource) {
      MutationInfo info = myQueue.get(resource);
      if (info == null) {
        myQueue.put(resource, new MutationInfo(resource, MutationInfo.DELETE));
      } else if (info.myOperation == MutationInfo.ADD) {
        myQueue.remove(resource);
      }
    }

    @Override
    public void commit() {
      List<MutationInfo> mutations = new ArrayList<MutationInfo>(myQueue.values());
      Collections.sort(mutations);
      for (int i = 0; i < mutations.size(); i++) {
        MutationInfo next = mutations.get(i);
        switch (next.myOperation) {
        case MutationInfo.DELETE: {
          myAssignments.remove(next.myResource);
          break;
        }
        case MutationInfo.ADD: {
          ResourceAssignment result = auxAddAssignment(next.myResource);
          result.setLoad(next.myAssignment.getLoad());
          result.setCoordinator(next.myAssignment.isCoordinator());
          result.setBlocking(next.myAssignment.isBlocking());
          result.setNoEffort(next.myAssignment.isNoEffort());
          result.setRoleForAssignment(next.myAssignment.getRoleForAssignment());
          result.setActualEffort(next.myAssignment.getActualEffort());
        }
        default:
          break;
        }
      }
    }

  }

  private static class MutationInfo implements Comparable<MutationInfo> {
    static final int ADD = 0;

    static final int DELETE = 1;

    private final ResourceAssignment myAssignment;

    private final int myOrder;

    private static int ourOrder;

    private int myOperation;

    private final HumanResource myResource;

    public MutationInfo(ResourceAssignment assignment, int operation) {
      myAssignment = assignment;
      myOrder = ourOrder++;
      myOperation = operation;
      myResource = assignment.getResource();
    }

    public MutationInfo(HumanResource resource, int operation) {
      this.myAssignment = null;
      this.myOrder = ourOrder++;
      this.myOperation = operation;
      this.myResource = resource;
    }

    @Override
    public boolean equals(Object o) {
      boolean result = o instanceof MutationInfo;
      if (result) {
        result = myAssignment.getResource().equals(((MutationInfo) o).myAssignment.getResource());
      }
      return result;
    }

    @Override
    public int compareTo(MutationInfo o) {
      if (!(o instanceof MutationInfo)) {
        throw new IllegalArgumentException();
      }
      return myOrder - o.myOrder;
    }
  }

  public void importData(ResourceAssignmentCollection assignmentCollection) {
    if (myTask.isUnplugged()) {
      ResourceAssignment[] assignments = assignmentCollection.getAssignments();
      for (int i = 0; i < assignments.length; i++) {
        ResourceAssignment next = assignments[i];
        addAssignment(next);
      }
    } else {
      ResourceAssignment[] assignments = assignmentCollection.getAssignments();
      for (int i = 0; i < assignments.length; i++) {
        ResourceAssignment next = assignments[i];
        HumanResource nextResource = next.getResource();
        HumanResource nextImportedResource = myResourceManager.get().getById(nextResource.getId());
        if (nextImportedResource != null) {
          ResourceAssignment copy = new ResourceAssignmentImpl(nextImportedResource);
          copy.setLoad(next.getLoad());
          copy.setCoordinator(next.isCoordinator());
          copy.setBlocking(next.isBlocking());
          copy.setNoEffort(next.isNoEffort());
          copy.setRoleForAssignment(next.getRoleForAssignment());
          copy.setActualEffort(next.getActualEffort());
          addAssignment(copy);
        }
      }
    }
  }

  @Override
  public HumanResource getCoordinator() {
    for (Iterator<ResourceAssignment> assignments = myAssignments.values().iterator(); assignments.hasNext();) {
      ResourceAssignment next = assignments.next();
      if (next.isCoordinator()) {
        return next.getResource();
      }
    }
    return null;
  }
}
