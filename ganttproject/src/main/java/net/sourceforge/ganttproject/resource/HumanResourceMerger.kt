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
package net.sourceforge.ganttproject.resource

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.option.ObservableEnumerationOption
import biz.ganttproject.core.option.PropertyPaneBuilder

enum class MergeResourcesEnum {
  NO, BY_ID, BY_EMAIL, BY_NAME;

  companion object {
    /**
     * The way of detecting duplicate resources which is used when the user has not chosen one.
     *
     * It must not be BY_ID: resource IDs start at 0 in every project file, so two files which were
     * created independently collide on resource #0 by construction, even when they describe
     * different people. With BY_ID the import silently replaced a resource of the target project
     * with an imported one -- name, mail, phone, role and pay rate included -- while the
     * assignments of the target project kept pointing at that record. BY_NAME only merges records
     * which the user would consider the same person anyway.
     */
    @JvmField
    val DEFAULT = BY_NAME
  }
}

interface HumanResourceMerger {
    fun merge(existing2imported: MutableMap<HumanResource?, HumanResource?>?)

    fun findNative(foreign: HumanResource?, nativeMgr: HumanResourceManager?): HumanResource?

    class MergeResourcesOption : ObservableEnumerationOption<MergeResourcesEnum>("impex.mergeResources",
      MergeResourcesEnum.DEFAULT,
      MergeResourcesEnum.entries
    ) {
      override fun visitPropertyPaneBuilder(builder: PropertyPaneBuilder) {
        builder.dropdown(this.delegate) {
          value2string = { RootLocalizer.formatText("optionValue.mergeresources_${it.name.lowercase()}.label") }
          labelText = RootLocalizer.formatText("option.impex.ganttprojectFiles.mergeResources.label")
        }
      }
    }
}
