/*
GanttProject is an opensource project management tool. License: GPL3
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
package net.sourceforge.ganttproject.document

import com.google.common.hash.Hashing
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import java.io.*
import java.net.URI

/**
 * This class implements the interface Document for file access on local file
 * systems.
 *
 * @author Michael Haeusler (michael at akatose.de)
 */
class FileDocument(val file: File) : AbstractDocument() {
  private var lastReadFingerprint: String = ""

  override fun getFileName(): String {
    return file.name
  }

  override fun canRead(): Boolean {
    return file.exists() && file.canRead()
  }

  override fun canWrite(): IStatus {
    return if (file.exists()) canOverwrite() else canCreate(file)
  }

  private fun canOverwrite(): IStatus {
    if (file.isDirectory) {
      return Status(IStatus.ERROR, PLUGIN_ID,
          Document.ErrorCode.IS_DIRECTORY.ordinal,
          "It is a directory",
          null
      )
    }
    if (!file.canWrite()) {
      return Status(IStatus.ERROR, PLUGIN_ID,
          Document.ErrorCode.NOT_WRITABLE.ordinal,
          "File is reported as not writeable",
          null
      )
    }
    return Status.OK_STATUS
  }

  override fun isValidForMRU(): Boolean {
    return file.exists()
  }

  @Throws(FileNotFoundException::class)
  override fun getInputStream(): InputStream =
    file.readBytes().let {
      this.lastReadFingerprint = it.fingerprint()
      it.inputStream()
    }

  @Throws(FileNotFoundException::class)
  override fun getOutputStream(): OutputStream {
    return object : ByteArrayOutputStream() {
      @Throws(IOException::class)
      override fun close() {
        super.close()
        val memBytes = this.toByteArray()
        lastReadFingerprint.let {
          val fileBytes = file.readBytes()
          if (it.isNotBlank() && it != fileBytes.fingerprint()) {
            throw IOException("This write has been cancelled because of the lost update: file last modification ts=${file.lastModified()}, content sha256=${fileBytes.sha256()}. This most likely means that the file has been modified by someone else. You may want to save the project to some other file.")
          }
        }
        file.writeBytes(memBytes)
        if (file.readBytes().fingerprint() != memBytes.fingerprint()) {
          throw IOException("Write verification failed: after write the file contents on disk is different from the contents in GanttProject memory. You may want to save a backup copy and find out what went wrong.")
        }
        lastReadFingerprint = memBytes.fingerprint()
      }
    }
  }

  override fun getPath(): String {
    return file.path
  }

  override fun getFilePath(): String {
    return path
  }

  fun open() {
    // Method is not used
  }

  @Throws(IOException::class)
  override fun write() {
    // Method is not used
  }

  override fun getURI(): URI {
    return file.toURI()
  }

  override fun isLocal(): Boolean {
    return true
  }

  @Throws(IOException::class)
  fun create() {
    if (file.exists()) {
      return
    }
    if (!file.parentFile.exists()) {
      val result = file.parentFile.mkdirs()
      if (!result) {
        throw IOException("Failed to create parent directories to file " + file.path)
      }
    }
    file.createNewFile()
  }

  @Throws(IOException::class)
  fun delete() {
    if (file.exists()) {
      if (!file.delete()) {
        throw IOException("Failed to delete file " + file.path)
      }
    }
  }
}


private fun (ByteArray).fingerprint(): String = Hashing.farmHashFingerprint64().hashBytes(this).toString()
private fun (ByteArray).sha256(): String = Hashing.sha256().hashBytes(this).toString()

private fun canCreate(f: File): IStatus {
  val parentFile = f.parentFile
  if (parentFile.exists()) {
    if (!parentFile.isDirectory) {
      return Status(IStatus.ERROR, AbstractDocument.PLUGIN_ID, Document.ErrorCode.PARENT_IS_NOT_DIRECTORY.ordinal,
        parentFile.absolutePath, null)
    }
    return if (!parentFile.canWrite()) {
      Status(IStatus.ERROR, AbstractDocument.PLUGIN_ID, Document.ErrorCode.PARENT_IS_NOT_WRITABLE.ordinal,
        parentFile.absolutePath, null)
    } else Status.OK_STATUS
  }
  return canCreate(parentFile)
}
