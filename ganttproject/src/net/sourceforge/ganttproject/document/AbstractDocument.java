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

    public boolean equals(Object o) {
        if (o instanceof Document) {
            return ((Document) o).getPath().equals(this.getPath());
        }
        return false;
    }

    public boolean acquireLock() {
        return true;
    }

    public void releaseLock() {
    }

    public String getFilePath() {
        return null;
    }

    public String getURLPath() {
        return null;
    }

    public String getUsername() {
        return null;
    }

    public String getPassword() {
        return null;
    }

    public void setUserInfo(String user, String pass) {
    }

    public String getLastError() {
        return "";
    }

    public void read() throws IOException {
        throw new UnsupportedOperationException(
                "This method should be overriden in derived classes");
    }

    public Portfolio getPortfolio() {
        throw new UnsupportedOperationException();
    }


}
