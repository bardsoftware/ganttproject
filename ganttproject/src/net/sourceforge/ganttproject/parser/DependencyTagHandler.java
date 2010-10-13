package net.sourceforge.ganttproject.parser;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.GanttTaskRelationship;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.dependency.TaskDependency.Hardness;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;

import org.xml.sax.Attributes;

public class DependencyTagHandler implements TagHandler, ParsingListener {
    private final TaskManager myTaskManager;

	private final UIFacade myUIFacade;

    public DependencyTagHandler(ParsingContext context, TaskManager taskManager, UIFacade uiFacade) {
        myContext = context;
        myTaskManager = taskManager;
        myUIFacade = uiFacade;
    }

    /**
     * @see net.sourceforge.ganttproject.parser.TagHandler#endElement(String,
     *      String, String)
     */
    public void endElement(String namespaceURI, String sName, String qName) {
        /*
         * if ("dependencies".equals (qName)) { myDependenciesSectionStarted =
         * false; }
         */
    }

    /**
     * @see net.sourceforge.ganttproject.parser.TagHandler#startElement(String,
     *      String, String, Attributes)
     */
    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) {

        /*
         * if ("dependencies".equals (qName)) { myDependenciesSectionStarted =
         * true; }
         */
        if ("depend".equals(qName)) {
            /*
             * if (!myDependenciesSectionStarted) { throw new
             * RuntimeException("Invalid file format. Found 'dependency' tag
             * without prior 'dependencies' tag"); } else {
             */
            loadDependency(attrs);
        }
    }

    public void parsingStarted() {
    }

    public void parsingFinished() {
        for (int i = 0; i < getDependencies().size(); i++) {
            GanttDependStructure ds = getDependencies()
                    .get(i);
            Task dependee = myTaskManager.getTask(ds.taskID); // By CL
            Task dependant = myTaskManager.getTask(ds.successorTaskID);
            if (dependee == null || dependant == null) {
                continue;
            }

            try {
                TaskDependency dep = myTaskManager.getDependencyCollection()
                        .createDependency(dependant, dependee,
                                new FinishStartConstraintImpl());
                dep
                        .setConstraint(myTaskManager
                                .createConstraint(ds.dependType));
                dep.setDifference(ds.difference);
                if (myContext.getTasksWithLegacyFixedStart().contains(dependant)) {
                	dep.setHardness(TaskDependency.Hardness.RUBBER);
                }
                else {
                	dep.setHardness(ds.myHardness);
                }
            } catch (TaskDependencyException e) {
                myUIFacade.logErrorMessage(e);
            }
        }
    }

    protected void loadDependency(Attributes attrs) {
        if (attrs != null) {
            GanttDependStructure gds = new GanttDependStructure();
            gds.setTaskID(getDependencyAddressee());
            gds.setDependTaskID(getDependencyAddresser(attrs));
            String dependencyTypeAsString = attrs.getValue("type");
            String differenceAsString = attrs.getValue("difference");
            String hardnessAsString = attrs.getValue("hardness");
            if (dependencyTypeAsString != null) {
                try {
                    int dependencyType = Integer
                            .parseInt(dependencyTypeAsString);
                    gds.setDependType(dependencyType);
                } catch (NumberFormatException e) {
                }
            }
            if (differenceAsString != null) {
                try {
                    int difference = Integer.parseInt(differenceAsString);
                    gds.setDifference(difference);
                } catch (NumberFormatException e) {
                }
            }
            if (hardnessAsString!=null) {
            	TaskDependency.Hardness hardness = TaskDependency.Hardness.parse(hardnessAsString);
            	gds.setHardness(hardness);
            }
            getDependencies().add(gds);
        }
    }

    protected int getDependencyAddressee() {
        return getContext().getTaskID();
    }

    protected int getDependencyAddresser(Attributes attrs) {
        try {
            return Integer.parseInt(attrs.getValue("id"));
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                    "Failed to parse 'depend' tag. Attribute 'id' seems to be invalid: "
                            + attrs.getValue("id"), e);
        }
    }

    private List<GanttDependStructure> getDependencies() {
        return myDependencies;
    }

    private ParsingContext getContext() {
        return myContext;
    }

    private List<GanttDependStructure> myDependencies = new ArrayList<GanttDependStructure>();

    private boolean myDependenciesSectionStarted = false;

    private ParsingContext myContext;

    private class GanttDependStructure {
        public int taskID, successorTaskID;

        public int difference = 0;

        public int dependType = GanttTaskRelationship.FS; //

		private Hardness myHardness = TaskDependency.Hardness.STRONG;

        public GanttDependStructure(int a, int b) {
            taskID = a;
            successorTaskID = b;
        }

        public void setHardness(Hardness hardness) {
        	myHardness = hardness;
		}

		public GanttDependStructure(int taskID, int successorID,
                int relationType) {
            this.taskID = taskID;
            this.successorTaskID = successorID;
            this.dependType = relationType;
        }

        public GanttDependStructure(int taskID, int successorID,
                int relationType, int difference) {
            this.taskID = taskID;
            this.successorTaskID = successorID;
            this.dependType = relationType;
            this.difference = difference;
        }

        public GanttDependStructure() {
        }

        public void setTaskID(int taskID) {
            this.taskID = taskID;
        }

        public void setDifference(int difference) {
            this.difference = difference;
        }

        public void setDependTaskID(int successorTaskID) {
            this.successorTaskID = successorTaskID;
        }

        public void setDependType(int dependType) {
            this.dependType = dependType;
        }
    }

}
