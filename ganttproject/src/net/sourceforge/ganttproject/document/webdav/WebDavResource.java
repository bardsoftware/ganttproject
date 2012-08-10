/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

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
package net.sourceforge.ganttproject.document.webdav;

import java.io.InputStream;
import java.util.List;

/**
 * Interface which provides an abstraction of WebDAV client implementations.
 *
 * It is supposed that user credentials are passed in the constructor and are available
 * to the resource instance during its lifetime.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public interface WebDavResource {
  class WebDavException extends Exception {
    public WebDavException(String message) {
      super(message);
    }
    public WebDavException(String message, Throwable cause) {
      super(message, cause);
    }
  }
  class WebDavRuntimeException extends RuntimeException {
    public WebDavRuntimeException(String message) {
      super(message);
    }
    public WebDavRuntimeException(String message, Throwable cause) {
      super(message, cause);
    }
    public WebDavRuntimeException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * This method will try to connect to a server at least once. Implementation may
   * decide to cache its result.
   *
   * @return {@code true} if resource exists on the server
   * @throws WebDavException if information is unavailable, e.g. due to network problems
   */
  boolean exists() throws WebDavException;

  WebDavUri getWebDavUri();
  /**
   * This method should be just a simple getter and should not issue any requests.
   * @return URL of this resource
   */
  String getUrl();

  /**
   * This method should be just a simple getter and should not issue any requests.
   * @return short name of this resource
   */
  String getName();

  /**
   * This method may or may not issue a request to a server. Implementation may decide to return
   * stub which has not yet received full information (and may not even exist on the server)
   *
   * @return parent resource
   */
  WebDavResource getParent();

  /**
   * This method may or may not issue a request to a server. Implementation may decide to fetch
   * all required pieces of information before calling this method or to delay fetching
   * information about the children until this method is called
   *
   * @return {@code true} if this resource is a collection
   * @throws WebDavException if information is unavailable, e.g. due to network problems
   */
  boolean isCollection() throws WebDavException;

  /**
   * This method may or may not issue a request to a server. Implementation may decide to fetch
   * all required pieces of information before calling this method or to delay fetching
   * information about the children until this method is called
   *
   * @return child resources if it is a collection or empty list otherwise
   * @throws WebDavException if information is unavailable, e.g. due to network problems
   */
  List<WebDavResource> getChildResources() throws WebDavException;

  /**
   * This method may or may not issue a request to a server. Implementation may decide to fetch
   * all required pieces of information before calling this method or to delay fetching
   * information until this method is called
   *
   * @return list of lock owners
   * @throws WebDavException if information is unavailable, e.g. due to network problems
   */
  List<String> getLockOwners() throws WebDavException;

  /**
   * This method may or may not issue a request to a server. Implementation may decide to fetch
   * all required pieces of information before calling this method or to delay fetching
   * information until this method is called
   *
   * @return {@code true} if this resource is locked
   * @throws WebDavException if information is unavailable, e.g. due to network problems
   */
  boolean isLocked() throws WebDavException;

  /**
   * This method may or may not issue a request to a server. Implementation may decide to fetch
   * all required pieces of information before calling this method or to delay fetching
   * information until this method is called
   *
   * @return {@code true} if user can lock this resource
   * @throws WebDavException if information is unavailable, e.g. due to network problems
   */
  boolean canLock() throws WebDavException;

  /**
   * This method issues a request to lock this resource with the given expiration time
   *
   * @param expirationTimeSec time in seconds after which lock expires
   * @throws WebDavException if locking fails, e.g. due to network problems
   */
  void lock(int expirationTimeSec) throws WebDavException;

  /**
   * This method issues a request to unlock this resource
   *
   * @throws WebDavException if unlocking fails, e.g. due to network problems
   */
  void unlock() throws WebDavException;

  /**
   * @return {@code true} if this resource is writable
   */
  boolean isWritable();

  /**
   * Writes data to the resource.
   *
   * @param byteArray data to write
   * @throws WebDavException if writing fails, e.g. due to network problems
   */
  void write(byte[] byteArray) throws WebDavException;

  /**
   * Reads data from the resource.
   *
   * @throws WebDavException if reading fails, e.g. due to network problems
   */
  InputStream getInputStream() throws WebDavException;

  void delete() throws WebDavException;

}