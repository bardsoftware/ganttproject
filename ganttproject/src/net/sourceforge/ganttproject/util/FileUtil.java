/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

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
package net.sourceforge.ganttproject.util;

import java.io.File;
import java.io.IOException;

public abstract class FileUtil {
  private static final char FILE_EXTENSION_SEPARATOR = '.';

  /**
   * @return the extension of file, or an empty String if no extension is
   *         present
   */
  public static String getExtension(File file) {
    int lastDot = file.getName().lastIndexOf(FILE_EXTENSION_SEPARATOR);
    return lastDot >= 0 ? file.getName().substring(lastDot + 1) : "";
  }

  /** @return f with the new extension */
  public static File replaceExtension(File f, String newExtension) {
    String filenameWithouExtension = getFilenameWithoutExtension(f);
    File containingFolder = f.getParentFile();
    return new File(containingFolder, filenameWithouExtension + FILE_EXTENSION_SEPARATOR + newExtension);
  }

  public static File appendExtension(File f, String extension) {
    File containingFolder = f.getParentFile();
    return new File(containingFolder, f.getName() + FILE_EXTENSION_SEPARATOR + extension);
  }


  /**
   * @return f with the suffix added before the extension (or at the end of the
   *         name if no extension is present)
   */
  public static File appendSuffixBeforeExtension(File f, String suffix) throws IOException {
    String filename = f.getName();
    int i = filename.lastIndexOf(FILE_EXTENSION_SEPARATOR);

    File containingFolder = f.getParentFile();
    File result;
    if (i > 0 && i < filename.length() - 1) {
      String withoutExtension = filename.substring(0, i);
      String extension = filename.substring(i);
      result = new File(containingFolder, withoutExtension + suffix + extension);
    } else {
      result = new File(containingFolder, filename + suffix);
    }
    if (!result.exists()) {
      result.createNewFile();
    }
    return result;
  }

  /** @return the filename of f without extension */
  public static String getFilenameWithoutExtension(File f) {
    String filename = f.getName();
    int i = filename.lastIndexOf(FILE_EXTENSION_SEPARATOR);
    return i >= 0 ? filename.substring(0, i) : filename;
  }
}
