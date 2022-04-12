/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2002-2011 Thomas Alexandre, GanttProject Team

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
package net.sourceforge.ganttproject.io;

import biz.ganttproject.core.io.XmlProject;
import biz.ganttproject.core.time.GanttCalendar;
import net.sourceforge.ganttproject.PrjInfos;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.parser.*;
import net.sourceforge.ganttproject.task.TaskManager;
import org.xml.sax.Attributes;

import java.io.*;
import java.util.ArrayList;

/**
 * Allows to load a gantt file from xml format, using SAX parser
 */
public class GanttXMLOpen implements GPParser {
  private final ArrayList<TagHandler> myTagHandlers = new ArrayList<>();

  private final ArrayList<ParsingListener> myListeners = new ArrayList<>();

  private final ParsingContext myContext;

  private final TaskManager myTaskManager;

  private PrjInfos myProjectInfo = null;

  private UIFacade myUIFacade = null;

  public GanttXMLOpen(PrjInfos info, TaskManager taskManager, UIFacade uiFacade) {
    this(taskManager);
    myProjectInfo = info;
    myUIFacade = uiFacade;
  }

  public GanttXMLOpen(TaskManager taskManager) {
    myContext = new ParsingContext();
    myTaskManager = taskManager;
  }

  @Override
  public boolean load(InputStream inStream) throws IOException {
    return doLoad(inStream);
  }

  public boolean doLoad(InputStream inStream) throws IOException {
    XmlParser parser = new XmlParser(myTagHandlers, myListeners);
    parser.parse(inStream);
    return true;

  }

  // This is a very special method which is used for loading role sets from the options file.
  public boolean load(File file) {
    XmlParser parser = new XmlParser(myTagHandlers, myListeners);
    try {
      parser.parse(new BufferedInputStream(new FileInputStream(file)));
    } catch (Exception e) {
      myUIFacade.showErrorDialog(e);
      return false;
    }
    return true;
  }

  @Override
  public void addTagHandler(TagHandler handler) {
    myTagHandlers.add(handler);
  }

  @Override
  public void addParsingListener(ParsingListener listener) {
    myListeners.add(listener);
  }

  @Override
  public ParsingContext getContext() {
    return myContext;
  }

  @Override
  public TagHandler getDefaultTagHandler() {
    return new DefaultTagHandler();
  }

  /**
   * Processes project and view attributes, timeline tasks and (because of histerical raisins) task notes.
   */
  private class DefaultTagHandler extends AbstractTagHandler {
    DefaultTagHandler() {
      super(null, true);
    }

    @Override
    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs) throws FileFormatException {
    }

    @Override
    public void process(XmlProject xmlProject) {
      myProjectInfo.setName(xmlProject.getName());
      myProjectInfo.setOrganization(xmlProject.getCompany());
      myProjectInfo.setDescription(xmlProject.getDescription());
      myProjectInfo.setWebLink(xmlProject.getWebLink());
      var viewDate = xmlProject.getViewDate();
      if (!viewDate.isBlank()) {
        myUIFacade.getScrollingManager().scrollTo(GanttCalendar.parseXMLDate(viewDate).getTime());
      }
      myUIFacade.setViewIndex(xmlProject.getViewIndex());
      myUIFacade.setGanttDividerLocation(xmlProject.getGanttDividerLocation());
      myUIFacade.setResourceDividerLocation(xmlProject.getResourceDividerLocation());
      myTaskManager.setZeroMilestones(xmlProject.getTasks().getEmptyMilestones());
    }
  }
}