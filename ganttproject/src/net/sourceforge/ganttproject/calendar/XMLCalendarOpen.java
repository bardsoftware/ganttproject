/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.calendar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sourceforge.ganttproject.parser.FileFormatException;
import net.sourceforge.ganttproject.parser.ParsingListener;
import net.sourceforge.ganttproject.parser.TagHandler;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import biz.ganttproject.core.calendar.GPCalendar;

/**
 * @author nbohn
 */
public class XMLCalendarOpen {
  public static class MyException extends Exception {
    MyException(Throwable cause) {
      super(cause);
    }
  }

  private List<URL> myCalendarResources = new ArrayList<URL>();
  private String myCalendarLabels[];

  private ArrayList<TagHandler> myTagHandlers = new ArrayList<TagHandler>();

  private ArrayList<ParsingListener> myListeners = new ArrayList<ParsingListener>();

  public boolean load(InputStream inputStream) throws MyException {
    // Use an instance of ourselves as the SAX event handler
    DefaultHandler handler = new GanttXMLParser();

    // Use the default (non-validating) parser
    SAXParserFactory factory = SAXParserFactory.newInstance();

    try {
      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(inputStream, handler);
    } catch (ParserConfigurationException e) {
      throw new MyException(e);
    } catch (SAXException e) {
      throw new MyException(e);
    } catch (IOException e) {
      throw new MyException(e);
    }
    return true;
  }

  public void addTagHandler(TagHandler handler) {
    myTagHandlers.add(handler);
  }

  public void addParsingListener(ParsingListener listener) {
    myListeners.add(listener);
  }

  private TagHandler getDefaultTagHandler() {
    return new DefaultTagHandler();
  }

  private class DefaultTagHandler implements TagHandler {
    private String name;

    @Override
    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs) {
      if (attrs != null) {
        String eName = qName; // element name
        for (int i = 0; i < attrs.getLength(); i++) {
          String aName = attrs.getLocalName(i); // Attr name
          if ("".equals(aName)) {
            aName = attrs.getQName(i);
          }
          if (eName.equals("calendar")) {
            if (aName.equals("name")) {
              name = attrs.getValue(i);
            } else if (aName.equals("type")) {
            }
          }
        }
      }
    }

    @Override
    public void endElement(String namespaceURI, String sName, String qName) {
    }

    public String getName() {
      return name;
    }
  }

  private class GanttXMLParser extends DefaultHandler {
    // ===========================================================
    // SAX DocumentHandler methods
    // ===========================================================

    @Override
    public void endDocument() throws SAXException {
      for (int i = 0; i < myListeners.size(); i++) {
        ParsingListener l = myListeners.get(i);
        l.parsingFinished();
      }
    }

    @Override
    public void startElement(String namespaceURI, String sName, // simple
        // name
        String qName, // qualified name
        Attributes attrs) throws SAXException {
      for (Iterator<TagHandler> handlers = myTagHandlers.iterator(); handlers.hasNext();) {
        TagHandler next = handlers.next();
        try {
          next.startElement(namespaceURI, sName, qName, attrs);
        } catch (FileFormatException e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public void endElement(String namespaceURI, String sName, String qName) throws SAXException {
      for (Iterator<TagHandler> handlers = myTagHandlers.iterator(); handlers.hasNext();) {
        TagHandler next = handlers.next();
        next.endElement(namespaceURI, sName, qName);
      }
    }
  }

  public void setCalendars() throws MyException {
    myCalendarResources.clear();
    DefaultTagHandler th = (DefaultTagHandler) getDefaultTagHandler();
    addTagHandler(th);
    IConfigurationElement[] calendarExtensions = Platform.getExtensionRegistry().getConfigurationElementsFor(
        GPCalendar.EXTENSION_POINT_ID);
    myCalendarLabels = new String[calendarExtensions.length];
    for (int i = 0; i < calendarExtensions.length; i++) {
      Bundle nextBundle = Platform.getBundle(calendarExtensions[i].getDeclaringExtension().getNamespaceIdentifier());
      URL calendarUrl = nextBundle.getResource(calendarExtensions[i].getAttribute("resource-url"));
      if (calendarUrl != null) {
        try {
          load(calendarUrl.openStream());
        } catch (IOException e) {
          throw new MyException(e);
        }
        myCalendarLabels[i] = th.getName();
        myCalendarResources.add(calendarUrl);
      }
    }
  }

  public URL[] getCalendarResources() {
    return myCalendarResources.toArray(new URL[0]);
  }

  public String[] getLabels() {
    return myCalendarLabels;
  }
}