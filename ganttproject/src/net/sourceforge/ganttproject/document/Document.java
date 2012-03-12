/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 GanttProject team

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.runtime.IStatus;

/**
 * This interface abstracts the details of file access. Implementations of this
 * interface provide methods to open streams to a project file, independent of
 * storage location (filesystem / WebDAV).
 * 
 * @author Michael Haeusler (michael at akatose.de)
 */
public interface Document {
  String PLUGIN_ID = "net.sourceforge.ganttproject";

  public enum ErrorCode {
    NOT_WRITABLE, IS_DIRECTORY, LOST_UPDATE, PARENT_IS_NOT_DIRECTORY, PARENT_IS_NOT_WRITABLE, GENERIC_NETWORK_ERROR,
  }

  /**
   * @return the filename of the document (can be used forthe application's
   *         titlebar or the export dialog).
   */
  public String getFileName();

  /**
   * Checks, whether the document is readable.
   * 
   * @return readability
   */
  public boolean canRead();

  /**
   * Checks, whether the document is writable.
   * 
   * @return writability
   */
  public IStatus canWrite();

  /**
   * Checks, whether the document should appear in the MRU (list of <b>m</b>ost
   * <b>r</b>ecently <b>u</b>sed files).
   * 
   * @return validity for MRU
   */
  public boolean isValidForMRU();

  /**
   * Tries to acquire a lock. <br>
   * This method is optional. Storage containers, for which locking is
   * inappropriate, should always return true.
   * 
   * @return whether a lock could be successfully acquired
   * @see #releaseLock()
   */
  public boolean acquireLock();

  /**
   * Releases a previously acquired lock.
   * 
   * @see #acquireLock()
   */
  public void releaseLock();

  /**
   * Gets an InputStream, that allows to read from the document.
   * 
   * @return InputStream to read from
   */
  public InputStream getInputStream() throws IOException;

  /**
   * Gets an OutputStream, that allows to write to the document.
   * 
   * @return OutputStream to write to
   */
  public OutputStream getOutputStream() throws IOException;

  /**
   * Gets the path to the document.
   * 
   * @return the path to the document
   */
  public String getPath();

  /**
   * Gets the path to the document, if it is a file on a local file system (can
   * be used to initialize a JFileChooser).
   * 
   * @return the path, if the document is a local file; <code>null</code>,
   *         otherwise.
   */
  public String getFilePath();

  /**
   * Gets the username used to authenticate to the storage container
   * 
   * @return username
   */
  public String getUsername();

  /**
   * Gets the password used to authenticate to the storage container
   * 
   * @return password
   */
  public String getPassword();

  /**
   * Gets the last error
   * 
   * @return errormessage
   */
  public String getLastError();

  public void read() throws IOException, DocumentException;

  public void write() throws IOException;

  public URI getURI();

  Portfolio getPortfolio();

  boolean isLocal();

  /**
   * Used to generate useful exceptions for document saving and loading
   * (preventing bothersome errors for the end users when possible)
   */
  public class DocumentException extends Exception {
    public DocumentException(String msg) {
      super(msg);
    }

    public DocumentException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }
}
