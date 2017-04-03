package biz.ganttproject.impex.xls;

import biz.ganttproject.core.option.BooleanOption;
import biz.ganttproject.impex.common.AbstractGanttExport;
import biz.ganttproject.impex.common.GanttExportWriter;
import net.sourceforge.ganttproject.IGanttProject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created by Маша on 04.04.2017.
 */
public class GanttXlsExport extends AbstractGanttExport {
    public GanttXlsExport(IGanttProject project) {
        super(project);
    }

    @Override
    public GanttExportWriter getWriter(OutputStream stream) throws IOException {
        return new XlsWriterImpl(stream);
    }

    @Override
    public Map<String, BooleanOption> getTaskOptions() {
        return null;
    }

    @Override
    public Map<String, BooleanOption> getResourceOptions() {
        return null;
    }

    @Override
    public boolean bFixedSize() {
        return false;
    }

    @Override
    public String sSeparatedChar() {
        return null;
    }

    @Override
    public String sSeparatedTextChar() {
        return null;
    }
}
