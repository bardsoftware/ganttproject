/***************************************************************************
 GanttHTMLFileFilter.java  -  description
 -------------------
 begin                : feb 2004
 copyright            : (C) 2004 by Thomas Alexandre
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

import java.io.File;

import javax.swing.filechooser.FileFilter;

import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas Filter for html files
 */
public class GanttHTMLFileFilter extends FileFilter {

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
     */
    public boolean accept(File f) {
        return f.getName().toLowerCase().endsWith(".html")
                || f.getName().toLowerCase().endsWith(".htm")
                || f.isDirectory();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.filechooser.FileFilter#getDescription()
     */
    public String getDescription() {
        return GanttLanguage.getInstance().getText("filterhtml");
    }

}
