package net.sourceforge.ganttproject.task.dependency;

import java.util.Date;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;

/**
 * Created by IntelliJ IDEA. User: bard Date: 14.02.2004 Time: 2:32:17 To change
 * this template use File | Settings | File Templates.
 */
public interface TaskDependency {
	abstract class Hardness {
		public static final Hardness RUBBER = new Hardness("Rubber"){
			public String toString() {
				return GanttLanguage.getInstance().getText("hardness.rubber");
			}
		};
		public static final Hardness STRONG = new Hardness("Strong"){
			public String toString() {
                return GanttLanguage.getInstance().getText("hardness.strong");
			}
		};
		public static Hardness parse(String hardnessAsString) {
			if (hardnessAsString==null) {
				throw new IllegalArgumentException("Null value is not allowed as hardness");
			}
			if ("Rubber".equals(hardnessAsString.trim())) {
				return RUBBER;
			}
			if ("Strong".equals(hardnessAsString.trim())) {
				return STRONG;
			}
			throw new IllegalArgumentException("Unexpected hardness string value="+hardnessAsString);
		}
        private String myID;
        
        private Hardness(String id) {
            myID = id;
        }
        public String getIdentifier() {
            return myID;
        }
	}
    Task getDependant();

    Task getDependee();

    void setConstraint(TaskDependencyConstraint constraint);

    TaskDependencyConstraint getConstraint();

    ActivityBinding getActivityBinding();

    void delete();

    interface ActivityBinding {
        TaskActivity getDependantActivity();

        TaskActivity getDependeeActivity();

        Date[] getAlignedBounds();
    }

    int getDifference();

    void setDifference(int difference);
    
    Hardness getHardness();
    void setHardness(Hardness hardness);
}
