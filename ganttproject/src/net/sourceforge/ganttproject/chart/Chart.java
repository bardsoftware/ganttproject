package net.sourceforge.ganttproject.chart;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Date;

import javax.swing.Icon;

import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.export.ChartImageVisitor;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.task.TaskManager;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;

public interface Chart extends IAdaptable {
    IGanttProject getProject();

    public void buildImage(GanttExportSettings settings, ChartImageVisitor imageVisitor);
    public RenderedImage getRenderedImage(GanttExportSettings settings);

    public Date getStartDate();
    void setStartDate(Date startDate);
    public Date getEndDate();

    void setDimensions(int height, int width);

    public String getName();

    /** Repaints the chart */
    public void reset();

    public GPOptionGroup[] getOptionGroups();

    public Chart createCopy();

    public ChartSelection getSelection();

    public IStatus canPaste(ChartSelection selection);

    public void paste(ChartSelection selection);

    public void addSelectionListener(ChartSelectionListener listener);
    public void removeSelectionListener(ChartSelectionListener listener);

}