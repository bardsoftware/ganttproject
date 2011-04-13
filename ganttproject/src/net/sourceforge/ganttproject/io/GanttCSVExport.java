/***************************************************************************
 GanttCSVExport.java
 -----------------
 begin                : 7 juil. 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas@ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package net.sourceforge.ganttproject.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.CustomColumnsValues;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.util.StringUtils;

/**
 * Class to export the project in csv text format
 * 
 * @author athomas
 */
public class GanttCSVExport {
    private final IGanttProject myProject;

    private final CSVOptions csvOptions;

    private final HumanResourceManager myHrManager;

    private final Task[] myTasks;

    private List<HumanResource> resources;

    private int iMaxSize = 0;

    /** Create a table like output with fixed column widths */
    private boolean bFixedSize;

    public GanttCSVExport(IGanttProject project, CSVOptions csvOptions) {
    	myProject = project;
        myTasks = project.getTaskManager().getTasks();
        myHrManager = project.getHumanResourceManager();
        this.csvOptions = csvOptions;
        bFixedSize = csvOptions.bFixedSize;
    }

    /** Save the project as CSV on a stream 
     * @throws IOException */
    public void save(OutputStream stream) throws IOException {
        OutputStreamWriter out = new OutputStreamWriter(stream);
        beginToSave(out);
        out.close();
    }

    /** Start saving the csv document. */
    private void beginToSave(OutputStreamWriter out) throws IOException {
        resources = myHrManager.getResources();

        if (csvOptions.bFixedSize) {
            getMaxSize();
        }

        writeTasks(out);
        out.write("\n");

        if(resources.size() > 0) {
            out.write("\n\n");
            writeResources(out);
            out.write("\n");
        }
    }

    private void writeCell(OutputStreamWriter out, String cellValue) throws IOException {
        out.write(correctField(cellValue)
                + (bFixedSize ? "" : csvOptions.sSeparatedChar));
	}

    private void writeTextCell(OutputStreamWriter out, String cellValue) throws IOException {
        out.write((bFixedSize ? "" : csvOptions.sSeparatedTextChar)
                + correctField(cellValue)
                + (bFixedSize ? "" : csvOptions.sSeparatedTextChar
                        + csvOptions.sSeparatedChar));    	
    }

    private void writeTaskHeaders(OutputStreamWriter out) throws IOException {
        if (csvOptions.bExportTaskID) {
        	writeCell(out, i18n("tableColID"));
        }
        if (csvOptions.bExportTaskName) {
        	writeCell(out, i18n("tableColName"));
        }
        if (csvOptions.bExportTaskStartDate) {
        	writeCell(out, i18n("tableColBegDate"));
        }
        if (csvOptions.bExportTaskEndDate) {
        	writeCell(out, i18n("tableColEndDate"));        	
        }
        if (csvOptions.bExportTaskDuration) {
        	writeCell(out, i18n("tableColDuration"));        	        	
        }
        if (csvOptions.bExportTaskPercent) {
        	writeCell(out, i18n("tableColCompletion"));	        	
        }
        if (csvOptions.bExportTaskWebLink) {
        	writeCell(out, i18n("webLink"));
        }
        if (csvOptions.bExportTaskResources) {
        	writeCell(out, i18n("resources"));        	
        }
        if (csvOptions.bExportTaskNotes) {
        	writeCell(out, i18n("notes"));        	        	
        }
        List<String> customFields = myProject.getCustomColumnsStorage().getCustomColumnsNames();
        for (int i = 0; i < customFields.size(); i++) {
        	writeCell(out, String.valueOf(customFields.get(i)));
        }
        out.write("\n\n");
	}

	private String i18n(String key) {
		return GanttLanguage.getInstance().getText(key);
	}

	/** Write all tasks. */
    private void writeTasks(OutputStreamWriter out) throws IOException {
    	writeTaskHeaders(out);
        List<String> customFields = myProject.getCustomColumnsStorage().getCustomColumnsNames();
        for (int i = 0; i < myTasks.length; i++) {
            Task task = myTasks[i];
            // ID
            if (csvOptions.bExportTaskID) {
                writeCell(out, String.valueOf(task.getTaskID()));
            }
            // Name
            if (csvOptions.bExportTaskName) {
                writeTextCell(out, getName(task));
            }
            // Start Date
            if (csvOptions.bExportTaskStartDate) {
                writeCell(out, task.getStart().toString());
            }
            // End Date
            if (csvOptions.bExportTaskEndDate) {
                writeCell(out, task.getEnd().toString());
            }
            // Duration
            if (csvOptions.bExportTaskDuration) {
                writeCell(out, ""+task.getDuration().getLength());
            }
            // Percent complete
            if (csvOptions.bExportTaskPercent) {
                writeCell(out, "" + task.getCompletionPercentage());
            }
            // Web Link
            if (csvOptions.bExportTaskWebLink) {
                writeTextCell(out, getWebLink((GanttTask) task));
            }
            // associated resources
            if (csvOptions.bExportTaskResources) {
            	writeTextCell(out, getAssignments(task));
            }
            // Notes
            if (csvOptions.bExportTaskNotes) {
            	writeTextCell(out, task.getNotes());
            }
            CustomColumnsValues customValues = task.getCustomValues();
            for (int j = 0; j < customFields.size(); j++) {
            	Object nextCustomFieldValue = customValues.getValue(String.valueOf(customFields.get(j)));
            	writeCell(out, String.valueOf(nextCustomFieldValue));
            }
            out.write("\n");
        }
    }

    private void writeResourceHeaders(OutputStreamWriter out) throws IOException {
        if (csvOptions.bExportResourceID) {
        	writeCell(out, i18n("tableColID"));
        }
        if (csvOptions.bExportResourceName) {
        	writeCell(out, i18n("tableColResourceName"));
        }		
        if (csvOptions.bExportResourceMail) {
        	writeCell(out, i18n("tableColResourceEMail"));
        }
        if (csvOptions.bExportResourcePhone) {
        	writeCell(out, i18n("tableColResourcePhone"));
        }
        if (csvOptions.bExportResourceRole) {
        	writeCell(out, i18n("tableColResourceRole"));        	
        }
        List<CustomPropertyDefinition> customFieldDefs = myProject.getResourceCustomPropertyManager().getDefinitions();
        for (int i=0; i<customFieldDefs.size(); i++) {
        	CustomPropertyDefinition nextDef = customFieldDefs.get(i);
        	writeCell(out, nextDef.getName());
        }
        out.write("\n\n");
    }

	/** write the resources. */
    private void writeResources(OutputStreamWriter out) throws IOException {
    	writeResourceHeaders(out);
        // parse all resources
        for (int i = 0; i < resources.size(); i++) {
            HumanResource p = resources.get(i);

            // ID
            if (csvOptions.bExportResourceID) {
                writeCell(out, "" + p.getId());
            }
            // Name
            if (csvOptions.bExportResourceName) {
                writeTextCell(out, p.getName());
            }
            // Mail
            if (csvOptions.bExportResourceMail) {
                writeTextCell(out, p.getMail());
            }
            // Phone
            if (csvOptions.bExportResourcePhone) {
                writeTextCell(out,p.getPhone());
            }
            // Role
            if (csvOptions.bExportResourceRole) {
                Role role = p.getRole();
                String sRoleID = role==null ? "0":role.getPersistentID();
                writeTextCell(out, sRoleID);
            }
            List<CustomProperty> customProps = p.getCustomProperties();
            for (int j=0; j<customProps.size(); j++) {
            	CustomProperty nextProperty = customProps.get(j);
            	writeTextCell(out, nextProperty.getValueAsString());
            }
            out.write("\n");
        }
    }


	/** set the maximum size for all strings. */
    private void getMaxSize() {
        List<String> customFields = myProject.getCustomColumnsStorage().getCustomColumnsNames();
        iMaxSize = 0;
        for (int i = 0; i < myTasks.length; i++) {
            Task task = myTasks[i];

            if (csvOptions.bExportTaskID) {
                String s = String.valueOf(task.getTaskID());
                if (s.length() > iMaxSize) {
                    iMaxSize = s.length();
                }
            }

            if (csvOptions.bExportTaskName) {
                String s = getName(task);
                if (s.length() > iMaxSize) {
                    iMaxSize = s.length();
                }
            }

            if (csvOptions.bExportTaskStartDate) {
                String s = String.valueOf(task.getStart());
                if (s.length() > iMaxSize) {
                    iMaxSize = s.length();
                }
            }

            if (csvOptions.bExportTaskEndDate) {
                String s = String.valueOf(task.getEnd());
                if (s.length() > iMaxSize) {
                    iMaxSize = s.length();
                }
            }

            if (csvOptions.bExportTaskDuration) {
                String s = String.valueOf(task.getDuration().getLength());
                if (s.length() > iMaxSize) {
                    iMaxSize = s.length();
                }
            }

            if (csvOptions.bExportTaskPercent) {
                String s = String.valueOf(task.getCompletionPercentage());
                if (s.length() > iMaxSize) {
                    iMaxSize = s.length();
                }
            }

            if (csvOptions.bExportTaskWebLink) {
                String s = getWebLink((GanttTask) task);
                if (s.length() > iMaxSize) {
                    iMaxSize = s.length();
                }
            }

            if (csvOptions.bExportTaskResources) {
                String s = getAssignments(task);
                if (s.length() > iMaxSize) {
                    iMaxSize = s.length();
                }
            }

            if (csvOptions.bExportTaskNotes) {
                String s = task.getNotes();
                if (s.length() > iMaxSize) {
                    iMaxSize = s.length();
                }
            }

            CustomColumnsValues customValues = task.getCustomValues();
            for (int j=0; j<customFields.size(); j++) {
            	Object nextCustomFieldValue = customValues.getValue(String.valueOf(customFields.get(j)));
            	String nextValueAsString = String.valueOf(nextCustomFieldValue); 
            	if (nextValueAsString.length() > iMaxSize) {
            		iMaxSize = nextValueAsString.length();
            	}
            }
        }

        // parse all resources
        for (int i = 0; i < resources.size(); i++) {
            HumanResource p = resources.get(i);

            if (csvOptions.bExportResourceID) {
                String s = String.valueOf(p.getId());
                if (s.length() > iMaxSize) {
                    iMaxSize = s.length();
                }
            }
            if (csvOptions.bExportResourceName) {
                String s = p.getName();
                if (s.length() > iMaxSize) {
                    iMaxSize = s.length();
                }
            }
            if (csvOptions.bExportResourceMail) {
                String s = p.getMail();
                if (s.length() > iMaxSize)
                    iMaxSize = s.length();
            }
            if (csvOptions.bExportResourcePhone) {
                String s = p.getPhone();
                if (s.length() > iMaxSize) {
                    iMaxSize = s.length();
                }
            }
            if (csvOptions.bExportResourceRole) {
                Role role = p.getRole();
                String sRoleID;
                if (role != null) {
                    sRoleID = role.getPersistentID();
                } else {
                    sRoleID = "0";
                }
                if (sRoleID.length() > iMaxSize) {
                    iMaxSize = sRoleID.length();
                }
            }
            List<CustomProperty> customProps = p.getCustomProperties();
            for (int j=0; j<customProps.size(); j++) {
            	CustomProperty nextProperty = customProps.get(j);
            	if (nextProperty.getValueAsString().length() > iMaxSize) {
            		iMaxSize = nextProperty.getValueAsString().length();
            	}
            }
        }
    }

    /** @return the name of task with the correct level. */
    private String getName(Task task) {
        if (bFixedSize) {
            return task.getName();
        }
        int depth = task.getManager().getTaskHierarchy().getDepth(task);
        return StringUtils.padLeft(task.getName(), depth * 2);
    }

    /** @return the link of the task. */
    private String getWebLink(GanttTask task) {
        return (task.getWebLink().equals("http://") ? "" : task.getWebLink());
    }

    /** @return the list of the assignment for the resources. */
    private String getAssignments(Task task) {
        String res = "";
        ResourceAssignment[] assignment = task.getAssignments();
        for (int i = 0; i < assignment.length; i++) {
            res += (assignment[i].getResource() + (i == assignment.length - 1 ? ""
                    : csvOptions.sSeparatedChar.equals(";") ? "," : ";"));
        }
        return res;
    }

    private String correctField(String field) {
        return StringUtils.padLeft(field, iMaxSize - field.length());
    }
}
