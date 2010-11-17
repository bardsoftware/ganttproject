/***************************************************************************
 GanttXMLFileFilter.java  -  description
 -------------------
 begin                : feb 2003
 copyright            : (C) 2002 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package net.sourceforge.ganttproject.filter;

/**
 * Class to select a filter for the FileChooser objet (*.xml)
 */
public class GanttXMLFileFilter extends ExtensionBasedFileFilter {
    public GanttXMLFileFilter() {
        super("xml|gan", "GanttProject files (.gan, .xml)");
    }
}
