/*
 * ProjectResource.java
 *
 * Created on 27.05.2003
 */

package net.sourceforge.ganttproject.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;

/**
 * @author barmeier
 */
public abstract class ProjectResource {

    private int id = -1;

    protected String name;

    private double costsPerUnit;

    private int maximumUnitsPerDay;

    private String unitMeasure; // means hours, days, meter, ...

    private String description;

    private LoadDistribution myLoadDistribution;

    protected ProjectResource() {
        this(-1);
    }
    protected ProjectResource(int id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setUnitMeasure(String unitMeasure) {
        this.unitMeasure = unitMeasure;
    }

    public String getUnitMeasure() {
        return unitMeasure;
    }

    public void setCostsPerUnit(double costsPerUnit) {
        this.costsPerUnit = costsPerUnit;
    }

    public double getCostsPerUnit() {
        return costsPerUnit;
    }

    public void setMaximumUnitsPerDay(int maximumUnitsPerDay) {
        this.maximumUnitsPerDay = maximumUnitsPerDay;
    }

    public int getMaximumUnitsPerDay() {
        return maximumUnitsPerDay;
    }

    public void setId(int id) {
        if (this.id == -1) {
            // setting the id is only allowed when id is not assigned
            this.id = id;
        }
    }

    public int getId() {
        return id;
    }

    public boolean equals(Object obj) {
        boolean res = false;
        if (obj instanceof ProjectResource) {
            ProjectResource pr = (ProjectResource) obj;
            res = pr.id == id;
        }
        return res;
    }

    public String toString() {
        return name;
    }

    public abstract ProjectResource unpluggedClone();

    public ResourceAssignment createAssignment(
            ResourceAssignment assignmentToTask) {
        for (int i = 0; i < myAssignments.size(); i++) {
            if (myAssignments.get(i).getTask().equals(
                    assignmentToTask.getTask())) {
                // throw new IllegalStateException("An attempt to assign resource
                // to the same task twice");
            }
        }
        ResourceAssignment result = new ResourceAssignmentImpl(assignmentToTask);
        myAssignments.add(result);
        resetLoads();
        return result;
    }

    /** Removes the assignment objects associated to this ProjectResource
     *  and those associated to it's Tasks */
    private void removeAllAssignments() {
        List<ResourceAssignment> copy = new ArrayList<ResourceAssignment>(myAssignments);
        for (int i=0; i<copy.size(); i++) {
            ResourceAssignmentImpl next = (ResourceAssignmentImpl) copy.get(i);
            next.myAssignmentToTask.delete();
        }
        resetLoads();
    }

    public void delete() {
        removeAllAssignments();
    }

    public LoadDistribution getLoadDistribution() {
        if (myLoadDistribution==null) {
            myLoadDistribution = new LoadDistribution(this);
        }
        return myLoadDistribution;
    }

    private final List<ResourceAssignment> myAssignments = new ArrayList<ResourceAssignment>();

    private class ResourceAssignmentImpl implements ResourceAssignment {

        private final ResourceAssignment myAssignmentToTask;

        private float myLoad;

        private boolean myCoordinator;

        private Role myRoleForAssignment;

        private ResourceAssignmentImpl(ResourceAssignment assignmentToTask) {
            myAssignmentToTask = assignmentToTask;
        }

        public Task getTask() {
            return myAssignmentToTask.getTask();
        }

        public ProjectResource getResource() {
            return ProjectResource.this;
        }

        public float getLoad() {
            return myLoad;
        }

        public void setLoad(float load) {
            myLoad = load;
            ProjectResource.this.fireAssignmentChanged(this);
        }

        /** Removes all related assignments */
        public void delete() {
            ProjectResource.this.myAssignments.remove(this);
            ProjectResource.this.fireAssignmentChanged(this);
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

    public ResourceAssignment[] getAssignments() {
        return myAssignments.toArray(new ResourceAssignment[0]);
    }

    public void resetLoads() {
        myLoadDistribution = null;
    }
    private void fireAssignmentChanged(ResourceAssignmentImpl resourceAssignmentImpl) {
        resetLoads();
        fireAssignmentsChanged();
    }
    protected abstract void fireAssignmentsChanged();

    public void swapAssignments(ResourceAssignment a1, ResourceAssignment a2) {
        Collections.swap(myAssignments, myAssignments.indexOf(a1), myAssignments.indexOf(a2));
        resetLoads();
        fireAssignmentsChanged();
    }
}
