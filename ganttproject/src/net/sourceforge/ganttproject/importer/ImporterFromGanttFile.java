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

import biz.ganttproject.core.calendar.ImportCalendarOption;
import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.table.ColumnList;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.document.FileDocument;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceMerger;
import net.sourceforge.ganttproject.resource.HumanResourceMerger.MergeResourcesOption;
import net.sourceforge.ganttproject.resource.OverwritingMerger;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManagerImpl;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmCollection;
import org.osgi.service.prefs.Preferences;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImporterFromGanttFile extends ImporterBase {
  private final HumanResourceMerger.MergeResourcesOption myMergeResourcesOption = new HumanResourceMerger.MergeResourcesOption();
  private final ImportCalendarOption myImportCalendarOption = new ImportCalendarOption();

  private final GPOption[] myOptions = new GPOption[]{myMergeResourcesOption, myImportCalendarOption};

  public ImporterFromGanttFile() {
    super("ganttprojectFiles");
    myMergeResourcesOption.loadPersistentValue(HumanResourceMerger.MergeResourcesOption.BY_ID);
    myImportCalendarOption.setSelectedValue(ImportCalendarOption.Values.NO);
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
  public void run() {
    final File selectedFile = getFile();
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
    try {
      Document document = bufferProject.getDocumentManager().getDocument(selectedFile.getAbsolutePath());
      AlgorithmCollection algs = getProject().getTaskManager().getAlgorithmCollection();
      try {
        algs.getScheduler().setEnabled(false);
        algs.getRecalculateTaskScheduleAlgorithm().setEnabled(false);
        algs.getAdjustTaskBoundsAlgorithm().setEnabled(false);
        document.read();
      } finally {
        algs.getRecalculateTaskScheduleAlgorithm().setEnabled(true);
        algs.getAdjustTaskBoundsAlgorithm().setEnabled(true);
        algs.getScheduler().setEnabled(true);
      }

      importBufferProject(targetProject, bufferProject, getUiFacade(), myMergeResourcesOption, myImportCalendarOption);
    } catch (DocumentException e) {
      getUiFacade().showErrorDialog(e);
    } catch (IOException e) {
      getUiFacade().showErrorDialog(e);
    }
  }

  private static class TaskFieldImpl implements ColumnList.Column {
    private final String myID;
    private final int myOrder;
    private final int myWidth;

    TaskFieldImpl(String id, int order, int width) {
      myID = id;
      myOrder = order;
      myWidth = width;
    }

    @Override
    public SortOrder getSort() {
      return SortOrder.UNSORTED;
    }

    @Override
    public void setSort(SortOrder sort) {

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

  public static class VisibleFieldsImpl implements ColumnList {
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
    public void importData(ColumnList source) {
      for (int i = 0; i < source.getSize(); i++) {
        Column nextField = source.getField(i);
        myFields.add(nextField);
      }
    }
  }

  private BufferProject createBufferProject(final IGanttProject targetProject, final UIFacade uiFacade) {
    return new BufferProject(targetProject, uiFacade);
  }

  protected Document getDocument(File selectedFile) {
    return new FileDocument(selectedFile);
  }

  public static Map<Task, Task> importBufferProject(IGanttProject targetProject, BufferProject bufferProject, UIFacade uiFacade, MergeResourcesOption mergeOption, ImportCalendarOption importCalendarOption) {
    targetProject.getRoleManager().importData(bufferProject.getRoleManager());
    if (importCalendarOption != null) {
      targetProject.getActiveCalendar().importCalendar(bufferProject.getActiveCalendar(), importCalendarOption);
    }
    {
      CustomPropertyManager targetResCustomPropertyMgr = targetProject.getResourceCustomPropertyManager();
      targetResCustomPropertyMgr.importData(bufferProject.getResourceCustomPropertyManager());
    }
    Map<HumanResource, HumanResource> original2ImportedResource = targetProject.getHumanResourceManager().importData(
        bufferProject.getHumanResourceManager(), new OverwritingMerger(mergeOption));

    Map<Task, Task> result = null;
    {
      CustomPropertyManager targetCustomColumnStorage = targetProject.getTaskCustomColumnManager();
      Map<CustomPropertyDefinition, CustomPropertyDefinition> that2thisCustomDefs = targetCustomColumnStorage.importData(bufferProject.getTaskCustomColumnManager());
      TaskManagerImpl origTaskManager = (TaskManagerImpl) targetProject.getTaskManager();
      try {
        origTaskManager.setEventsEnabled(false);
        result = origTaskManager.importData(bufferProject.getTaskManager(), that2thisCustomDefs);
        origTaskManager.importAssignments(bufferProject.getTaskManager(), targetProject.getHumanResourceManager(),
            result, original2ImportedResource);
      } finally {
        origTaskManager.setEventsEnabled(true);
      }
    }
    uiFacade.refresh();
    uiFacade.getTaskTree().getVisibleFields().importData(bufferProject.getVisibleFields());
    uiFacade.getResourceTree().getVisibleFields().importData(bufferProject.myResourceVisibleFields);
    return result;
  }
}
