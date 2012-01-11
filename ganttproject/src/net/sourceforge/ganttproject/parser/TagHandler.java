/***************************************************************************
 TagHandler.java  -  description
 -------------------
 begin                : may 2003

 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 3 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package net.sourceforge.ganttproject.parser;

import org.xml.sax.Attributes;

/**
 * Class to parse the xml file attribute
 */
public interface TagHandler {
    /** Method when start to parse an attribute */
    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) throws FileFormatException;

    /** Method when finish to parse an attribute */
    public void endElement(String namespaceURI, String sName, String qName);
}
