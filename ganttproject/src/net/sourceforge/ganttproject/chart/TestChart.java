/*
 * Created on 20.05.2005
 */
package net.sourceforge.ganttproject.chart;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Date;
import javax.swing.Icon;

import org.eclipse.core.runtime.IStatus;

import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.task.TaskManager;

public class TestChart implements Chart {
    public BufferedImage getChart(GanttExportSettings settings) {
        throw new UnsupportedOperationException();
    }

    public Date getStartDate() {
        throw new UnsupportedOperationException();
    }

    public Date getEndDate() {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        return "Test chart";
    }

    public void setTaskManager(TaskManager taskManager) {
        // TODO Auto-generated method stub

    }

    public Object getAdapter(Class arg0) {
        return null;
    }

    public void reset() {
        // TODO Auto-generated method stub

    }

    public Icon getIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    public GPOptionGroup[] getOptionGroups() {
        return null;
    }

    public Chart createCopy() {
        // TODO Auto-generated method stub
        return null;
    }

	public RenderedImage getRenderedImage(GanttExportSettings settings) {
		// TODO Auto-generated method stub
		return null;
	}

	public ChartSelection getSelection() {
		// TODO Auto-generated method stub
		return null;
	}

	public IStatus canPaste(ChartSelection selection) {
		// TODO Auto-generated method stub
		return null;
	}

	public void paste(ChartSelection selection) {
		// TODO Auto-generated method stub
		
	}

	public void addSelectionListener(ChartSelectionListener listener) {
		// TODO Auto-generated method stub
		
	}

	public void removeSelectionListener(ChartSelectionListener listener) {
		// TODO Auto-generated method stub
		
	}

    public ChartModelBase getModel() {
        // TODO Auto-generated method stub
        return null;
    }

}
