/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 GanttProject Team

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
package net.sourceforge.ganttproject.filter;

import java.io.File;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileFilter;

/**
 * Class to select a filter for the FileChooser object for the provided
 * extension
 * 
 * @author bard
 */
public class ExtensionBasedFileFilter extends FileFilter {
  private final String myDescription;

  private final Pattern myPattern;

  public ExtensionBasedFileFilter(String fileExtension, String description) {
    myDescription = description;
    myPattern = Pattern.compile(fileExtension);
  }

  @Override
  public boolean accept(File f) {
    if (f.isDirectory()) {
      return true;
    }
    return matches(getExtension(f));
  }

  @Override
  public String getDescription() {
    return myDescription;
  }

  /** @return extension of File f */
  private static String getExtension(File f) {
    String ext = null;
    String s = f.getName();
    int i = s.lastIndexOf('.');

    if (i > 0 && i < s.length() - 1) {
      ext = s.substring(i + 1).toLowerCase();
    }
    return ext;
  }

  private boolean matches(String fileExtension) {
    boolean result = fileExtension != null && myPattern.matcher(fileExtension).matches();
    return result;
  }
}
