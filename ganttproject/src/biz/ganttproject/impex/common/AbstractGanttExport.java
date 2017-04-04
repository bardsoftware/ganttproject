package biz.ganttproject.impex.common;

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.option.BooleanOption;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.sourceforge.ganttproject.*;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskProperties;
import net.sourceforge.ganttproject.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: akurutin
 * Date: 03.04.17
 * Time: 19:35
 */
public abstract class AbstractGanttExport implements GanttExport {
  private static final Predicate<ResourceAssignment> COORDINATOR_PREDICATE = new Predicate<ResourceAssignment>() {
    public boolean apply(ResourceAssignment arg) {
      return arg.isCoordinator();
    }
  };

  private final TaskManager myTaskManager;
  private final CustomPropertyManager myTaskCustomPropertyManager;
  private final HumanResourceManager myHumanResourceManager;
  private final CustomPropertyManager myHumanResourceCustomPropertyManager;
  private final RoleManager myRoleManager;

  public AbstractGanttExport(IGanttProject project) {
    this(project.getTaskManager(), project.getHumanResourceManager(), project.getRoleManager());
  }

  AbstractGanttExport(TaskManager taskManager, HumanResourceManager resourceManager, RoleManager roleManager) {
    myTaskManager = Preconditions.checkNotNull(taskManager);
    myTaskCustomPropertyManager = Preconditions.checkNotNull(taskManager.getCustomPropertyManager());
    myHumanResourceManager = Preconditions.checkNotNull(resourceManager);
    myHumanResourceCustomPropertyManager = Preconditions.checkNotNull(resourceManager.getCustomPropertyManager());
    myRoleManager = Preconditions.checkNotNull(roleManager);
  }

  public abstract GanttExportWriter getWriter(OutputStream stream) throws IOException;

  public abstract Map<String, BooleanOption> getTaskOptions();

  public abstract Map<String, BooleanOption> getResourceOptions();

  public abstract boolean bFixedSize();

  public abstract String sSeparatedChar();

  public abstract String sSeparatedTextChar();

  /**
   * Save the project as CSV on a stream
   *
   * @throws IOException
   */
  public void save(OutputStream stream) throws IOException {
    GanttExportWriter writer = getWriter(stream);

//    if (csvOptions.bFixedSize) {
//      // TODO The CVS library we use is lacking support for fixed size
//      getMaxSize();
//    }

    writeTasks(writer);

    if (myHumanResourceManager.getResources().size() > 0) {
      writer.println();
      writer.println();
      writeResources(writer);
    }
    writer.flush();
    writer.close();
  }

  private List<CustomPropertyDefinition> writeTaskHeaders(GanttExportWriter writer) throws IOException {
    List<CustomPropertyDefinition> defs = myTaskCustomPropertyManager.getDefinitions();
    for (Map.Entry<String, BooleanOption> entry : getTaskOptions().entrySet()) {
      TaskDefaultColumn defaultColumn = TaskDefaultColumn.find(entry.getKey());
      if (!entry.getValue().isChecked()) {
        continue;
      }
      if (defaultColumn == null) {
        writer.print(i18n(entry.getKey()));
      } else {
        writer.print(defaultColumn.getName());
      }
    }
    for (CustomPropertyDefinition def : defs) {
      writer.print(def.getName());
    }
    writer.println();
    return defs;
  }

  private String i18n(String key) {
    return GanttLanguage.getInstance().getText(key);
  }

  /**
   * Write all tasks.
   *
   * @throws IOException
   */
  private void writeTasks(GanttExportWriter writer) throws IOException {
    List<CustomPropertyDefinition> customFields = writeTaskHeaders(writer);
    for (Task task : myTaskManager.getTasks()) {
      for (Map.Entry<String, BooleanOption> entry : getTaskOptions().entrySet()) {
        if (!entry.getValue().isChecked()) {
          continue;
        }
        TaskDefaultColumn defaultColumn = TaskDefaultColumn.find(entry.getKey());
        if (defaultColumn == null) {
          if ("webLink".equals(entry.getKey())) {
            writer.print(getWebLink((GanttTask) task));
            continue;
          }
          if ("notes".equals(entry.getKey())) {
            writer.print(task.getNotes());
            continue;
          }
        } else {
          switch (defaultColumn) {
            case ID:
              writer.print(String.valueOf(task.getTaskID()));
              break;
            case NAME:
              writer.print(getName(task));
              break;
            case BEGIN_DATE:
              writer.print(task.getStart().toString());
              break;
            case END_DATE:
              writer.print(task.getDisplayEnd().toString());
              break;
            case DURATION:
              writer.print(String.valueOf(task.getDuration().getLength()));
              break;
            case COMPLETION:
              writer.print(String.valueOf(task.getCompletionPercentage()));
              break;
            case OUTLINE_NUMBER:
              List<Integer> outlinePath = task.getManager().getTaskHierarchy().getOutlinePath(task);
              writer.print(Joiner.on('.').join(outlinePath));
              break;
            case COORDINATOR:
              ResourceAssignment coordinator = Iterables.tryFind(Arrays.asList(task.getAssignments()), COORDINATOR_PREDICATE).orNull();
              writer.print(coordinator == null ? "" : coordinator.getResource().getName());
              break;
            case PREDECESSORS:
              writer.print(TaskProperties.formatPredecessors(task, ";", true));
              break;
            case RESOURCES:
              writer.print(getAssignments(task));
              break;
            case COST:
              writer.print(task.getCost().getValue().toPlainString());
              break;
            case INFO:
            case PRIORITY:
            case TYPE:
              break;
          }
        }
      }
      writeCustomPropertyValues(writer, customFields, task.getCustomValues().getCustomProperties());
    }
  }

  private List<CustomPropertyDefinition> writeResourceHeaders(GanttExportWriter writer) throws IOException {
    for (Map.Entry<String, BooleanOption> entry : getResourceOptions().entrySet()) {
      ResourceDefaultColumn defaultColumn = ResourceDefaultColumn.find(entry.getKey());
      if (!entry.getValue().isChecked()) {
        continue;
      }
      if (defaultColumn == null) {
        if ("id".equals(entry.getKey())) {
          writer.print(i18n("tableColID"));
        } else {
          writer.print(i18n(entry.getKey()));
        }
      } else {
        writer.print(defaultColumn.getName());
      }
    }
    List<CustomPropertyDefinition> customFieldDefs = myHumanResourceCustomPropertyManager.getDefinitions();
    for (int i = 0; i < customFieldDefs.size(); i++) {
      CustomPropertyDefinition nextDef = customFieldDefs.get(i);
      writer.print(nextDef.getName());
    }
    writer.println();
    return customFieldDefs;
  }

  /**
   * write the resources.
   *
   * @throws IOException
   */
  private void writeResources(GanttExportWriter writer) throws IOException {
    Set<Role> projectRoles = Sets.newHashSet(myRoleManager.getProjectLevelRoles());
    List<CustomPropertyDefinition> customPropDefs = writeResourceHeaders(writer);
    // parse all resources
    for (HumanResource p : myHumanResourceManager.getResources()) {
      for (Map.Entry<String, BooleanOption> entry : getResourceOptions().entrySet()) {
        if (!entry.getValue().isChecked()) {
          continue;
        }
        ResourceDefaultColumn defaultColumn = ResourceDefaultColumn.find(entry.getKey());
        if (defaultColumn == null) {
          if ("id".equals(entry.getKey())) {
            writer.print(String.valueOf(p.getId()));
            continue;
          }
        } else {
          switch (defaultColumn) {
            case NAME:
              writer.print(p.getName());
              break;
            case EMAIL:
              writer.print(p.getMail());
              break;
            case PHONE:
              writer.print(p.getPhone());
              break;
            case ROLE:
              Role role = p.getRole();
              final String sRoleID;
              if (role == null) {
                sRoleID = "0";
              } else if (projectRoles.contains(role)) {
                sRoleID = role.getName();
              } else {
                sRoleID = role.getPersistentID();
              }
              writer.print(sRoleID);
              break;
            case ROLE_IN_TASK:
              writer.print("");
              break;
            case STANDARD_RATE:
              writer.print(p.getStandardPayRate().toPlainString());
              break;
            case TOTAL_COST:
              writer.print(p.getTotalCost().toPlainString());
              break;
          }
        }
      }
      writeCustomPropertyValues(writer, customPropDefs, p.getCustomProperties());
    }
  }

  private void writeCustomPropertyValues(GanttExportWriter writer,
                                         List<CustomPropertyDefinition> defs, List<CustomProperty> values) throws IOException {
    Map<String, CustomProperty> definedProps = Maps.newHashMap();
    for (CustomProperty prop : values) {
      definedProps.put(prop.getDefinition().getID(), prop);
    }
    for (CustomPropertyDefinition def : defs) {
      CustomProperty value = definedProps.get(def.getID());
      String valueAsString = value == null ? null : Strings.nullToEmpty(value.getValueAsString());
      writer.print(valueAsString);
    }
    writer.println();

  }

  /**
   * @return the name of task with the correct level.
   */
  private String getName(Task task) {
    if (bFixedSize()) {
      return task.getName();
    }
    int depth = task.getManager().getTaskHierarchy().getDepth(task) - 1;
    return StringUtils.padLeft(task.getName(), depth * 2);
  }

  /**
   * @return the link of the task.
   */
  private String getWebLink(GanttTask task) {
    return (task.getWebLink() == null || task.getWebLink().equals("http://") ? "" : task.getWebLink());
  }

  /**
   * @return the list of the assignment for the resources.
   */
  private String getAssignments(Task task) {
    String res = "";
    ResourceAssignment[] assignment = task.getAssignments();
    for (int i = 0; i < assignment.length; i++) {
      res += (assignment[i].getResource() + (i == assignment.length - 1 ? ""
          : sSeparatedChar().equals(";") ? "," : ";"));
    }
    return res;
  }


}
