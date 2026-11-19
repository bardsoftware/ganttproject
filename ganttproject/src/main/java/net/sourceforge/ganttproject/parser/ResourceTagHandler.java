/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 GanttProject Team

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
package net.sourceforge.ganttproject.parser;

import biz.ganttproject.core.io.XmlProject;
import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.customproperty.CustomPropertyManager;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;

import kotlin.Unit;

import java.util.function.Consumer;

/** Class to parse the attribute of resources handler */
public class ResourceTagHandler {
  private final CustomPropertyManager myCustomPropertyManager;
  private final HumanResourceManager myResourceManager;
  private final RoleManager myRoleManager;
  private final ZoomManager myZoomManager;
  private final ColumnList myResourceColumns;
  private final Consumer<String> myProblemReporter;

  public ResourceTagHandler(HumanResourceManager resourceManager, RoleManager roleManager,
                            CustomPropertyManager resourceCustomPropertyManager, ZoomManager zoomManager,
                            ColumnList resourceColumns) {
    this(resourceManager, roleManager, resourceCustomPropertyManager, zoomManager, resourceColumns, problem -> {});
  }

  /**
   * @param problemReporter receives a localized description of every problem found in the resources, e.g. a vacation
   *                        which covers no day.
   */
  public ResourceTagHandler(HumanResourceManager resourceManager, RoleManager roleManager,
                            CustomPropertyManager resourceCustomPropertyManager, ZoomManager zoomManager,
                            ColumnList resourceColumns, Consumer<String> problemReporter) {
    myProblemReporter = problemReporter;
    myResourceManager = resourceManager;
    myCustomPropertyManager = resourceCustomPropertyManager;
    myRoleManager = roleManager;
    myZoomManager = zoomManager;
    myResourceColumns = resourceColumns;
  }

  public void process(XmlProject xmlProject) {
    var resourceLoader = new ResourceLoader(myResourceManager, myRoleManager, myCustomPropertyManager,
        (name, start, end) -> {
          myProblemReporter.accept(
              GanttLanguage.getInstance().formatText("resource.vacation.withoutDays", name, start.toString(), end.toString()));
          return Unit.INSTANCE;
        });
    resourceLoader.loadResources(xmlProject);
    ResourceLoaderKt.loadResourceView(xmlProject, myZoomManager, myResourceColumns);
  }
}
