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
import biz.ganttproject.customproperty.*;
import com.google.common.base.Strings;
import kotlin.Unit;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

  private final List<GanttDaysOff> myDaysOffList = new ArrayList<>();

  /**
   * What getDaysOff() hands out: a view, so that a caller holding on to it keeps seeing the
   * resource, and unmodifiable, so that the only ways in are addDaysOff, removeDaysOff and
   * clearDaysOff. Every one of those resets the load distribution and notifies -- a caller able to
   * mutate the list directly could change the resource behind the back of both.
   */
  private final List<GanttDaysOff> myDaysOffView = Collections.unmodifiableList(myDaysOffList);

  /**
   * Called by every method that changes the days off, after it really changed something. A day off
   * shifts this resource's load distribution just as an assignment does, so the cached distribution
   * has to go and whoever watches the resource has to hear about it.
   */
  private void onDaysOffChanged() {
    resetLoads();
    fireResourceChanged();
  }

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
    myCustomProperties = new CustomColumnsValues(myManager.getCustomPropertyManager(), event -> Unit.INSTANCE);
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
    // Straight into the list rather than through addDaysOff(): the copy is not in the manager yet
    // and must not be announced, which is what areEventsEnabled = false above takes care of for the
    // setters. Adding to the list directly keeps the days off out of it without depending on that
    // flag at all, and there is no load distribution to reset on a resource being built.
    myDaysOffList.addAll(copy.getDaysOff());
    areEventsEnabled = true;
    myCustomProperties = copy.myCustomProperties.copyOf();
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
    System.out.println("add day off: " + gdo.getStart() + " - " + gdo.getFinish() + "");
    myDaysOffList.add(gdo);
    onDaysOffChanged();
  }

  /**
   * Takes a single day off interval away again -- the counterpart of {@link #addDaysOff}. Together
   * with {@link #clearDaysOff} it is the only way out, as {@link #getDaysOff} hands out a view that
   * cannot be modified.
   *
   * The interval is matched the way the list matches it, that is by {@code Object.equals}. Note that
   * GanttDaysOff only overloads {@code equals(GanttDaysOff)} and does not override
   * {@code equals(Object)}, so an interval built afresh from the same two dates is NOT the one this
   * resource holds. Pass an instance obtained from this resource.
   *
   * @return true if the interval was there and has been removed, false if there was nothing to do
   */
  public boolean removeDaysOff(GanttDaysOff gdo) {
    if (!myDaysOffList.remove(gdo)) {
      // Nothing went away, so there is nothing to recalculate and nothing to report.
      return false;
    }
    onDaysOffChanged();
    return true;
  }

  /**
   * Takes every day off interval away in one go. This is what a caller replacing the whole set needs
   * -- the resource properties dialog does not edit single intervals, it drops all of them and
   * writes the edited ones back.
   *
   * Notifies once when something was removed and not at all when the list was already empty.
   *
   * Named clearDaysOff rather than plain clear(), unlike HumanResourceManager.clear(), because a
   * resource holds assignments and custom properties too and a bare clear() would not say which of
   * them it means. The ...DaysOff suffix is what the two neighbouring methods already use.
   */
  public void clearDaysOff() {
    if (myDaysOffList.isEmpty()) {
      // Nothing to remove, so nothing to report. Skipping the empty case is what keeps the cost of
      // the dialog's clear-all-and-rewrite at (M > 0 ? 1 : 0) + N notifications.
      return;
    }
    myDaysOffList.clear();
    onDaysOffChanged();
  }

  /**
   * @return the resource's days off as an unmodifiable view. Changes go through addDaysOff,
   *         removeDaysOff and clearDaysOff; the returned list follows them.
   */
  public List<GanttDaysOff> getDaysOff() {
    return myDaysOffView;
  }

  public Object getCustomField(CustomPropertyDefinition def) {
    return myCustomProperties.getValue(def);
  }

  @Override
  public Object getValue(CustomPropertyDefinition def) {
    return getCustomField(def);
  }

  @Override public void setValue(CustomPropertyDefinition def, Object value) throws CustomColumnsException {
    myCustomProperties.setValue(def, value);
    fireResourceChanged();
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

  public CustomColumnsValues getCustomValues() {
    return myCustomProperties;
  }

  @Override
  public List<CustomProperty> getCustomProperties() {
    return myCustomProperties.getCustomProperties();
  }

  @Override
  public CustomProperty addCustomProperty(CustomPropertyDefinition definition, String valueAsString) throws CustomColumnsException {
    final PropertyTypeEncoder.CustomPropertyDefinitionStub stubDefinition = PropertyTypeEncoder.INSTANCE.decodeTypeAndDefaultValue(
        definition.getTypeAsString(), valueAsString);
    setValue(definition, stubDefinition.getDefaultValue());
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

  public double getTotalLoad() {
    double totalLoad = 0.0;
    for (ResourceAssignment assignment : myAssignments) {
      totalLoad = totalLoad + assignment.getLoad() * assignment.getTask().getDuration().getLength() / 100.0;
    }
    return totalLoad;
  }

  public BigDecimal getTotalCost() {
    BigDecimal cost = BigDecimal.ZERO;
    for (ResourceAssignment assignment : myAssignments) {
      int taskDuration = assignment.getTask().getDuration().getLength();
      BigDecimal assignmentCost = new BigDecimal(taskDuration * assignment.getLoad() / 100).multiply(getStandardPayRate());
      cost = cost.add(assignmentCost);
    }
    return cost;
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
  public int hashCode() {
    return Objects.hash(id);
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
