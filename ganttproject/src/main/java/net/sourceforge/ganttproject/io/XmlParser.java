/*
Copyright 2014 BarD Software s.r.o
Copyright 2003-2013 GanttProject Team

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
package net.sourceforge.ganttproject.io;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.parser.FileFormatException;
import net.sourceforge.ganttproject.parser.ParsingListener;
import net.sourceforge.ganttproject.parser.TagHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * SAX parser which delegates parsing semantics to TagHandler instances.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class XmlParser extends DefaultHandler2 {
  private final List<TagHandler> myTagHandlers;
  private final List<ParsingListener> myListeners;
  private boolean myCdataStarted;

  public XmlParser(List<TagHandler> tagHandlers, List<ParsingListener> listeners) {
    myTagHandlers = tagHandlers;
    myListeners = listeners;
  }

  @Override
  public void startDocument() throws SAXException {
    super.startDocument();
  }

  @Override
  public void endDocument() {
    for (ParsingListener l : myListeners) {
      l.parsingFinished();
    }
  }

  @Override
  public void startElement(String namespaceURI, String sName, // simple
      // name
      String qName, // qualified name
      Attributes attrs) {
    for (TagHandler next : myTagHandlers) {
      try {
        next.startElement(namespaceURI, sName, qName, attrs);
      } catch (FileFormatException e) {
        System.err.println(e.getMessage());
      }
    }
  }

  @Override
  public void endElement(String namespaceURI, String sName, String qName) {
    for (TagHandler next : myTagHandlers) {
      next.endElement(namespaceURI, sName, qName);
    }
  }

  @Override
  public void startCDATA() {
    myCdataStarted = true;
  }

  @Override
  public void endCDATA() {
    myCdataStarted = false;
  }

  @Override
  public void characters(char buf[], int offset, int len) {
    if (!myCdataStarted) {
      return;
    }
    String s = new String(buf, offset, len);
    for (TagHandler tagHandler : myTagHandlers) {
      if (tagHandler.hasCdata()) {
        tagHandler.appendCdata(s);
      }
    }
  }

  public void parse(InputStream inStream) throws IOException {
    // Use the default (non-validating) parser
    SAXParserFactory factory = SAXParserFactory.newInstance();
    try {
      // Parse the input
      SAXParser saxParser;
      saxParser = factory.newSAXParser();
      XMLReader xmlReader = saxParser.getXMLReader();
      xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler",
          this);
      saxParser.parse(inStream, this);
    } catch (ParserConfigurationException | SAXException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
      throw new IOException(e.getMessage());
    } catch (RuntimeException e) {
      if (!GPLogger.logToLogger(e)) {
        e.printStackTrace(System.err);
      }
      throw new IOException(e.getMessage());
    }
  }
}