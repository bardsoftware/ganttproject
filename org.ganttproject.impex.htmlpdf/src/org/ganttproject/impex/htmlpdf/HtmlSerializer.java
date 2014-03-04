/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

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
package org.ganttproject.impex.htmlpdf;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import javax.xml.transform.sax.TransformerHandler;

import net.sourceforge.ganttproject.GPVersion;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.export.ExportException;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.FileUtil;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import biz.ganttproject.core.time.CalendarFactory;

/**
 * HTML-specific serializer.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class HtmlSerializer extends XmlSerializer {
  private ExporterToHTML myEngine;

  public HtmlSerializer(ExporterToHTML engine) {
    myEngine = engine;
  }

  void serialize(TransformerHandler handler, File outputFile) throws SAXException, IOException, ExportException {
    String filenameWithoutExtension = getFilenameWithoutExtension(outputFile);
    handler.startDocument();
    AttributesImpl attrs = new AttributesImpl();

    writeViews(getUIFacade(), handler);
    startElement("ganttproject", attrs, handler);
    textElement("title", attrs, "GanttProject - " + filenameWithoutExtension, handler);

    addAttribute("prefix", filenameWithoutExtension, attrs);
    startElement("links", attrs, handler);
    textElement("home", attrs, i18n("home"), handler);
    textElement("chart", attrs, i18n("gantt"), handler);
    textElement("tasks", attrs, i18n("task"), handler);
    textElement("resources", attrs, i18n("human"), handler);
    endElement("links", handler);

    startElement("project", attrs, handler);
    addAttribute("title", i18n("project"), attrs);
    textElement("name", attrs, getProject().getProjectName(), handler);
    addAttribute("title", i18n("organization"), attrs);
    textElement("organization", attrs, getProject().getOrganization(), handler);
    addAttribute("title", i18n("webLink"), attrs);
    textElement("webLink", attrs, getProject().getWebLink(), handler);
    addAttribute("title", i18n("shortDescription"), attrs);
    textElement("description", attrs, getProject().getDescription(), handler);
    endElement("project", handler);

    // TODO: [dbarashev, 10.09.2005] introduce output files grouping structure
    String ganttChartFileName = ExporterToHTML.replaceExtension(outputFile, ExporterToHTML.GANTT_CHART_FILE_EXTENSION).getName();
    textElement("chart", attrs, ganttChartFileName, handler);
    addAttribute("name", i18n("colName"), attrs);
    addAttribute("role", i18n("colRole"), attrs);
    addAttribute("mail", i18n("colMail"), attrs);
    addAttribute("phone", i18n("colPhone"), attrs);
    startElement("resources", attrs, handler);
    writeResources(getProject().getHumanResourceManager(), handler);

    String resourceChartFileName = ExporterToHTML.replaceExtension(outputFile,
        ExporterToHTML.RESOURCE_CHART_FILE_EXTENSION).getName();
    addAttribute("path", resourceChartFileName, attrs);
    emptyElement("chart", attrs, handler);
    endElement("resources", handler);

//    addAttribute("name", i18n("name"), attrs);
//    addAttribute("begin", i18n("start"), attrs);
//    addAttribute("end", i18n("end"), attrs);
//    addAttribute("milestone", i18n("meetingPoint"), attrs);
//    addAttribute("progress", i18n("advancement"), attrs);
//    addAttribute("assigned-to", i18n("assignTo"), attrs);
//    addAttribute("notes", i18n("notesTask"), attrs);
    try {
      writeTasks(getProject().getTaskManager(), handler);
    } catch (Exception e) {
      throw new ExportException("Failed to write tasks", e);
    }

    addAttribute("version", "Ganttproject (" + GPVersion.CURRENT + ")", attrs);
    Calendar c = CalendarFactory.newCalendar();
    String dateAndTime = GanttLanguage.getInstance().formatShortDate(c) + " - " + GanttLanguage.getInstance().formatTime(c);

    addAttribute("date", dateAndTime, attrs);
    emptyElement("footer", attrs, handler);
    endElement("ganttproject", handler);
    handler.endDocument();
  }

  private IGanttProject getProject() {
    return myEngine.getProject();
  }

  private UIFacade getUIFacade() {
    return myEngine.getUIFacade();
  }

  @Override
  protected String getAssignedResourcesDelimiter() {
    return ", ";
  }

  private static String getFilenameWithoutExtension(File f) {
    return FileUtil.getFilenameWithoutExtension(f);
  }
}
