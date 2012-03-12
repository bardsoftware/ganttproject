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
package net.sourceforge.ganttproject.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * This class implements the interface Document for file access on local file
 * systems.
 * 
 * @author Michael Haeusler (michael at akatose.de)
 */
public class FileDocument extends AbstractDocument {
  private File file;
  private long myLastAccessTimestamp;

  public FileDocument(File file) {
    this.file = file;
  }

  @Override
  public String getFileName() {
    return file.getName();
  }

  @Override
  public boolean canRead() {
    return file.canRead();
  }

  @Override
  public IStatus canWrite() {
    return (file.exists()) ? canOverwrite() : canCreate(file);
  }

  private IStatus canOverwrite() {
    if (file.isDirectory()) {
      return new Status(IStatus.ERROR, PLUGIN_ID, Document.ErrorCode.IS_DIRECTORY.ordinal(), "", null);
    }
    if (!file.canWrite()) {
      return new Status(IStatus.ERROR, PLUGIN_ID, Document.ErrorCode.NOT_WRITABLE.ordinal(), "", null);
    }
    if (file.lastModified() > getLastAccessTimestamp()) {
      return new Status(IStatus.ERROR, PLUGIN_ID, Document.ErrorCode.LOST_UPDATE.ordinal(), "", null);
    }
    return Status.OK_STATUS;
  }

  private long getLastAccessTimestamp() {
    return myLastAccessTimestamp;
  }

  private static IStatus canCreate(File f) {
    File parentFile = f.getParentFile();
    if (parentFile.exists()) {
      if (!parentFile.isDirectory()) {
        return new Status(IStatus.ERROR, PLUGIN_ID, Document.ErrorCode.PARENT_IS_NOT_DIRECTORY.ordinal(),
            parentFile.getAbsolutePath(), null);
      }
      if (!parentFile.canWrite()) {
        return new Status(IStatus.ERROR, PLUGIN_ID, Document.ErrorCode.PARENT_IS_NOT_WRITABLE.ordinal(),
            parentFile.getAbsolutePath(), null);
      }
      return Status.OK_STATUS;
    }
    return canCreate(parentFile);
  }

  @Override
  public boolean isValidForMRU() {
    return file.exists();
  }

  @Override
  public InputStream getInputStream() throws FileNotFoundException {
    myLastAccessTimestamp = System.currentTimeMillis();
    return new FileInputStream(file);
  }

  @Override
  public OutputStream getOutputStream() throws FileNotFoundException {
    return new FileOutputStream(file) {
      @Override
      public void close() throws IOException {
        super.close();
        myLastAccessTimestamp = System.currentTimeMillis();
      }
    };
  }

  @Override
  public String getPath() {
    return file.getPath();
  }

  @Override
  public String getFilePath() {
    return getPath();
  }

  public void open() {
    // Method is not used
  }

  @Override
  public void write() throws IOException {
    // Method is not used
  }

  @Override
  public URI getURI() {
    return file.toURI();
  }

  @Override
  public boolean isLocal() {
    return true;
  }
}
