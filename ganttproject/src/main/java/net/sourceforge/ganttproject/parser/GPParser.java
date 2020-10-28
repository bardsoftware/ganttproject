/*
 * Created on 12.03.2005
 */
package net.sourceforge.ganttproject.parser;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author bard
 */
public interface GPParser {
  boolean load(InputStream inStream) throws IOException;

  void addTagHandler(TagHandler handler);

  void addParsingListener(ParsingListener listener);

  TagHandler getTimelineTagHandler();
  TagHandler getDefaultTagHandler();

  ParsingContext getContext();

}
