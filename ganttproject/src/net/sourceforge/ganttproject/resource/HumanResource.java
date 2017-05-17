/*
Copyright 2003 GanttProject Team

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

import biz.ganttproject.core.calendar.GanttDaysOff;
import com.google.common.base.Strings;
import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyHolder;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.CustomColumnsException;
import net.sourceforge.ganttproject.task.CustomColumnsValues;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

  private BigDecimal myStandardPayRate;

  private final DefaultListModel<GanttDaysOff> myDaysOffList = new DefaultListModel<>();

  private final List<ResourceAssignment> myAssignments = new ArrayList<>();

  private final CustomColumnsValues myCustomProperties;

  private final HumanResourceManager myManager;

  HumanResource(HumanResourceManager manager) {
    this("", -1, manager);
  }

  /** Creates a new instance of HumanResource */
  public HumanResource(String name, int id, HumanResourceManager manager) {
    this.id = id;
    this.name = name;
    myManager = manager;
    myCustomProperties = new CustomColumnsValues(myManager.getCustomPropertyManager());
  }

  private HumanResource(HumanResource copy) {
    areEventsEnabled = false;
    setId(-1);
    String newName = GanttLanguage.getInstance().formatText("resource.copy.prefix",
        GanttLanguage.getInstance().getText("copy2"), copy.getName());
    setName(newName);
    setDescription(copy.getDescription());
    setMail(copy.getMail());
    setPhone(copy.getPhone());
    setRole(copy.getRole());
    setStandardPayRate(copy.getStandardPayRate());
    myManager = copy.myManager;
    DefaultListModel<GanttDaysOff> copyDaysOff = copy.getDaysOff();
    for (int i = 0; i < copyDaysOff.getSize(); i++) {
      myDaysOffList.addElement(copyDaysOff.get(i));
    }
    areEventsEnabled = true;
    myCustomProperties = (CustomColumnsValues) copy.myCustomProperties.clone();
  }

  /**
   * Removes the assignment objects associated to this ProjectResource and those
   * associated to it's Tasks
   */
  private void removeAllAssignments() {
    List<ResourceAssignment> copy = new ArrayList<>(myAssignments);
    for (ResourceAssignment aCopy : copy) {
      ResourceAssignmentImpl next = (ResourceAssignmentImpl) aCopy;
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
    fireResourceChanged();
  }

  public String getName() {
    return Strings.nullToEmpty(name);
  }

  public void setDescription(String description) {
    this.description = description;
    fireResourceChanged();
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
      System.err.println("[HumanResource] getRole(): I have no role :( name=" + getName());
    }
    return myRole;
  }

  public void addDaysOff(GanttDaysOff gdo) {
    myDaysOffList.addElement(gdo);
    fireResourceChanged();
  }

  public DefaultListModel<GanttDaysOff> getDaysOff() {
    return myDaysOffList;
  }

  Object getCustomField(CustomPropertyDefinition def) {
    return myCustomProperties.getValue(def);
  }

  public void setCustomField(CustomPropertyDefinition def, Object value) {
    try {
      myCustomProperties.setValue(def, value);
      fireResourceChanged();
    } catch (CustomColumnsException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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

  private void fireAssignmentsChanged() {
    if (areEventsEnabled) {
      myManager.fireAssignmentsChanged(this);
    }
  }

  @Override
  public List<CustomProperty> getCustomProperties() {
    return myCustomProperties.getCustomProperties();
  }

  @Override
  public CustomProperty addCustomProperty(CustomPropertyDefinition definition, String valueAsString) {
    final CustomPropertyDefinition stubDefinition = CustomPropertyManager.PropertyTypeEncoder.decodeTypeAndDefaultValue(
        definition.getTypeAsString(), valueAsString);
    setCustomField(definition, stubDefinition.getDefaultValue());
    return new CustomPropertyImpl(definition, stubDefinition.getDefaultValue());
  }

  private static class CustomPropertyImpl implements CustomProperty {
    private CustomPropertyDefinition myDefinition;
    private Object myValue;

    CustomPropertyImpl(CustomPropertyDefinition definition, Object value) {
      myDefinition = definition;
      myValue = value;
    }

    @Override
    public CustomPropertyDefinition getDefinition() {
      return myDefinition;
    }

    @Override
    public Object getValue() {
      return myValue;
    }

    @Override
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

  private void fireAssignmentChanged() {
    resetLoads();
    fireAssignmentsChanged();
  }

  public void swapAssignments(ResourceAssignment a1, ResourceAssignment a2) {
    Collections.swap(myAssignments, myAssignments.indexOf(a1), myAssignments.indexOf(a2));
    resetLoads();
    fireAssignmentsChanged();
  }

  public void setStandardPayRate(BigDecimal rate) {
    myStandardPayRate = rate;
  }

  public BigDecimal getStandardPayRate() {
    return myStandardPayRate == null ? BigDecimal.ZERO : myStandardPayRate;
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

    @Override
    public Task getTask() {
      return myAssignmentToTask.getTask();
    }

    @Override
    public HumanResource getResource() {
      return HumanResource.this;
    }

    @Override
    public float getLoad() {
      return myLoad;
    }

    @Override
    public void setLoad(float load) {
      myLoad = load;
      HumanResource.this.fireAssignmentChanged();
    }

    /** Removes all related assignments */
    @Override
    public void delete() {
      HumanResource.this.myAssignments.remove(this);
      HumanResource.this.fireAssignmentChanged();
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
    public Role getRoleForAssignment() {

      return myRoleForAssignment;
    }

    @Override
    public void setRoleForAssignment(Role role) {
      myRoleForAssignment = role;
    }

    @Override
    public String toString() {
      return this.getResource().getName() + " -> " + this.getTask().getName();
    }
  }
}
