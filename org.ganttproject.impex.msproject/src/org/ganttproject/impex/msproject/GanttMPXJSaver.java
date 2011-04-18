/*
 * file:       GanttMPXJSaver.java
 * author:     Jon Iles
 * copyright:  (c) Tapster Rock Limited 2005
 * date:       04/02/2005
 */

/*
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package org.ganttproject.impex.msproject;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.GanttTaskRelationship;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;

import com.tapsterrock.mpx.ConstraintType;
import com.tapsterrock.mpx.MPXDuration;
import com.tapsterrock.mpx.MPXFile;
import com.tapsterrock.mpx.ProjectHeader;
import com.tapsterrock.mpx.Relation;
import com.tapsterrock.mpx.RelationType;
import com.tapsterrock.mpx.Resource;
import com.tapsterrock.mpx.TimeUnit;

/**
 * This class implements the mechanism used to export Microsoft Project data
 * from the GanttProject application using MPXJ.
 */
public class GanttMPXJSaver {
    private HumanResourceManager myHrManager;

    private TaskManager myTaskManager;

    private final IGanttProject m_project;

    // private GanttTree m_tree;

    private MPXFile m_mpx;

    private HashMap<Integer, Integer> m_ganttMpxTaskMap = new HashMap<Integer, Integer>();

    private HashMap<Integer, Integer>  m_ganttMpxResourceMap = new HashMap<Integer, Integer>();

    /**
     * @param project
     *            current project
     * @param tree
     *            current task tree
     * @param resources
     *            current resources
     */
    protected GanttMPXJSaver(IGanttProject project) {
        myHrManager = (HumanResourceManager) project.getHumanResourceManager();
        myTaskManager = project.getTaskManager();
        m_project = project;
    }

    /**
     * Process resource data.
     */
    private void processResources() {
        try {
            List<HumanResource> resources = myHrManager.getResources();

            for (int i = 0; i < resources.size(); i++) {
                HumanResource resource = resources.get(i);

                Resource mpxResource = m_mpx.addResource();
                mpxResource.setName(resource.getName());
                mpxResource.setEmailAddress(resource.getMail());

                m_ganttMpxResourceMap.put(new Integer(resource.getId()),
                        mpxResource.getUniqueID());
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    /**
     * Process task data.
     */
    private void processTasks() {
        // Enumeration children =
        // ((DefaultMutableTreeNode)m_tree.getJTree().getModel().getRoot()).children();
        Task[] children = myTaskManager.getTaskHierarchy().getNestedTasks(
                myTaskManager.getTaskHierarchy().getRootTask());

        for (int i = 0; i < children.length; i++) {
            processTask(children[i], null);
        }
    }

    /**
     * This method is called recursively to process a single task and any child
     * tasks.
     * 
     * @param node
     *            Gantt task
     * @param parent
     *            parent MPXJ task
     */
    private void processTask(Task ganttTask, com.tapsterrock.mpx.Task parent) {
        try {
            //
            // Process the current task
            //
            int percentageComplete = ganttTask.getCompletionPercentage();
            boolean milestone = ganttTask.isMilestone();
            long taskLength = milestone == false ? ganttTask.getDuration()
                    .getLength() : 0;

            com.tapsterrock.mpx.Task mpxTask = parent == null ? m_mpx.addTask()
                    : parent.addTask();
            mpxTask.setName(ganttTask.getName());
            mpxTask.setMilestone(ganttTask.isMilestone());
            mpxTask.setStart(ganttTask.getStart().getTime());
            mpxTask.setConstraintDate(ganttTask.getStart().getTime());
            mpxTask.setConstraintType(ConstraintType.MUST_START_ON);
            mpxTask.setDuration(MPXDuration.getInstance(taskLength, TimeUnit.DAYS));
            mpxTask.setPercentageComplete(percentageComplete);
            mpxTask.setHyperlink(((GanttTask) ganttTask).getWebLink());
            mpxTask.setNotes(ganttTask.getNotes());
            if (percentageComplete != 0) {
                mpxTask.setActualStart(mpxTask.getStart());
            }

            m_ganttMpxTaskMap.put(new Integer(ganttTask.getTaskID()), mpxTask
                    .getUniqueID());

            //
            // Process any child tasks
            //
            Task[] children = myTaskManager.getTaskHierarchy().getNestedTasks(
                    ganttTask);
            for (int i = 0; i < children.length; i++) {
                processTask(children[i], mpxTask);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Process relationships.
     */
    private void processRelationships() {
        Task[] children = myTaskManager.getTaskHierarchy().getNestedTasks(
                myTaskManager.getTaskHierarchy().getRootTask());

        for (int i = 0; i < children.length; i++) {
            processRelationships(children[i]);
        }
    }

    /**
     * Recursively process the relationships between tasks.
     * 
     * @param element
     *            current task
     */
    private void processRelationships(Task ganttTask) {
        // Vector relationships = ganttTask.getPredecessorsOld();
        TaskDependency[] dependencies = ganttTask.getDependencies().toArray();
        TaskDependency relationship;
        com.tapsterrock.mpx.Task mpxSuccessorTask;
        com.tapsterrock.mpx.Task mpxPredecessorTask;
        Integer mpxSuccessorTaskID;
        Integer mpxPredecessorTaskID;
        Relation mpxRelation;

        for (int loop = 0; loop < dependencies.length; loop++) {
            relationship = dependencies[loop];
            mpxSuccessorTaskID = (Integer) m_ganttMpxTaskMap.get(new Integer(
                    relationship.getDependant().getTaskID()));
            mpxPredecessorTaskID = (Integer) m_ganttMpxTaskMap.get(new Integer(
                    relationship.getDependee().getTaskID()));

            if (mpxSuccessorTaskID != null && mpxPredecessorTaskID != null) {
                mpxSuccessorTask = m_mpx.getTaskByUniqueID(mpxSuccessorTaskID
                        .intValue());
                mpxPredecessorTask = m_mpx
                        .getTaskByUniqueID(mpxPredecessorTaskID.intValue());

                mpxRelation = mpxSuccessorTask
                        .addPredecessor(mpxPredecessorTask);

                TaskDependencyConstraint constraint = relationship
                        .getConstraint();
                switch (constraint.getID()) {
                case GanttTaskRelationship.SS:
                    mpxRelation.setType(RelationType.START_START);
                    break;
                case GanttTaskRelationship.SF:
                    mpxRelation.setType(RelationType.START_FINISH);
                    break;
                case GanttTaskRelationship.FS:
                    mpxRelation.setType(RelationType.FINISH_START);
                    break;
                case GanttTaskRelationship.FF:
                    mpxRelation.setType(RelationType.FINISH_FINISH);
                    break;
                }
            }
        }

        Task[] children = myTaskManager.getTaskHierarchy().getNestedTasks(
                ganttTask);
        for (int i = 0; i < children.length; i++) {
            processRelationships(children[i]);
        }
    }

    /**
     * Process resource assignments.
     */
    private void processAssignments() {
        Task[] children = myTaskManager.getTaskHierarchy().getNestedTasks(
                myTaskManager.getTaskHierarchy().getRootTask());

        for (int i = 0; i < children.length; i++) {
            processAssignments(children[i]);
        }
    }

    /**
     * Recursively process resource assignments.
     * 
     * @param node
     *            current task
     */
    private void processAssignments(Task ganttTask) {
        try {
            //
            // Process the current task
            //
            ResourceAssignment[] assignments = ganttTask.getAssignments();
            for (int loop = 0; loop < assignments.length; loop++) {
                processAssignment(ganttTask, assignments[loop]);
            }

            //
            // Process any child tasks
            //
            Task[] children = myTaskManager.getTaskHierarchy().getNestedTasks(
                    ganttTask);
            for (int i = 0; i < children.length; i++) {
                processAssignments(children[i]);
            }
        }

        catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Process a single resource assignment.
     * 
     * @param ganttTask
     *            parent task
     * @param ganttAssignment
     *            resource assignment
     * @throws Exception
     */
    private void processAssignment(Task ganttTask,
            ResourceAssignment ganttAssignment) throws Exception {
        Integer mpxTaskID = (Integer) m_ganttMpxTaskMap.get(new Integer(
                ganttTask.getTaskID()));
        Integer mpxResourceID = (Integer) m_ganttMpxResourceMap
                .get(new Integer(ganttAssignment.getResource().getId()));

        if (mpxTaskID != null && mpxResourceID != null) {
            com.tapsterrock.mpx.Task mpxTask = m_mpx
                    .getTaskByUniqueID(mpxTaskID.intValue());
            com.tapsterrock.mpx.Resource mpxResource = m_mpx
                    .getResourceByUniqueID(mpxResourceID.intValue());
            MPXDuration taskWork = mpxTask.getDuration();
            MPXDuration resourceWork = MPXDuration.getInstance(
                    (taskWork.getDuration() * ganttAssignment.getLoad()) / 100,
                    taskWork.getUnits());
            MPXDuration resourceActualWork = MPXDuration.getInstance(
                    (resourceWork.getDuration() * mpxTask
                            .getPercentageCompleteValue()) / 100, resourceWork
                            .getUnits());

            com.tapsterrock.mpx.ResourceAssignment mpxResourceAssignment = mpxTask
                    .addResourceAssignment(mpxResource);
            mpxResourceAssignment.setUnits(ganttAssignment.getLoad());
            mpxResourceAssignment.setWork(resourceWork);
            mpxResourceAssignment.setActualWork(resourceActualWork);
        }
    }

    /**
     * Main method called to save Gantt Project data in a Microsoft Project file
     * format.
     * 
     * @param file
     *            output file
     * @param mpx
     *            MPXJ file instance
     */
    protected void save(File file, MPXFile mpx) throws Exception {
        m_mpx = mpx;
        m_mpx.setAutoCalendarUniqueID(true);
        m_mpx.setAutoOutlineLevel(true);
        m_mpx.setAutoOutlineNumber(true);
        m_mpx.setAutoResourceID(true);
        m_mpx.setAutoResourceUniqueID(true);
        m_mpx.setAutoTaskID(true);
        m_mpx.setAutoTaskUniqueID(true);
        m_mpx.setAutoWBS(true);
        m_mpx.addDefaultBaseCalendar();

        ProjectHeader header = m_mpx.getProjectHeader();
        header.setProjectTitle(m_project.getProjectName());
        header.setCompany(m_project.getOrganization());
        header.setComments(m_project.getDescription());

        processResources();
        processTasks();
        processRelationships();
        processAssignments();

        header.setStartDate(mpx.getStartDate());

        mpx.write(file);
    }
}
