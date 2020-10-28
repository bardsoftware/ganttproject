/*
GanttProject is an opensource project management tool.
Copyright (C) 2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.resource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import biz.ganttproject.core.calendar.GanttDaysOff;
import biz.ganttproject.core.option.EnumerationOption;

import net.sourceforge.ganttproject.CustomProperty;

public class OverwritingMerger implements HumanResourceMerger {
  private final EnumerationOption myMergeOption;
  private final Map<String, HumanResource> myCache = new HashMap<String, HumanResource>();

  public OverwritingMerger(EnumerationOption mergeOption) {
    myMergeOption = mergeOption;
  }

  @Override
  public void merge(Map<HumanResource, HumanResource> foreign2native) {
    for (Iterator<Entry<HumanResource, HumanResource>> entries = foreign2native.entrySet().iterator(); entries.hasNext();) {
      Map.Entry<HumanResource, HumanResource> entry = entries.next();
      merge(entry.getKey(), entry.getValue());
    }
  }

  private void merge(HumanResource mergeFrom, HumanResource mergeTo) {
    if (mergeFrom.getDaysOff() != null) {
      for (int i = 0; i < mergeFrom.getDaysOff().size(); i++) {
        mergeTo.addDaysOff(GanttDaysOff.create((GanttDaysOff) mergeFrom.getDaysOff().get(i)));
      }
    }
    mergeTo.setName(mergeFrom.getName());
    mergeTo.setDescription(mergeFrom.getDescription());
    mergeTo.setMail(mergeFrom.getMail());
    mergeTo.setPhone(mergeFrom.getPhone());
    mergeTo.setRole(mergeFrom.getRole());
    mergeTo.setStandardPayRate(mergeFrom.getStandardPayRate());
    List<CustomProperty> customProperties = mergeFrom.getCustomProperties();
    for (int i = 0; i < customProperties.size(); i++) {
      CustomProperty nextProperty = customProperties.get(i);
      mergeTo.addCustomProperty(nextProperty.getDefinition(), nextProperty.getValueAsString());
    }
  }

  @Override
  public HumanResource findNative(HumanResource foreign, HumanResourceManager nativeMgr) {
    if (MergeResourcesOption.NO.equals(myMergeOption.getValue())) {
      return null;
    }
    if (MergeResourcesOption.BY_ID.equals(myMergeOption.getValue())) {
      return nativeMgr.getById(foreign.getId());
    }
    if (MergeResourcesOption.BY_EMAIL.equals(myMergeOption.getValue())) {
      if (myCache.isEmpty()) {
        buildEmailCache(nativeMgr);
      }
      return myCache.get(foreign.getMail());
    }
    if (MergeResourcesOption.BY_NAME.equals(myMergeOption.getValue())) {
      if (myCache.isEmpty()) {
        buildNameCache(nativeMgr);
      }
      return myCache.get(foreign.getName());
    }
    assert false : "We should not be here. Option ID=" + myMergeOption.getValue();
    return null;
  }

  private void buildNameCache(HumanResourceManager nativeMgr) {
    List<HumanResource> resources = nativeMgr.getResources();
    for (int i = 0; i < resources.size(); i++) {
      HumanResource hr = resources.get(i);
      myCache.put(hr.getName(), hr);
    }
  }

  private void buildEmailCache(HumanResourceManager nativeMgr) {
    List<HumanResource> resources = nativeMgr.getResources();
    for (int i = 0; i < resources.size(); i++) {
      HumanResource hr = resources.get(i);
      myCache.put(hr.getMail(), hr);
    }
  }
}
