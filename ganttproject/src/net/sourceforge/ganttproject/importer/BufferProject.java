/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2013 BarD Software s.r.o

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
package net.sourceforge.ganttproject.importer;

import biz.ganttproject.core.table.ColumnList;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttProjectImpl;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.PrjInfos;
import net.sourceforge.ganttproject.document.DocumentCreator;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.importer.ImporterFromGanttFile.VisibleFieldsImpl;
import net.sourceforge.ganttproject.io.GPSaver;
import net.sourceforge.ganttproject.io.GanttXMLOpen;
import net.sourceforge.ganttproject.io.GanttXMLSaver;
import net.sourceforge.ganttproject.parser.GPParser;
import net.sourceforge.ganttproject.parser.ParserFactory;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.CustomColumnsManager;

/**
 * Buffer project is a target for importing functions, and when it is filled with
 * the imported data, it is merged into the real opened project.
 *
 * @author dbarashev
 */
public class BufferProject extends GanttProjectImpl implements ParserFactory {
  PrjInfos myProjectInfo = new PrjInfos();
  final DocumentManager myDocumentManager;
  final UIFacade myUIfacade;
  private final ColumnList myVisibleFields = new VisibleFieldsImpl();
  final ColumnList myResourceVisibleFields = new VisibleFieldsImpl();
  private final HumanResourceManager myBufferResourceManager;

  public BufferProject(IGanttProject targetProject, UIFacade uiFacade) {
    myDocumentManager = new DocumentCreator(this, uiFacade, this) {
      @Override
      protected ColumnList getVisibleFields() {
        return myVisibleFields;
      }
      @Override
      protected ColumnList getResourceVisibleFields() {
        return myResourceVisibleFields;
      }
    };
    myUIfacade = uiFacade;
    getTaskManager().getDependencyHardnessOption().setValue(targetProject.getTaskManager().getDependencyHardnessOption().getValue());
    myBufferResourceManager = new HumanResourceManager(RoleManager.Access.getInstance().getDefaultRole(),
        new CustomColumnsManager(), targetProject.getRoleManager());
  }

  public ColumnList getVisibleFields() {
    return myVisibleFields;
  }

  @Override
  public GPParser newParser() {
    return new GanttXMLOpen(myProjectInfo, getUIConfiguration(), getTaskManager(), myUIfacade);
  }

  @Override
  public GPSaver newSaver() {
    return new GanttXMLSaver(this);
  }

  @Override
  public DocumentManager getDocumentManager() {
    return myDocumentManager;
  }

  @Override
  public CustomPropertyManager getTaskCustomColumnManager() {
    return getTaskManager().getCustomPropertyManager();
  }

  @Override
  public HumanResourceManager getHumanResourceManager() {
    return myBufferResourceManager;
  }


}
