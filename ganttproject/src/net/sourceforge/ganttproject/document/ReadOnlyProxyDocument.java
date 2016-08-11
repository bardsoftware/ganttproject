/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 GanttProject Team

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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * Document which proxies all read methods and forbids all write methods.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class ReadOnlyProxyDocument implements Document {

  private final Document myDelegate;

  public ReadOnlyProxyDocument(Document delegate) {
    myDelegate = delegate;
  }

  @Override
  public String getFileName() {
    return myDelegate.getFileName();
  }

  @Override
  public boolean canRead() {
    return myDelegate.canRead();
  }

  @Override
  public IStatus canWrite() {
    return new Status(IStatus.ERROR, "net.sourceforge.ganttproject", 0, "You can't write a read-only document", null);
  }

  @Override
  public boolean isValidForMRU() {
    return false;
  }

  @Override
  public boolean acquireLock() {
    return true;
  }

  @Override
  public void releaseLock() {
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return myDelegate.getInputStream();
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return null;
  }

  @Override
  public String getPath() {
    return myDelegate.getPath();
  }

  @Override
  public String getFilePath() {
    return myDelegate.getFilePath();
  }

  @Override
  public String getUsername() {
    return myDelegate.getUsername();
  }

  @Override
  public String getPassword() {
    return myDelegate.getPassword();
  }

  @Override
  public String getLastError() {
    return myDelegate.getLastError();
  }

  @Override
  public void read() throws IOException, DocumentException {
    myDelegate.read();
  }

  @Override
  public void write() throws IOException {
    throw new IOException("You can't write a read-only document");
  }

  @Override
  public URI getURI() {
    return myDelegate.getURI();
  }

  @Override
  public Portfolio getPortfolio() {
    return null;
  }

  @Override
  public boolean isLocal() {
    return myDelegate.isLocal();
  }

  @Override
  public void setMirror(Document mirrorDocument) {
    throw new UnsupportedOperationException("Read only document doesn't support mirroring");
  }
}
