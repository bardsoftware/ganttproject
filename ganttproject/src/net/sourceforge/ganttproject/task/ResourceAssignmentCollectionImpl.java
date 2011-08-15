/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.Role;

class ResourceAssignmentCollectionImpl implements ResourceAssignmentCollection {
    private final Map<HumanResource, ResourceAssignment> myAssignments = new HashMap<HumanResource, ResourceAssignment>();

    private final TaskImpl myTask;

    private HumanResourceManager myResourceManager;

    public ResourceAssignmentCollectionImpl(TaskImpl task,
            HumanResourceManager resourceManager) {
        myTask = task;
        myResourceManager = resourceManager;
    }

    private ResourceAssignmentCollectionImpl(
            ResourceAssignmentCollectionImpl collection) {
        myTask = collection.myTask;
        ResourceAssignment[] assignments = collection.getAssignments();
        for (int i = 0; i < assignments.length; i++) {
            ResourceAssignment next = assignments[i];
            ResourceAssignment copy = new ResourceAssignmentImpl(next
                    .getResource());
            copy.setLoad(next.getLoad());
            copy.setCoordinator(next.isCoordinator());
            copy.setRoleForAssignment(next.getRoleForAssignment());
            addAssignment(copy);
        }
    }

    public void clear() {
        ResourceAssignment[] assignments = getAssignments();
        for (int i = 0; i < assignments.length; i++) {
            assignments[i].delete();
        }
    }

    public ResourceAssignment[] getAssignments() {
        return myAssignments.values().toArray(
                new ResourceAssignment[myAssignments.size()]);
    }

    public ResourceAssignment getAssignment(HumanResource resource) {
        return myAssignments.get(resource);
    }

    public ResourceAssignmentMutator createMutator() {
        return new ResourceAssignmentMutatorImpl();
    }

    ResourceAssignmentCollectionImpl copy() {
        return new ResourceAssignmentCollectionImpl(this);
    }

    public ResourceAssignment addAssignment(HumanResource resource) {
        return auxAddAssignment(resource);
    }

    public void deleteAssignment(HumanResource resource) {
        myAssignments.remove(resource);
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
     * @param resource Assigned resource
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
//            resource.setAssignmentCollection(ResourceAssignmentCollectionImpl.this);
        }

        public Task getTask() {
            return ResourceAssignmentCollectionImpl.this.getTask();
        }

        public HumanResource getResource() {
            return myAssignmentToResource.getResource();
        }

        public float getLoad() {
            return myAssignmentToResource.getLoad();
        }

        // TODO transaction
        public void setLoad(float load) {
            myAssignmentToResource.setLoad(load);
        }

        /**
         * Deletes all the assignments and all the related assignments
         */
        public void delete() {
            ResourceAssignmentCollectionImpl.this
                    .deleteAssignment(getResource());
            myAssignmentToResource.delete();
        }

        public void setCoordinator(boolean responsible) {
            myAssignmentToResource.setCoordinator(responsible);
        }

        public boolean isCoordinator() {
            return myAssignmentToResource.isCoordinator();
        }

        public Role getRoleForAssignment() {
            return myAssignmentToResource.getRoleForAssignment();
        }

        public void setRoleForAssignment(Role role) {
            myAssignmentToResource.setRoleForAssignment(role);

        }

        public String toString() {
            return this.getResource().getName() + " -> "
                    + this.getTask().getName();
        }
    }

    private class ResourceAssignmentStub implements ResourceAssignment {
        private final HumanResource myResource;

        private float myLoad;

        private boolean myCoordinator;

        private Role myRoleForAssignment;

        public ResourceAssignmentStub(HumanResource resource) {
            myResource = resource;
        }

        public Task getTask() {
            return ResourceAssignmentCollectionImpl.this.getTask();
        }

        public HumanResource getResource() {
            return myResource;
        }

        public float getLoad() {
            return myLoad;
        }

        public void setLoad(float load) {
            myLoad = load;
        }

        public void delete() {
        }

        public void setCoordinator(boolean responsible) {
            myCoordinator = responsible;
        }

        public boolean isCoordinator() {
            return myCoordinator;
        }

        public Role getRoleForAssignment() {

            return myRoleForAssignment;
        }

        public void setRoleForAssignment(Role role) {
            myRoleForAssignment = role;

        }

        public String toString() {
            return this.getResource().getName() + " -> "
                    + this.getTask().getName();
        }
    }

    private class ResourceAssignmentMutatorImpl implements
            ResourceAssignmentMutator {
        private Map<HumanResource, MutationInfo> myQueue = new HashMap<HumanResource, MutationInfo>();

        public ResourceAssignment addAssignment(HumanResource resource) {
            ResourceAssignment result = new ResourceAssignmentStub(resource);
            myQueue.put(resource, new MutationInfo(result, MutationInfo.ADD));
            return result;
        }

        public void deleteAssignment(HumanResource resource) {
            MutationInfo info = myQueue.get(resource);
            if (info == null) {
                myQueue.put(resource, new MutationInfo(resource,
                        MutationInfo.DELETE));
            } else if (info.myOperation == MutationInfo.ADD) {
                myQueue.remove(resource);
            }
        }

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
                    result.setRoleForAssignment(next.myAssignment
                            .getRoleForAssignment());
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

        public boolean equals(Object o) {
            boolean result = o instanceof MutationInfo;
            if (result) {
                result = myAssignment.getResource().equals(
                        ((MutationInfo) o).myAssignment.getResource());
            }
            return result;
        }

        public int compareTo(MutationInfo o) {
            if (!(o instanceof MutationInfo)) {
                throw new IllegalArgumentException();
            }
            return myOrder - o.myOrder;
        }
    }

    public void importData(ResourceAssignmentCollection assignmentCollection) {
        if (myTask.isUnplugged()) {
            ResourceAssignment[] assignments = assignmentCollection
                    .getAssignments();
            for (int i = 0; i < assignments.length; i++) {
                ResourceAssignment next = assignments[i];
                addAssignment(next);
            }
        } else {
            ResourceAssignment[] assignments = assignmentCollection
                    .getAssignments();
            for (int i = 0; i < assignments.length; i++) {
                ResourceAssignment next = assignments[i];
                HumanResource nextResource = next.getResource();
                HumanResource nextImportedResource = myResourceManager
                        .getById(nextResource.getId());
                if (nextImportedResource != null) {
                    ResourceAssignment copy = new ResourceAssignmentImpl(
                            nextImportedResource);
                    copy.setLoad(next.getLoad());
                    copy.setCoordinator(next.isCoordinator());
                    copy.setRoleForAssignment(next.getRoleForAssignment());
                    addAssignment(copy);
                }
            }
        }
    }

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
