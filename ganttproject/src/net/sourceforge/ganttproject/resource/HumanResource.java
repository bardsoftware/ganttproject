/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 GanttProject Team

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
package net.sourceforge.ganttproject.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.DefaultListModel;

import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyHolder;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.calendar.GanttDaysOff;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.roles.Role;
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

    /**
     * contains all the custom property values of a resource. the key is the
     * property name and the value is the property value
     */
    private final Map<String, Object> customFields;

    private final HumanResourceManager myManager;

    HumanResource(HumanResourceManager manager) {
        this("", -1, manager);
    }

    /** Creates a new instance of HumanResource */
    HumanResource(String name, int id, HumanResourceManager manager) {
        this.id = id;
        this.name = name;
        customFields = new HashMap<String, Object>();
        myManager = manager;
    }

    void initCustomProperties() {
        List<CustomPropertyDefinition> defs = myManager
                .getCustomPropertyManager().getDefinitions();
        for (int i = 0; i < defs.size(); i++) {
            CustomPropertyDefinition nextDefinition = defs.get(i);
            customFields.put(nextDefinition.getName(), nextDefinition
                    .getDefaultValue());
        }
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
        customFields = new HashMap<String, Object>(copy.customFields);
        areEventsEnabled = true;
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

    public Object getCustomField(String title) {
        return customFields.get(title);
    }

    public void setCustomField(String title, Object val) {
        this.customFields.put(title, val);
    }

    public void removeCustomField(String title) {
        this.customFields.remove(title);
    }

    public ResourceAssignment createAssignment(
            ResourceAssignment assignmentToTask) {
//        for (int i = 0; i < myAssignments.size(); i++) {
//            if (myAssignments.get(i).getTask().equals(
//                    assignmentToTask.getTask())) {
//                throw new IllegalStateException(
//                        "An attempt to assign resource to the same task twice");
//            }
//        }
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
        List<CustomProperty> result = new ArrayList<CustomProperty>(
                customFields.size());
        for (Iterator<Entry<String, Object>> entries = customFields.entrySet()
                .iterator(); entries.hasNext();) {
            Map.Entry<String, Object> nextEntry = entries.next();
            String nextName = nextEntry.getKey();
            Object nextValue = nextEntry.getValue();
            CustomPropertyDefinition nextDefinition = myManager
                    .getCustomPropertyDefinition(nextName);
            result.add(new CustomPropertyImpl(nextDefinition, nextValue));
        }
        return result;
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

    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof HumanResource) {
            HumanResource pr = (HumanResource) obj;
            result = pr.id == id;
        }
        return result;
    }

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

        public String toString() {
            return this.getResource().getName() + " -> "
                    + this.getTask().getName();
        }
    }
}
