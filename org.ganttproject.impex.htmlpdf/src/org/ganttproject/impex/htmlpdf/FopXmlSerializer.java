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

import java.text.DateFormat;
import java.util.Locale;

import javax.xml.transform.sax.TransformerHandler;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.export.ExportException;

import org.ganttproject.impex.htmlpdf.FOPEngine.ExportState;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import biz.ganttproject.core.time.CalendarFactory;

/**
 * FOP-specific serializer.
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class FopXmlSerializer extends XmlSerializer {
  private final FOPEngine myFopEngine;

  public FopXmlSerializer(FOPEngine fopEngine) {
    myFopEngine = fopEngine;
  }

  protected void exportProject(ExportState state, TransformerHandler handler) throws SAXException, ExportException {
    DateFormat df = java.text.DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
    handler.startDocument();
    AttributesImpl attrs = new AttributesImpl();
    addAttribute("xmlns:xsl", "http://www.w3.org/1999/XSL/Transform", attrs);
    addAttribute("xmlns:ganttproject", "http://ganttproject.sf.net/", attrs);
    addAttribute("version", "1.0", attrs);
    startElement("xsl:stylesheet", attrs, handler);
    // handler.startPrefixMapping("ganttproject",
    // "http://ganttproject.sf.net");

    writeViews(myFopEngine.getUiFacade(), handler);

    addAttribute("xslfo-path", myFopEngine.getSelectedStylesheet().getUrl().getPath(), attrs);
    startPrefixedElement("report", attrs, handler);
    addAttribute("xslfo-path", myFopEngine.getSelectedStylesheet().getUrl().getPath(), attrs);
    addAttribute("title", i18n("ganttReport"), attrs);
    addAttribute("name", i18n("project"), attrs);
    addAttribute("nameValue", getProject().getProjectName(), attrs);
    addAttribute("organisation", i18n("organization"), attrs);
    addAttribute("organisationValue", getProject().getOrganization(), attrs);
    addAttribute("webLink", i18n("webLink"), attrs);
    addAttribute("webLinkValue", getProject().getWebLink(), attrs);
    addAttribute("currentDateTimeValue", df.format(new java.util.Date()), attrs);
    addAttribute("description", i18n("shortDescription"), attrs);

    addAttribute("begin", i18n("start"), attrs);
    addAttribute("beginValue", CalendarFactory.createGanttCalendar(getProject().getTaskManager().getProjectStart()).toString(), attrs);

    addAttribute("end", i18n("end"), attrs);
    addAttribute("endValue", CalendarFactory.createGanttCalendar(getProject().getTaskManager().getProjectEnd()).toString(), attrs);

    startPrefixedElement("project", attrs, handler);
    textElement("descriptionValue", attrs, getProject().getDescription(), handler);
    endPrefixedElement("project", handler);
    writeCharts(state, handler);
    writeTasks(getProject().getTaskManager(), handler);
    writeResources(getProject().getHumanResourceManager(), handler);
    endPrefixedElement("report", handler);
    // handler.endPrefixMapping("ganttproject");
    endElement("xsl:stylesheet", handler);
    handler.endDocument();
  }

  private void writeCharts(ExportState state, TransformerHandler handler) throws SAXException {
    AttributesImpl attrs = new AttributesImpl();
    addAttribute("title", i18n("ganttChart"), attrs);
    addAttribute("src", state.ganttChartImageFile.getAbsolutePath(), attrs);
    startPrefixedElement("ganttchart", attrs, handler);
    endPrefixedElement("ganttchart", handler);

    addAttribute("title", i18n("resourcesChart"), attrs);
    addAttribute("src", state.resourceChartImageFile.getAbsolutePath(), attrs);
    startPrefixedElement("resourceschart", attrs, handler);
    endPrefixedElement("resourceschart", handler);
  }

  private IGanttProject getProject() {
    return myFopEngine.getProject();

  }

  @Override
  protected String getAssignedResourcesDelimiter() {
    return "\n\r";
  }

}
