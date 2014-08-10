/*
 * Created on 28.09.2003
 *
 */
package net.sourceforge.ganttproject.document;

import java.io.IOException;

/**
 * @author Michael Haeusler (michael at akatose.de)
 */
public abstract class AbstractDocument implements Document {

  @Override
  public boolean equals(Object o) {
    if (o instanceof Document) {
      return ((Document) o).getPath().equals(this.getPath());
    }
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
  public String getFilePath() {
    return null;
  }

  @Override
  public String getUsername() {
    return null;
  }

  @Override
  public String getPassword() {
    return null;
  }

  public void setUserInfo(String user, String pass) {
  }

  @Override
  public String getLastError() {
    return "";
  }

  @Override
  public void read() throws IOException {
    throw new UnsupportedOperationException("This method should be overriden in derived classes");
  }

  @Override
  public Portfolio getPortfolio() {
    throw new UnsupportedOperationException();
  }

}
