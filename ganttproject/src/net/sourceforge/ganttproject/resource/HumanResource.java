/*
 * HumanResource.java
 *
 * Created on 27.05.2003
 */

package net.sourceforge.ganttproject.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;

import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyHolder;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.calendar.GanttDaysOff;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.CustomColumnsException;
import net.sourceforge.ganttproject.task.CustomColumnsValues;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;

/**
 * @author barmeier
 */
public class HumanResource implements CustomPropertyHolder {
    /**
     * Can be turned (temporarily) off to prevent (a flood of) events
     */
    private boolean areEventsEnabled = true;

    private int id = -1;

    private String name;

    private String phone = "";

    private String email = "";

    private Role myRole;

    private String description;

    private LoadDistribution myLoadDistribution;

    private final DefaultListModel myDaysOffList = new DefaultListModel();

    private final List<ResourceAssignment> myAssignments = new ArrayList<ResourceAssignment>();

    private final CustomColumnsValues myCustomProperties;

    private final HumanResourceManager myManager;

    HumanResource(HumanResourceManager manager) {
        this("", -1, manager);
    }

    /** Creates a new instance of HumanResource */
    HumanResource(String name, int id, HumanResourceManager manager) {
        this.id = id;
        this.name = name;
        myManager = manager;
        myCustomProperties = new CustomColumnsValues(myManager.getCustomPropertyManager());
    }

    private HumanResource(HumanResource copy) {
        areEventsEnabled = false;
        setId(-1);
        setName(GanttLanguage.getInstance().getText("copy2") + "_"
                + copy.getName());
        setDescription(copy.getDescription());
        setMail(copy.getMail());
        setPhone(copy.getPhone());
        setRole(copy.getRole());
        myManager = copy.myManager;
        DefaultListModel copyDaysOff = copy.getDaysOff();
        for (int i = 0; i < copyDaysOff.getSize(); i++) {
            myDaysOffList.addElement(copyDaysOff.get(i));
        }
        areEventsEnabled = true;
        myCustomProperties = (CustomColumnsValues) copy.myCustomProperties.clone();
    }

    /**
     * Removes the assignment objects associated to this ProjectResource and
     * those associated to it's Tasks
     */
    private void removeAllAssignments() {
        List<ResourceAssignment> copy = new ArrayList<ResourceAssignment>(
                myAssignments);
        for (int i = 0; i < copy.size(); i++) {
            ResourceAssignmentImpl next = (ResourceAssignmentImpl) copy.get(i);
            next.myAssignmentToTask.delete();
        }
        resetLoads();
    }

    public void delete() {
        removeAllAssignments();
        myManager.remove(this);
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

    public void setMail(String email) {
        if (email == null) {
            return;
        }
        this.email = email;
        fireResourceChanged();
    }

    public String getMail() {
        return email;
    }

    public void setPhone(String phone) {
        if (phone == null) {
            return;
        }
        this.phone = phone;
        fireResourceChanged();
    }

    public String getPhone() {
        return phone;
    }

    public void setRole(Role role) {
        myRole = role;
        fireResourceChanged();
    }

    public Role getRole() {
        if (myRole == null) {
            System.err
                    .println("[HumanResource] getRole(): I have no role :( name="
                            + getName());
        }
        return myRole;
    }

    public void addDaysOff(GanttDaysOff gdo) {
        myDaysOffList.addElement(gdo);
        fireResourceChanged();
    }

    public DefaultListModel getDaysOff() {
        return myDaysOffList;
    }

    public Object getCustomField(String propertyName) {
        return myCustomProperties.getValue(propertyName);
    }

    public void setCustomField(String propertyName, Object value) {
        try {
            myCustomProperties.setValue(propertyName, value);
        } catch (CustomColumnsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void removeCustomField(String propertyName) {
        myCustomProperties.removeCustomColumn(propertyName);
    }

    public ResourceAssignment createAssignment(ResourceAssignment assignmentToTask) {
        ResourceAssignment result = new ResourceAssignmentImpl(assignmentToTask);
        myAssignments.add(result);
        resetLoads();
        fireAssignmentsChanged();
        return result;
    }

    public ResourceAssignment[] getAssignments() {
        return myAssignments.toArray(new ResourceAssignment[0]);
    }

    public HumanResource unpluggedClone() {
        return new HumanResource(this);
    }

    private void fireResourceChanged() {
        if (areEventsEnabled) {
            myManager.fireResourceChanged(this);
        }
    }

    protected void fireAssignmentsChanged() {
        if (areEventsEnabled) {
            myManager.fireAssignmentsChanged(this);
        }
    }

    public List<CustomProperty> getCustomProperties() {
        return myCustomProperties.getCustomProperties();
    }

    public CustomProperty addCustomProperty(
            CustomPropertyDefinition definition, String valueAsString) {
        final CustomPropertyDefinition stubDefinition = CustomPropertyManager.PropertyTypeEncoder
                .decodeTypeAndDefaultValue(definition.getTypeAsString(),
                        valueAsString);
        setCustomField(definition.getName(), stubDefinition.getDefaultValue());
        return new CustomPropertyImpl(definition, stubDefinition
                .getDefaultValue());
    }

    private static class CustomPropertyImpl implements CustomProperty {
        private CustomPropertyDefinition myDefinition;
        private Object myValue;

        public CustomPropertyImpl(CustomPropertyDefinition definition,
                Object value) {
            myDefinition = definition;
            myValue = value;
        }

        public CustomPropertyDefinition getDefinition() {
            return myDefinition;
        }

        public Object getValue() {
            return myValue;
        }

        public String getValueAsString() {
            return HumanResourceManager.getValueAsString(myValue);
        }
    }

    public void resetLoads() {
        myLoadDistribution = null;
    }

    public LoadDistribution getLoadDistribution() {
        if (myLoadDistribution == null) {
            myLoadDistribution = new LoadDistribution(this);
        }
        return myLoadDistribution;
    }

    private void fireAssignmentChanged(
            ResourceAssignmentImpl resourceAssignmentImpl) {
        resetLoads();
        fireAssignmentsChanged();
    }

    public void swapAssignments(ResourceAssignment a1, ResourceAssignment a2) {
        Collections.swap(myAssignments, myAssignments.indexOf(a1),
                myAssignments.indexOf(a2));
        resetLoads();
        fireAssignmentsChanged();
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof HumanResource) {
            HumanResource pr = (HumanResource) obj;
            result = pr.id == id;
        }
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

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

        public HumanResource getResource() {
            return HumanResource.this;
        }

        public float getLoad() {
            return myLoad;
        }

        public void setLoad(float load) {
            myLoad = load;
            HumanResource.this.fireAssignmentChanged(this);
        }

        /** Removes all related assignments */
        public void delete() {
            HumanResource.this.myAssignments.remove(this);
            HumanResource.this.fireAssignmentChanged(this);
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

        @Override
        public String toString() {
            return this.getResource().getName() + " -> "
                    + this.getTask().getName();
        }
    }
}
