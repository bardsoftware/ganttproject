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
package net.sourceforge.ganttproject.importer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.prefs.Preferences;

import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttProjectImpl;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.PrjInfos;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentCreator;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.document.FileDocument;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueEvent;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueListener;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.io.GPSaver;
import net.sourceforge.ganttproject.io.GanttXMLOpen;
import net.sourceforge.ganttproject.parser.GPParser;
import net.sourceforge.ganttproject.parser.ParserFactory;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceMerger;
import net.sourceforge.ganttproject.resource.OverwritingMerger;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerImpl;

public class ImporterFromGanttFile extends ImporterBase {
  private final DefaultEnumerationOption<Object> myMergeResourcesOption = new HumanResourceMerger.MergeResourcesOption();

  private final GPOption[] myOptions = new GPOption[] { myMergeResourcesOption };

  public ImporterFromGanttFile() {
    super("ganttprojectFiles");
    myMergeResourcesOption.lock();
    myMergeResourcesOption.loadPersistentValue(HumanResourceMerger.MergeResourcesOption.BY_ID);
    myMergeResourcesOption.commit();
  }

  @Override
  public String getFileNamePattern() {
    return "xml|gan";
  }

  @Override
  protected GPOption[] getOptions() {
    return myOptions;
  }

  @Override
  public void setContext(IGanttProject project, UIFacade uiFacade, Preferences preferences) {
    super.setContext(project, uiFacade, preferences);
    final Preferences node = preferences.node("/instance/net.sourceforge.ganttproject/import");
    myMergeResourcesOption.lock();
    myMergeResourcesOption.loadPersistentValue(node.get(myMergeResourcesOption.getID(),
        HumanResourceMerger.MergeResourcesOption.BY_ID));
    myMergeResourcesOption.commit();
    myMergeResourcesOption.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        node.put(myMergeResourcesOption.getID(), String.valueOf(event.getNewValue()));
      }
    });
  }

  @Override
  public void run(final File selectedFile) {
    final IGanttProject targetProject = getProject();
    final BufferProject bufferProject = createBufferProject(targetProject, getUiFacade());
    getUiFacade().getUndoManager().undoableEdit("Import", new Runnable() {
      @Override
      public void run() {
        ImporterFromGanttFile.this.run(selectedFile, targetProject, bufferProject);
      }
    });
  }

  private void run(File selectedFile, IGanttProject targetProject, BufferProject bufferProject) {
    openDocument(targetProject, bufferProject, getUiFacade(), selectedFile);
    getUiFacade().getTaskTree().getVisibleFields().importData(bufferProject.getVisibleFields());
    getUiFacade().getResourceTree().getVisibleFields().importData(bufferProject.myResourceVisibleFields);
  }

  private static class TaskFieldImpl implements TableHeaderUIFacade.Column {
    private final String myID;
    private final int myOrder;
    private final int myWidth;

    TaskFieldImpl(String id, int order, int width) {
      myID = id;
      myOrder = order;
      myWidth = width;
    }

    @Override
    public String getID() {
      return myID;
    }

    @Override
    public int getOrder() {
      return myOrder;
    }

    @Override
    public int getWidth() {
      return myWidth;
    }

    @Override
    public boolean isVisible() {
      return true;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public void setVisible(boolean visible) {
    }

    @Override
    public void setOrder(int order) {
    }

    @Override
    public void setWidth(int width) {
    }
  }

  private static class VisibleFieldsImpl implements TableHeaderUIFacade {
    private final List<Column> myFields = new ArrayList<Column>();

    @Override
    public void add(String name, int order, int width) {
      myFields.add(new TaskFieldImpl(name, order, width));
    }

    @Override
    public void clear() {
      myFields.clear();
    }

    @Override
    public Column getField(int index) {
      return myFields.get(index);
    }

    @Override
    public int getSize() {
      return myFields.size();
    }

    @Override
    public void importData(TableHeaderUIFacade source) {
      for (int i = 0; i < source.getSize(); i++) {
        Column nextField = source.getField(i);
        myFields.add(nextField);
      }
    }
  }

  class BufferProject extends GanttProjectImpl implements ParserFactory {
    PrjInfos myProjectInfo = new PrjInfos();
    final DocumentManager myDocumentManager;
    final TaskManager myTaskManager;
    final UIFacade myUIfacade;
    private final TableHeaderUIFacade myVisibleFields = new VisibleFieldsImpl();
    private final TableHeaderUIFacade myResourceVisibleFields = new VisibleFieldsImpl();

    BufferProject(IGanttProject targetProject, UIFacade uiFacade) {
      myDocumentManager = new DocumentCreator(this, uiFacade, this) {
        @Override
        protected TableHeaderUIFacade getVisibleFields() {
          return myVisibleFields;
        }
        @Override
        protected TableHeaderUIFacade getResourceVisibleFields() {
          return myResourceVisibleFields;
        }
      };
      myTaskManager = targetProject.getTaskManager().emptyClone();
      myUIfacade = uiFacade;
    }

    public TableHeaderUIFacade getVisibleFields() {
      return myVisibleFields;
    }

    @Override
    public GPParser newParser() {
      return new GanttXMLOpen(myProjectInfo, getUIConfiguration(), getTaskManager(), myUIfacade);
    }

    @Override
    public GPSaver newSaver() {
      return null;
    }

    @Override
    public DocumentManager getDocumentManager() {
      return myDocumentManager;
    }

    @Override
    public TaskManager getTaskManager() {
      return myTaskManager;
    }

    @Override
    public CustomPropertyManager getTaskCustomColumnManager() {
      return myTaskManager.getCustomPropertyManager();
    }
  }

  private BufferProject createBufferProject(final IGanttProject targetProject, final UIFacade uiFacade) {
    return new BufferProject(targetProject, uiFacade);
  }

  protected Document getDocument(File selectedFile) {
    return new FileDocument(selectedFile);
  }

  protected void openDocument(IGanttProject targetProject, IGanttProject bufferProject, UIFacade uiFacade,
      File selectedFile) {
    try {
      Document document = bufferProject.getDocumentManager().getDocument(selectedFile.getAbsolutePath());
      document.read();
      targetProject.getRoleManager().importData(bufferProject.getRoleManager());
      {
        CustomPropertyManager targetResCustomPropertyMgr = targetProject.getResourceCustomPropertyManager();
        targetResCustomPropertyMgr.importData(bufferProject.getResourceCustomPropertyManager());
      }
      Map<HumanResource, HumanResource> original2ImportedResource = targetProject.getHumanResourceManager().importData(
          bufferProject.getHumanResourceManager(), new OverwritingMerger(myMergeResourcesOption));

      {
        CustomPropertyManager targetCustomColumnStorage = targetProject.getTaskCustomColumnManager();
        Map<CustomPropertyDefinition, CustomPropertyDefinition> that2thisCustomDefs = targetCustomColumnStorage.importData(bufferProject.getTaskCustomColumnManager());
        TaskManagerImpl origTaskManager = (TaskManagerImpl) targetProject.getTaskManager();
        try {
          origTaskManager.setEventsEnabled(false);
          Map<Task, Task> original2ImportedTask = origTaskManager.importData(bufferProject.getTaskManager(),
              that2thisCustomDefs);
          origTaskManager.importAssignments(bufferProject.getTaskManager(), targetProject.getHumanResourceManager(),
              original2ImportedTask, original2ImportedResource);
        } finally {
          origTaskManager.setEventsEnabled(true);
        }
      }
      uiFacade.refresh();
    } catch (DocumentException e) {
      uiFacade.showErrorDialog(e);
    } catch (IOException e) {
      uiFacade.showErrorDialog(e);
    }
  }
}
