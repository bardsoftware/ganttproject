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

import biz.ganttproject.core.time.GanttCalendar;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.PrjInfos;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.parser.AbstractTagHandler;
import net.sourceforge.ganttproject.parser.GPParser;
import net.sourceforge.ganttproject.parser.ParsingContext;
import net.sourceforge.ganttproject.parser.ParsingListener;
import net.sourceforge.ganttproject.parser.TagHandler;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import org.xml.sax.Attributes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Allows to load a gantt file from xml format, using SAX parser
 */
public class GanttXMLOpen implements GPParser {
  /** 0-->description of project, 1->note for task */
  int typeChar = -1;

  private final ArrayList<TagHandler> myTagHandlers = new ArrayList<TagHandler>();

  private final ArrayList<ParsingListener> myListeners = new ArrayList<ParsingListener>();

  private final ParsingContext myContext;

  private final TaskManager myTaskManager;

  private int viewIndex;

  private int ganttDividerLocation;

  private int resourceDividerLocation;

  private PrjInfos myProjectInfo = null;

  private UIFacade myUIFacade = null;

  private UIConfiguration myUIConfig;

  private TagHandler myTimelineTagHandler = new TimelineTagHandler();

  public GanttXMLOpen(PrjInfos info, UIConfiguration uiConfig, TaskManager taskManager, UIFacade uiFacade) {
    this(taskManager);
    myProjectInfo = info;
    myUIConfig = uiConfig;
    this.viewIndex = 0;

    this.ganttDividerLocation = 300; // TODO is this arbitrary value right ?
    this.resourceDividerLocation = 300;
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
    // Use an instance of ourselves as the SAX event handler
    XmlParser parser = new XmlParser(myTagHandlers, myListeners);
    parser.parse(inStream);
    myUIFacade.setViewIndex(viewIndex);
    myUIFacade.setGanttDividerLocation(ganttDividerLocation);
    if (resourceDividerLocation != 0) {
      myUIFacade.setResourceDividerLocation(resourceDividerLocation);
    }
    return true;

  }

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

  private class DefaultTagHandler extends AbstractTagHandler {
    private final Set<String> myTags = ImmutableSet.of("project", "tasks", "description", "notes");
    private boolean hasCdata = false;

    DefaultTagHandler() {
      super(null, true);
    }

    @Override
    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs) {
      clearCdata();
      String eName = sName; // element name
      if ("".equals(eName)) {
        eName = qName; // not namespace aware
      }
      setTagStarted(myTags.contains(eName));
      hasCdata = "description".equals(eName) || "notes".equals(eName);
      if (eName.equals("tasks")) {
        myTaskManager.setZeroMilestones(null);
      }
      if (attrs != null) {
        for (int i = 0; i < attrs.getLength(); i++) {
          String aName = attrs.getLocalName(i); // Attr name
          if ("".equals(aName)) {
            aName = attrs.getQName(i);
            // The project part
          }
          if (eName.equals("project")) {
            if (aName.equals("name")) {
              myProjectInfo.setName(attrs.getValue(i));
            } else if (aName.equals("company")) {
              myProjectInfo.setOrganization(attrs.getValue(i));
            } else if (aName.equals("webLink")) {
              myProjectInfo.setWebLink(attrs.getValue(i));
            }
            // TODO: 1.12 repair scrolling to the saved date
            else if (aName.equals("view-date")) {
              myUIFacade.getScrollingManager().scrollTo(GanttCalendar.parseXMLDate(attrs.getValue(i)).getTime());
            } else if (aName.equals("view-index")) {
              viewIndex = new Integer(attrs.getValue(i)).hashCode();
            } else if (aName.equals("gantt-divider-location")) {
              ganttDividerLocation = new Integer(attrs.getValue(i)).intValue();
            } else if (aName.equals("resource-divider-location")) {
              resourceDividerLocation = new Integer(attrs.getValue(i)).intValue();
            }
          } else if (eName.equals("tasks")) {
            if ("empty-milestones".equals(aName)) {
              myTaskManager.setZeroMilestones(Boolean.parseBoolean(attrs.getValue(i)));
            }
          }
        }
      }
    }

    @Override
    public void endElement(String namespaceURI, String sName, String qName) {
      if (!myTags.contains(qName)) {
        return;
      }
      if ("description".equals(qName)) {
        myProjectInfo.setDescription(getCdata());
      } else if ("notes".equals(qName)) {
        Task currentTask = getContext().peekTask();
        currentTask.setNotes(getCdata());
      }
      hasCdata = false;
      setTagStarted(false);
    }

    @Override
    public boolean hasCdata() {
      return hasCdata;
    }

    @Override
    public void appendCdata(String cdata) {
      super.appendCdata(cdata);
    }
  }

  @Override
  public TagHandler getTimelineTagHandler() {
    return myTimelineTagHandler;
  }

  class TimelineTagHandler extends AbstractTagHandler implements ParsingListener {
    private final List<Integer> myIds = Lists.newArrayList();

    public TimelineTagHandler() {
      super("timeline", true);
    }

    @Override
    public void parsingStarted() {
    }

    @Override
    public void parsingFinished() {
      myUIFacade.getCurrentTaskView().getTimelineTasks().clear();
      for (Integer id : myIds) {
        Task t = myTaskManager.getTask(id);
        if (t != null) {
          myUIFacade.getCurrentTaskView().getTimelineTasks().add(t);
        }
      }
    }

    @Override
    protected boolean onStartElement(Attributes attrs) {
      clearCdata();
      return super.onStartElement(attrs);
    }

    @Override
    protected void onEndElement() {
      String[] ids = getCdata().split(",");
      for (String id : ids) {
        try {
          myIds.add(Integer.valueOf(id.trim()));
        } catch (NumberFormatException e) {
          GPLogger.logToLogger(e);
        }
      }
      clearCdata();
    }
  }
}
