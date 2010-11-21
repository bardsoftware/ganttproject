/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2010 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package biz.ganttproject.impex.msproject2;

import java.util.HashMap;
import java.util.Map;

import net.sf.mpxj.ConstraintType;
import net.sf.mpxj.Duration;
import net.sf.mpxj.Priority;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.RelationType;
import net.sf.mpxj.TimeUnit;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependencySlice;

/**
 * Creates MPXJ ProjectFile from GanttProject's IGanttProject.
 * @author dbarashev (Dmitry Barashev)
 */
class ProjectFileExporter {
	private IGanttProject myNativeProject;
	private ProjectFile myOutputProject;

	public ProjectFileExporter(IGanttProject nativeProject) {
		myNativeProject = nativeProject;
		myOutputProject = new ProjectFile();
	}
	
	ProjectFile run() {
		Map<Integer, net.sf.mpxj.Task> id2mpxjTask = new HashMap<Integer, net.sf.mpxj.Task>(); 
		exportTasks(id2mpxjTask);
		exportDependencies(id2mpxjTask);
		return myOutputProject;
	}
	
	private void exportTasks(Map<Integer, net.sf.mpxj.Task> id2mpxjTask) {
		for (Task t : getTaskHierarchy().getNestedTasks(getTaskHierarchy().getRootTask())) {
			exportTask(t, null, 0, id2mpxjTask);			
		}
	}
	
	private void exportTask(Task t, net.sf.mpxj.Task mpxjParentTask, int outlineLevel, Map<Integer, net.sf.mpxj.Task> id2mpxjTask) {
		net.sf.mpxj.Task mpxjTask = mpxjParentTask == null ? myOutputProject.addTask() : mpxjParentTask.addTask();
//		if (mpxjParentTask != null) {
//			mpxjParentTask.addChildTask(mpxjTask, 1);
//		}
		mpxjTask.setOutlineLevel(outlineLevel);
		mpxjTask.setUniqueID(t.getTaskID());
        mpxjTask.setID(id2mpxjTask.size());
		mpxjTask.setName(t.getName());
		mpxjTask.setNotes(t.getNotes());
		mpxjTask.setMilestone(t.isMilestone());
        mpxjTask.setPhysicalPercentComplete(t.getCompletionPercentage());
        mpxjTask.setHyperlink(((GanttTask)t).getWebLink());
        mpxjTask.setStart(t.getStart().getTime());
        mpxjTask.setFinish(t.getEnd().getTime());
        mpxjTask.setConstraintType(ConstraintType.AS_SOON_AS_POSSIBLE);
        mpxjTask.setPriority(convertPriority(t));
        
        exportCustomProperties(t, mpxjTask);
        id2mpxjTask.put(t.getTaskID(), mpxjTask);
        
		for (Task child : getTaskHierarchy().getNestedTasks(t)) {
			exportTask(child, mpxjTask, outlineLevel + 1, id2mpxjTask);
		}
		
	}

	private void exportCustomProperties(Task t, net.sf.mpxj.Task mpxjTask) {
		// TODO Auto-generated method stub
	}

	private void exportDependencies(Map<Integer, net.sf.mpxj.Task> id2mpxjTask) {
		for (Task t : getTaskManager().getTasks()) {
			net.sf.mpxj.Task mpxjTask = id2mpxjTask.get(t.getTaskID());
			
			TaskDependencySlice dependencies = t.getDependenciesAsDependant();
			for (TaskDependency dep : dependencies.toArray()) {
				net.sf.mpxj.Task mpxjPredecessor = id2mpxjTask.get(dep.getDependee().getTaskID());
				assert mpxjPredecessor != null : "Can't find mpxj task for id=" + dep.getDependee().getTaskID();
				mpxjTask.addPredecessor(mpxjPredecessor, convertConstraint(dep), convertLag(dep));
			}
		}
	}

	private RelationType convertConstraint(TaskDependency dep) {
		switch (TaskDependencyConstraint.Type.getType(dep.getConstraint())) {
		case finishstart:
			return RelationType.FINISH_START;
		case startfinish:
			return RelationType.START_FINISH;
		case finishfinish:
			return RelationType.FINISH_FINISH;
		case startstart:
			return RelationType.START_START;
		default:
			assert false : "Should not be here";
			return null;
		}
	}

	private Duration convertLag(TaskDependency dep) {
		// TODO(dbarashev): Get rid of days
		return Duration.getInstance(dep.getDifference(), TimeUnit.DAYS);
	}

	private Priority convertPriority(Task t) {
		switch (t.getPriority()) {
		case LOWEST:
			return Priority.getInstance(Priority.LOWEST);
		case LOW:
			return Priority.getInstance(Priority.LOW);
		case NORMAL:
			return Priority.getInstance(Priority.MEDIUM);
		case HIGH:
			return Priority.getInstance(Priority.HIGH);
		case HIGHEST:
			return Priority.getInstance(Priority.HIGHEST);
		default:
			assert false : "Should not be here";
			return Priority.getInstance(Priority.MEDIUM);
		}
	}

	private TaskManager getTaskManager() {
		return myNativeProject.getTaskManager();
	}
	
	private TaskContainmentHierarchyFacade getTaskHierarchy() {
		return getTaskManager().getTaskHierarchy();
	}
}
