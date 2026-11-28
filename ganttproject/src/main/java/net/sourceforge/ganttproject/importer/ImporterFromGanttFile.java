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
import net.sourceforge.ganttproject.GanttPreviousState;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.FileDocument;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.HumanResourceMerger;
import net.sourceforge.ganttproject.resource.MergeResourcesEnum;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmCollection;
import org.osgi.service.prefs.Preferences;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static net.sourceforge.ganttproject.importer.BaselineImportKt.baselineNameCollisions;
import static net.sourceforge.ganttproject.importer.BaselineImportKt.importBaselines;
import static net.sourceforge.ganttproject.importer.BufferProjectImportKt.importBufferProject;

public class ImporterFromGanttFile extends ImporterBase {
  private final HumanResourceMerger.MergeResourcesOption myMergeResourcesOption = new HumanResourceMerger.MergeResourcesOption();
  private final ImportCalendarOption myImportCalendarOption = new ImportCalendarOption();

  private final GPOption[] myOptions = new GPOption[]{myMergeResourcesOption, myImportCalendarOption};
  private Function<List<String>, BaselineNameCollision> myBaselineNameCollisionQuestion =
      BaselineNameCollisionDialogKt::askBaselineNameCollision;

  public ImporterFromGanttFile() {
    super("ganttprojectFiles");
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
    myMergeResourcesOption.loadPersistentValue(node.get(myMergeResourcesOption.getID(), MergeResourcesEnum.DEFAULT.name().toLowerCase()));
    myMergeResourcesOption.commit();
    myMergeResourcesOption.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        node.put(myMergeResourcesOption.getID(), String.valueOf(event.getNewValue()));
      }
    });
  }

  /**
   * Replaces the dialog which asks what to do with imported baselines whose names are already
   * taken. The function gets the taken names and returns null if the import shall be cancelled.
   */
  void setBaselineNameCollisionQuestion(Function<List<String>, BaselineNameCollision> question) {
    myBaselineNameCollisionQuestion = question;
  }

  @Override
  public void run() {
    final IGanttProject targetProject = getProject();
    // The files are read and the question is asked before the undoable edit starts, so that
    // cancelling the question changes nothing and leaves no undo entry.
    final List<BufferProject> bufferProjects = new ArrayList<>();
    getModel().getFiles().forEach( file -> {
      final BufferProject bufferProject = createBufferProject(targetProject, getUiFacade());
      if (read(file, bufferProject)) {
        bufferProjects.add(bufferProject);
      }
    });
    List<List<GanttPreviousState>> sourceBaselines = new ArrayList<>();
    bufferProjects.forEach(bufferProject -> sourceBaselines.add(bufferProject.getBaselines()));
    List<String> collisions = baselineNameCollisions(sourceBaselines, targetProject.getBaselines());
    final BaselineNameCollision onNameCollision;
    if (collisions.isEmpty()) {
      onNameCollision = BaselineNameCollision.RENAME;
    } else {
      onNameCollision = myBaselineNameCollisionQuestion.apply(collisions);
      if (onNameCollision == null) {
        return;
      }
    }
    getUiFacade().getUndoManager().undoableEdit("Import", new Runnable() {
      @Override
      public void run() {
        bufferProjects.forEach(bufferProject -> importBuffer(targetProject, bufferProject, onNameCollision));
      }
    });
  }

  private boolean read(File selectedFile, BufferProject bufferProject) {
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
      return true;
    } catch (Exception  e) {
      getUiFacade().showErrorDialog(e);
      return false;
    }
  }

  private void importBuffer(IGanttProject targetProject, BufferProject bufferProject, BaselineNameCollision onNameCollision) {
    try {
      Map<Task, Task> taskMapping = importBufferProject(targetProject, bufferProject,
          BufferProjectImportKt.asImportBufferProjectApi(getUiFacade()), myMergeResourcesOption, myImportCalendarOption, false);
      importBaselines(bufferProject.getBaselines(), targetProject.getBaselines(), taskMapping, onNameCollision);
    } catch (Exception  e) {
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
    public void importData(ColumnList source, boolean keepVisibleColumnsIgnored) {
      for (int i = 0; i < source.getSize(); i++) {
        Column nextField = source.getField(i);
        myFields.add(nextField);
      }
    }

    @Override
    public List<Column> exportData() {
      return List.copyOf(myFields);
    }
  }

  private BufferProject createBufferProject(final IGanttProject targetProject, final UIFacade uiFacade) {
    return new BufferProject(targetProject, uiFacade);
  }

  protected Document getDocument(File selectedFile) {
    return new FileDocument(selectedFile);
  }

}
