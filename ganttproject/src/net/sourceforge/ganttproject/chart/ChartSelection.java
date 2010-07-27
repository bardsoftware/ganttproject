package net.sourceforge.ganttproject.chart;

import java.util.List;

import org.eclipse.core.runtime.IStatus;

public interface ChartSelection {
	boolean isEmpty();
	List/*<Task>*/ getTasks();
	List/*<HumanResource>*/ getHumanResources();
	IStatus isDeletable();
	void startCopyClipboardTransaction();
	void startMoveClipboardTransaction();
	void cancelClipboardTransaction();
	void commitClipboardTransaction();
}
