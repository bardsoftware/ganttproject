package biz.ganttproject.impex.common;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: akurutin
 * Date: 03.04.17
 * Time: 19:46
 */
public interface GanttExportWriter {
  void print(String value) throws IOException;

  void println() throws IOException;

  void flush() throws IOException;

  void close() throws IOException;
}
