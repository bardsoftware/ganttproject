package biz.ganttproject.impex.common;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: akurutin
 * Date: 03.04.17
 * Time: 19:34
 */
public interface GanttExport {
  void save(OutputStream stream) throws IOException;
}
