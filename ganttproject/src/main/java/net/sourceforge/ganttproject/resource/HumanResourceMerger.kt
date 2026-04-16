/*
GanttProject is an opensource project management tool.
Copyright (C) 2010-2011 Dmitry Barashev, GanttProject team

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

import java.util.Map;

import biz.ganttproject.core.option.DefaultEnumerationOption;


public interface HumanResourceMerger {
  void merge(Map<HumanResource, HumanResource> existing2imported);

  HumanResource findNative(HumanResource foreign, HumanResourceManager nativeMgr);

  public static class MergeResourcesOption extends DefaultEnumerationOption<Object> {
    public static final String NO = "mergeresources_no";
    public static final String BY_ID = "mergeresources_by_id";
    public static final String BY_EMAIL = "mergeresources_by_email";
    public static final String BY_NAME = "mergeresources_by_name";

    public MergeResourcesOption() {
      super("impex.ganttprojectFiles.mergeResources", new String[] { NO, BY_ID, BY_EMAIL, BY_NAME });
    }
  }
}
