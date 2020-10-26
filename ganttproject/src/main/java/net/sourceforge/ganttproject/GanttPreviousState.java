/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Dmitry Barashev

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
package net.sourceforge.ganttproject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import net.sourceforge.ganttproject.io.HistorySaver;
import net.sourceforge.ganttproject.io.SaverBase;
import net.sourceforge.ganttproject.parser.PreviousStateTasksTagHandler;
import net.sourceforge.ganttproject.task.Task;

import net.sourceforge.ganttproject.task.TaskManager;
import org.xml.sax.SAXException;

/**
 * @author nbohn
 */
public class GanttPreviousState {
  private final List<GanttPreviousStateTask> myTasks;

  private String myName;

  private File myFile;

  public GanttPreviousState(String name, List<GanttPreviousStateTask> tasks) {
    myName = name;
    myTasks = tasks;
  }

  public void init() throws IOException {
    myFile = createTemporaryFile();
    myFile.deleteOnExit();
  }

  public void setName(String name) {
    myName = name;
  }

  private class BaselineSaver extends SaverBase {
    void save(File file, List<GanttPreviousStateTask> tasks) throws TransformerConfigurationException, SAXException {
      StreamResult result = new StreamResult(file);
      TransformerHandler handler = createHandler(result);
      HistorySaver saver = new HistorySaver();
      handler.startDocument();
      saver.saveBaseline(myName, tasks, handler);
      handler.endDocument();
    }
  }

  public void saveFile() throws IOException {
    BaselineSaver saver = new BaselineSaver();
    try {
      saver.save(myFile, myTasks);
    } catch (TransformerConfigurationException e) {
      throw new IOException(e);
    } catch (SAXException e) {
      throw new IOException(e);
    }
  }

  private static File createTemporaryFile() throws IOException {
    String fileName = "_GanttProject_ps_" + (int) (10000. * Math.random());
    return File.createTempFile(fileName, ".gan");
  }

  public String getName() {
    return myName;
  }

  public void remove() {
    myFile.delete();
  }

  public List<GanttPreviousStateTask> load() {
    ArrayList<GanttPreviousStateTask> tasks = null;
    PreviousStateTasksTagHandler handler = new PreviousStateTasksTagHandler();
    SAXParserFactory factory = SAXParserFactory.newInstance();
    try {
      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(myFile, handler);
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      return null;
    } catch (SAXException e) {
      e.printStackTrace();
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    tasks = handler.getTasks();
    return tasks;
  }

  public static List<GanttPreviousStateTask> createTasks(TaskManager taskManager) {
    List<GanttPreviousStateTask> result = new ArrayList<GanttPreviousStateTask>();
    for (Task t : taskManager.getTasks()) {
      GanttPreviousStateTask baselineTask = new GanttPreviousStateTask(t.getTaskID(), t.getStart(),
          t.getDuration().getLength(), t.isMilestone(), taskManager.getTaskHierarchy().hasNestedTasks(t));
      result.add(baselineTask);
    }
    return result;
  }
}
