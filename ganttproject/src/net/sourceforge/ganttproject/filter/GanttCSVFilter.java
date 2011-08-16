/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Thomas Alexandre, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.filter;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Filter for csv txt file import
 *
 * @author athomas
 */
public class GanttCSVFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = getExtension(f);

        if (extension != null
                && (/* extension.equals("txt") || */extension.equals("csv")))
            return true;

        return false;
    }

    @Override
    public String getDescription() {
        return GanttLanguage.getInstance().getText("filtercsv");
    }

    /** @return Extension of File f */
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
