package biz.ganttproject.impex.msproject2;

import net.sf.mpxj.ProjectFile;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

class ProjectFileExporter {
	private IGanttProject myNativeProject;
	private ProjectFile myOutputProject;

	public ProjectFileExporter(IGanttProject nativeProject) {
		myNativeProject = nativeProject;
		myOutputProject = new ProjectFile();
	}
	
	ProjectFile run() {
		exportTasks();
		return myOutputProject;
	}
	
	private void exportTasks() {
		for (Task t : getTaskManager().getTasks()) {
			exportTask(t);
		}
	}
	
	private void exportTask(Task t) {
		net.sf.mpxj.Task outTask = myOutputProject.addTask();
		outTask.setName(t.getName());
		outTask.setNotes(t.getNotes());
	}

	private TaskManager getTaskManager() {
		return myNativeProject.getTaskManager();
	}
}
