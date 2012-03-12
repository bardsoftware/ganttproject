/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.parser;

import org.xml.sax.Attributes;

/**
 * @author nbohn
 */
public class IconPositionTagHandler implements TagHandler {
  private int[] myList;

  private String myPositions = "";

  private int[] myDeletedList;

  private String myDeletedPositions = "";

  public IconPositionTagHandler() {
  }

  @Override
  public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
      throws FileFormatException {
    if (qName.equals("positions")) {
      loadIcon(attrs);
      loadDeletedIcon(attrs);
    }
  }

  @Override
  public void endElement(String namespaceURI, String sName, String qName) {
  }

  private void loadIcon(Attributes atts) {
    myPositions = atts.getValue("icons-list");
    if (!myPositions.equals("")) {
      String[] positions = myPositions.split(",");
      myList = new int[positions.length];
      for (int i = 0; i < positions.length; i++)
        myList[i] = (new Integer(positions[i])).intValue();
    }
  }

  private void loadDeletedIcon(Attributes atts) {
    myDeletedPositions = atts.getValue("deletedIcons-list");
    if ((myDeletedPositions != null) && (!myDeletedPositions.equals(""))) {
      String[] positions = myDeletedPositions.split(",");
      myDeletedList = new int[positions.length];
      for (int i = 0; i < positions.length; i++)
        myDeletedList[i] = (new Integer(positions[i])).intValue();
    }
  }

  public int[] getList() {
    return myList;
  }

  public String getPositions() {
    return myPositions;
  }

  public int[] getDeletedList() {
    return myDeletedList;
  }

  public String getDeletedPositions() {
    return myDeletedPositions;
  }
}
