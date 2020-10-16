/*
 * Created on 14.03.2005
 */
package net.sourceforge.ganttproject.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author bard
 */
public interface GPSaver {
  void save(OutputStream output) throws IOException;

}
