/*
Copyright 2018 BarD Software s.r.o

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
package net.sourceforge.ganttproject;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import net.sourceforge.ganttproject.chart.gantt.ClipboardContents;
import net.sourceforge.ganttproject.chart.gantt.ClipboardTaskProcessor;
import net.sourceforge.ganttproject.io.GanttXMLSaver;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Set;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPTransferable implements Transferable {
  public static final DataFlavor EXTERNAL_DOCUMENT_FLAVOR = new DataFlavor("application/x-ganttproject", "GanttProject XML File");
  static final DataFlavor EXTERNAL_TEXT_FLAVOR = new DataFlavor("text/plain", "GanttProject Task List");
  static DataFlavor INTERNAL_DATA_FLAVOR;
  private static Set<DataFlavor> ourFlavors;
  static {
    try {
      String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + ClipboardContents.class.getName() + "\"";
      INTERNAL_DATA_FLAVOR = new DataFlavor(mimeType);
      ourFlavors = ImmutableSet.of(INTERNAL_DATA_FLAVOR, EXTERNAL_DOCUMENT_FLAVOR, EXTERNAL_TEXT_FLAVOR);
    } catch (ClassNotFoundException e) {
      System.out.println("ClassNotFound: " + e.getMessage());
    }
  }


  private final ClipboardContents myClipboardContents;

  public GPTransferable(ClipboardContents contents) {
    myClipboardContents = contents;
  }

  @Override
  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
    if (INTERNAL_DATA_FLAVOR.equals(flavor)) {
      return myClipboardContents;
    }
    if (EXTERNAL_TEXT_FLAVOR.equals(flavor)) {
      return createTextFlavor();
    }
    if (EXTERNAL_DOCUMENT_FLAVOR.equals(flavor)) {
      return createDocumentFlavor();
    }
    throw new UnsupportedFlavorException(flavor);
  }

  private InputStream createDocumentFlavor() {
    IGanttProject bufferProject = new GanttProjectImpl();
    final TaskManager taskMgr = bufferProject.getTaskManager();
    ClipboardTaskProcessor processor = new ClipboardTaskProcessor(taskMgr);
    // In intra-document copy+paste we do copy so-called external dependencies (those where one of the tasks is not in
    // the clipboard). However, we do not want to copy them into the system clipboard because in the target project
    // external task may not exist or may exist and be not the one we want.
    processor.setTruncateExternalDeps(true);
    // We also do not copy assignments into the system clipboard.
    processor.setTruncateAssignments(true);
    processor.pasteAsChild(taskMgr.getRootTask(), myClipboardContents);

    for (HumanResource res : myClipboardContents.getResources()) {
      bufferProject.getHumanResourceManager().add(res.unpluggedClone());
    }
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      GanttXMLSaver saver = new GanttXMLSaver(bufferProject);
      saver.save(out);
      out.flush();
      return new ByteArrayInputStream(out.toByteArray());
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private InputStream createTextFlavor() {
    StringBuilder builder = new StringBuilder();
    for (Task t : myClipboardContents.getTasks()) {
      builder.append(t.getName()).append(System.getProperty("line.separator"));
    }
    return new ByteArrayInputStream(builder.toString().getBytes(Charsets.UTF_8));
  }

  @Override
  public DataFlavor[] getTransferDataFlavors() {
    return ourFlavors.toArray(new DataFlavor[3]);
  }

  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return ourFlavors.contains(flavor);
  }
}
