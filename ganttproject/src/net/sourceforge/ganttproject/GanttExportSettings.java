package net.sourceforge.ganttproject;

import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.task.Task;

/** Class to store 3 boolean values */
public class GanttExportSettings {
    private Date startDate = null;

    private Date endDate = null;

    public boolean name, percent, depend, border3d, ok;

    private boolean onlySelectedItems;

    private List<Task> myVisibleTasks;

    private int myRowCount;

    private int myWidth = -1;

    public GanttExportSettings() {
        name = percent = depend = ok = true;
        onlySelectedItems = false;
    }

    public GanttExportSettings(boolean bName, boolean bPercent,
            boolean bDepend, boolean b3dBorders) {
        name = bName;
        percent = bPercent;
        depend = bDepend;
        border3d = b3dBorders;
        ok = true;
        onlySelectedItems = false;
    }

    public void setOnlySelectedItem(boolean selected){
        onlySelectedItems = selected;
    }

    public boolean isOnlySelectedItem(){
        return onlySelectedItems;
    }

    public void setStartDate(Date date) {
        startDate = date;
    }

    public void setEndDate(Date date) {
        endDate = date;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setVisibleTasks(List<Task> visibleTasks) {
        myVisibleTasks = visibleTasks;
        if (visibleTasks != null) {
            myRowCount = visibleTasks.size();
        }
    }

    public List<Task> getVisibleTasks() {
        return myVisibleTasks;
    }

    public int getRowCount() {
        return myRowCount;
    }

    public void setRowCount(int rowCount) {
        myRowCount = rowCount;
    }

    public int getWidth() {
        return myWidth;
    }

    public void setWidth(int width) {
        myWidth = width;
    }
}
