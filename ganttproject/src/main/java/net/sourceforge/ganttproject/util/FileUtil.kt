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
package net.sourceforge.ganttproject.util

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileUtil {
  private const val FILE_EXTENSION_SEPARATOR = '.'

  /**
   * @return the extension of file, or an empty String if no extension is
   * present
   */
  @JvmStatic
  fun getExtension(file: File): String {
    val lastDot = file.name.lastIndexOf(FILE_EXTENSION_SEPARATOR)
    return if (lastDot >= 0) file.name.substring(lastDot + 1) else ""
  }

  /** @return f with the new extension
   */
  fun replaceExtension(f: File, newExtension: String): File {
    val filenameWithouExtension = getFilenameWithoutExtension(f)
    val containingFolder = f.parentFile
    return File(containingFolder, filenameWithouExtension + FILE_EXTENSION_SEPARATOR + newExtension)
  }

  fun appendExtension(f: File, extension: String): File {
    val containingFolder = f.parentFile
    return File(containingFolder, f.name + FILE_EXTENSION_SEPARATOR + extension)
  }

  /**
   * @return f with the suffix added before the extension (or at the end of the
   * name if no extension is present)
   */
  @JvmStatic
  @Throws(IOException::class)
  fun appendSuffixBeforeExtension(f: File, suffix: String): File {
    val filename = f.name
    val i = filename.lastIndexOf(FILE_EXTENSION_SEPARATOR)
    val containingFolder = f.parentFile
    val result: File
    result = if (i > 0 && i < filename.length - 1) {
      val withoutExtension = filename.substring(0, i)
      val extension = filename.substring(i)
      File(containingFolder, withoutExtension + suffix + extension)
    } else {
      File(containingFolder, filename + suffix)
    }
    if (!result.exists()) {
      result.createNewFile()
    }
    return result
  }

  /** @return the filename of f without extension
   */
  fun getFilenameWithoutExtension(f: File): String {
    return getFilenameWithoutExtension(f.name)
  }

  @JvmStatic
  fun replaceExtension(filename: String, newExtension: String): String {
    return getFilenameWithoutExtension(filename) + FILE_EXTENSION_SEPARATOR + newExtension
  }

  private fun getFilenameWithoutExtension(filename: String): String {
    val i = filename.lastIndexOf(FILE_EXTENSION_SEPARATOR)
    return if (i >= 0) filename.substring(0, i) else filename
  }

  fun zip(entries: List<ZipInput>): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val zipOut = ZipOutputStream(outputStream)
    entries.forEach {
      zipOut.putNextEntry(ZipEntry(it.first))
      it.second().use { inputStream -> inputStream.copyTo(zipOut) }
      zipOut.closeEntry()
    }
    zipOut.close()
    return outputStream.toByteArray()
  }
}

typealias ZipInput = Pair<String, ()-> InputStream>
