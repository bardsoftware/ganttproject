/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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
package net.sourceforge.ganttproject.action.edit;

import kotlin.Unit;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.gantt.ClipboardContentsKt;
import net.sourceforge.ganttproject.chart.gantt.GanttChartSelection;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.view.GPViewManager;
import net.sourceforge.ganttproject.importer.BufferProject;
import net.sourceforge.ganttproject.importer.BufferProjectImportKt;
import net.sourceforge.ganttproject.resource.HumanResourceMerger;
import net.sourceforge.ganttproject.resource.MergeResourcesEnum;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.undo.GPUndoManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.util.function.Supplier;

import static net.sourceforge.ganttproject.importer.BufferProjectImportKt.importBufferProject;

public class PasteAction extends GPAction {
  private final GPViewManager myViewmanager;
  private final Supplier<GPUndoManager> myUndoManager;
  private final IGanttProject myProject;
  private final UIFacade myUiFacade;

  public PasteAction(IGanttProject project, UIFacade uiFacade, GPViewManager viewManager, Supplier<GPUndoManager> undoManager) {
    super("paste");
    myViewmanager = viewManager;
    myUndoManager = undoManager;
    myProject = project;
    myUiFacade = uiFacade;
    ClipboardContentsKt.onClipboardChange(isAvailable -> {
      updateAction();
      return Unit.INSTANCE;
    });
  }

  @Override
  public void updateAction() {
    super.updateAction();
    setEnabled(canPaste(myViewmanager.getSelectedArtefacts()));
  }

  /**
   * Paste is possible if the selection has its own clipboard contents, or if the system clipboard has data which
   * can be pasted into the active chart. The clipboard contents copied in this GanttProject instance may be not
   * acceptable, e.g. copied resources can't be pasted into the task table.
   */
  private boolean canPaste(ChartSelection selection) {
    if (!selection.isEmpty() && selection.hasOwnClipboardContents()) {
      return true;
    }
    if (!ClipboardContentsKt.hasClipboardData()) {
      return false;
    }
    var ownClipboardContents = ClipboardContentsKt.getOwnClipboardContents();
    return ownClipboardContents == null || selection.acceptsClipboardContents(ownClipboardContents);
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    if (calledFromAppleScreenMenu(evt)) {
      return;
    }
    ChartSelection selection = myViewmanager.getSelectedArtefacts();
    if (!selection.isEmpty() && selection.hasOwnClipboardContents()) {
      pasteInternalFlavor(selection);
      return;
    }
    var ownClipboardContents = ClipboardContentsKt.getOwnClipboardContents();
    if (ownClipboardContents != null && !selection.acceptsClipboardContents(ownClipboardContents)) {
      // The clipboard contents were copied in this GanttProject instance, but they can't be pasted into the active
      // chart, e.g. resources copied in the resource chart and pasted into the task table.
      return;
    }
    // The clipboard contents came from elsewhere, e.g. from another GanttProject window, or from another chart
    // with no selection. We import them into the top level or, if the user chooses so, paste them after the selected
    // task. See issue https://github.com/bardsoftware/ganttproject/issues/2861
    var clipboardProject = ClipboardContentsKt.getProjectFromClipboard(new BufferProject(myProject, myUiFacade));
    if (clipboardProject == null) {
      myUiFacade.getActiveChart().focus();
      return;
    }
    Task pasteTarget = selection instanceof GanttChartSelection ganttChartSelection ? ganttChartSelection.getPasteTarget() : null;
    if (pasteTarget == null || clipboardProject.getTaskManager().getTaskHierarchy().getNestedTasks(clipboardProject.getTaskManager().getRootTask()).length == 0) {
      pasteExternalDocument(clipboardProject, null);
      return;
    }
    PasteExternalDialogKt.showPasteExternalDialog(choice -> {
      SwingUtilities.invokeLater(() -> {
        switch (choice) {
          case AFTER_SELECTED -> pasteExternalDocument(clipboardProject, pasteTarget);
          case TOP_LEVEL -> pasteExternalDocument(clipboardProject, null);
          case CANCEL -> {}
        }
      });
      return Unit.INSTANCE;
    });
  }

  /**
   * Imports the clipboard project into the top level of the task tree and, if pasteTarget is not null,
   * moves the imported top-level tasks after the paste target.
   */
  private void pasteExternalDocument(BufferProject clipboardProject, @Nullable Task pasteTarget) {
    try {
      myUndoManager.get().undoableEdit(getLocalizedName(), () -> {
        HumanResourceMerger.MergeResourcesOption mergeOption = new HumanResourceMerger.MergeResourcesOption();
        mergeOption.setSelectedValue(MergeResourcesEnum.NO);
        var taskMapping = importBufferProject(myProject, clipboardProject,
            BufferProjectImportKt.asImportBufferProjectApi(myUiFacade), mergeOption, null, false);
        if (pasteTarget != null) {
          ClipboardContentsKt.moveImportedTasksAfter(pasteTarget, clipboardProject, taskMapping);
        }
      });
    } catch (Exception e) {
      GPLogger.log(e);
    }
    myUiFacade.getActiveChart().focus();
  }

  private void pasteInternalFlavor(final ChartSelection selection) {
    myUndoManager.get().undoableEdit(getLocalizedName(), selection::commitClipboardTransaction);
  }

  @Override
  public PasteAction asToolbarAction() {
    final PasteAction result = new PasteAction(myProject, myUiFacade, myViewmanager, myUndoManager);
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    this.addPropertyChangeListener(evt -> {
      if ("enabled".equals(evt.getPropertyName())) {
        result.setEnabled((Boolean) evt.getNewValue());
      }
    });
    result.setEnabled(this.isEnabled());
    return result;
  }
}
