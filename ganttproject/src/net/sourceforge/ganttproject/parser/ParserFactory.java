/*
 * Created on 12.03.2005
 */
package net.sourceforge.ganttproject.parser;

import net.sourceforge.ganttproject.io.GPSaver;

/**
 * @author bard
 */
public interface ParserFactory {
  GPParser newParser();

  GPSaver newSaver();
}
