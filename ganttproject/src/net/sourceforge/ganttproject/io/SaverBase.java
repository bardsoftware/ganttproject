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
package net.sourceforge.ganttproject.io;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

public class SaverBase {

  protected TransformerHandler createHandler(Result result) throws TransformerConfigurationException {
    SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    TransformerHandler handler = factory.newTransformerHandler();
    Transformer serializer = handler.getTransformer();
    serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
    serializer.setOutputProperty(OutputKeys.METHOD, "xml");
    serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    handler.setResult(result);
    return handler;
  }

  protected void startElement(String name, TransformerHandler handler) throws SAXException {
    startElement(name, ourEmptyAttributes, handler);
  }

  protected void startElement(String name, AttributesImpl attrs, TransformerHandler handler) throws SAXException {
    handler.startElement("", name, name, attrs);
    attrs.clear();
  }

  protected void endElement(String name, TransformerHandler handler) throws SAXException {
    handler.endElement("", name, name);
  }

  protected void addAttribute(String name, String value, AttributesImpl attrs) {
    if (value != null) {
      attrs.addAttribute("", name, name, "CDATA", value);
    }
  }

  protected void addAttribute(String name, int value, AttributesImpl attrs) {
    addAttribute(name, String.valueOf(value), attrs);
  }

  protected void addAttribute(String name, Boolean value, AttributesImpl attrs) {
    addAttribute(name, value.toString(), attrs);
  }

  protected void emptyElement(String name, AttributesImpl attrs, TransformerHandler handler) throws SAXException {
    startElement(name, attrs, handler);
    endElement(name, handler);
    attrs.clear();
  }

  protected void cdataElement(String name, String cdata, AttributesImpl attrs, TransformerHandler handler)
      throws SAXException {
    if (cdata == null) {
      return;
    }
    startElement(name, attrs, handler);
    handler.startCDATA();
    handler.characters(cdata.toCharArray(), 0, cdata.length());
    handler.endCDATA();
    endElement(name, handler);
  }

  protected void emptyComment(TransformerHandler handler) throws SAXException {
    handler.comment(new char[] { ' ' }, 0, 1);

  }

  private static AttributesImpl ourEmptyAttributes = new AttributesImpl();

}
