/***************************************************************************
 GanttTXTFileFilter.java  -  description
 -------------------
 begin                : jun 2004
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

/**
 * @author athomas Filter for txt file import
 */
public class GanttTXTFileFilter extends FileFilter {

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
     */
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = getExtension(f);

        if (extension != null && extension.equals("txt"))
            return true;

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.filechooser.FileFilter#getDescription()
     */
    public String getDescription() {
        return "Text files (txt)";
    }

    /** Extention return */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

}
