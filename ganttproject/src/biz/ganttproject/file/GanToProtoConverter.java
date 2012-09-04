package biz.ganttproject.file;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import biz.ganttproject.core.proto.FileProto;
import biz.ganttproject.core.proto.ProjectProto;
import biz.ganttproject.core.proto.TaskProto;
import biz.ganttproject.core.proto.TaskProto.Dependency.Stiffness;
import biz.ganttproject.core.proto.ViewProto;
import biz.ganttproject.core.proto.ViewProto.GanttView;
import biz.ganttproject.core.proto.ViewProto.GanttView.Builder;
import biz.ganttproject.core.proto.ViewProto.View;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.TextFormat;

public class GanToProtoConverter {
  private static Document readDocument(InputStream inputStream) throws JDOMException, IOException {
    SAXBuilder saxBuilder = new SAXBuilder();
    return saxBuilder.build(inputStream);
  }

  private static FileProto.File readFile(Document doc) throws JDOMException {
    Element elProject = doc.getRootElement();
    ProjectProto.Project.Builder projectBuilder = ProjectProto.Project.newBuilder();
    projectBuilder.setName(elProject.getAttributeValue("name"))
      .setOrganization(elProject.getAttributeValue("company"))
      .setProjectUrl(elProject.getAttributeValue("webLink"));
    projectBuilder.setTaskList(TaskProto.TaskList.newBuilder());

    FileProto.File.Builder fileBuilder = FileProto.File.newBuilder();
    fileBuilder = readViews(elProject, fileBuilder);
    readTaskList(projectBuilder, elProject.getChild("tasks"));
    readDependencyList(projectBuilder, elProject.getChild("tasks"));
    fileBuilder.setProject(projectBuilder);
    return fileBuilder.build();
  }

  private static ProjectProto.Project.Builder readTaskList(ProjectProto.Project.Builder projectBuilder, Element elTasks) {
    TaskProto.TaskList.Builder taskListBuilder = TaskProto.TaskList.newBuilder();
    readTasks(taskListBuilder, elTasks, null);
    projectBuilder.setTaskList(taskListBuilder);
    return projectBuilder;
  }


  private static void readTasks(TaskProto.TaskList.Builder taskListBuilder, Element elRoot, TaskProto.Task parentTask) {
    List<Element> tasks = elRoot.getChildren("task");
    for (Element elTask : tasks) {
      TaskProto.Task.Builder taskBuilder = TaskProto.Task.newBuilder();
      taskBuilder.setId(Integer.parseInt(elTask.getAttributeValue("id")))
        .setName(elTask.getAttributeValue("name"))
        .addActivity(TaskProto.Task.Activity.newBuilder().setStartDate(elTask.getAttributeValue("start")).setDuration(elTask.getAttributeValue("duration")));
      if (Boolean.TRUE == Boolean.parseBoolean(elTask.getAttributeValue("meeting"))) {
        taskBuilder.setIsMilestone(true);
      }
      if (parentTask != null) {
        taskBuilder.setParent(parentTask.getId());
      }
      String priority = elTask.getAttributeValue("priority");
      if (priority != null && Integer.parseInt(priority) < TaskProto.Task.Priority.values().length) {
        taskBuilder.setPriority(TaskProto.Task.Priority.values()[Integer.parseInt(priority)]);
      }

      String earliestStart = elTask.getAttributeValue("thirdDate");
      if (earliestStart != null) {
        taskBuilder.setEarliestStart(TaskProto.EarliestStartConstraint.newBuilder().setDate(earliestStart));
      }

      String webLink = elTask.getAttributeValue("webLink");
      if (webLink != null) {
        try {
          taskBuilder.setUrl(URLDecoder.decode(webLink, "ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace(System.err);
        }
      }

      String shape = elTask.getAttributeValue("shape");
      if (shape != null) {
        int shapeBitmap = 0;
        String[] bits = shape.split(",");
        for (int i = 0; i < bits.length; i++) {
          if ("1".equals(bits[i])) {
            shapeBitmap |= 1 << (15 - i);
          }
        }
        taskBuilder.setShape(shapeBitmap);
      }
      TaskProto.Task task = taskBuilder.build();
      taskListBuilder.addTask(task);
      readTasks(taskListBuilder, elTask, task);
    }
  }

  private static void readDependencyList(ProjectProto.Project.Builder projectBuilder, Element elTasks) throws JDOMException {
    TaskProto.DependencyList.Builder depListBuilder = TaskProto.DependencyList.newBuilder();
    XPath xpath = XPath.newInstance("//depend");
    List<Element> elDepends = xpath.selectNodes(elTasks);
    for (Element elDependency : elDepends) {
      TaskProto.Dependency.Builder depBuilder = TaskProto.Dependency.newBuilder()
          .setDependeeTaskId(Integer.parseInt(elDependency.getParentElement().getAttributeValue("id")))
          .setDependantTaskId(Integer.parseInt(elDependency.getAttributeValue("id")))
          .setType(TaskProto.Dependency.Type.valueOf(Integer.parseInt(elDependency.getAttributeValue("type"))));
      if (!TaskProto.Dependency.getDefaultInstance().getLag().equals(elDependency.getAttributeValue("difference"))) {
        depBuilder.setLag(elDependency.getAttributeValue("difference"));
      }
      Stiffness stiffness = TaskProto.Dependency.Stiffness.valueOf(TaskProto.Dependency.Stiffness.class, elDependency.getAttributeValue("hardness").toUpperCase());
      if (!TaskProto.Dependency.getDefaultInstance().getStiffness().equals(stiffness)) {
        depBuilder.setStiffness(stiffness);
      }
      depListBuilder.addDependency(depBuilder);
    }
    projectBuilder.setDependencyList(depListBuilder);
  }


  private static final Map<String, ViewProto.GanttView.TaskLabel.Position> ourLabelPositions = ImmutableMap.of(
      "taskLabelUp", ViewProto.GanttView.TaskLabel.Position.TOP,
      "taskLabelLeft", ViewProto.GanttView.TaskLabel.Position.LEFT,
      "taskLabelDown", ViewProto.GanttView.TaskLabel.Position.BOTTOM,
      "taskLabelRight", ViewProto.GanttView.TaskLabel.Position.RIGHT);

  private static FileProto.File.Builder readViews(Element elProject, FileProto.File.Builder fileBuilder) throws JDOMException {
    View.Builder[] viewBuilders = new View.Builder[] {readGanttView(elProject), readResourceView(elProject)};
    viewBuilders[Integer.parseInt(elProject.getAttributeValue("view-index"))].setIsActive(true);
    ViewProto.GanttView.Builder ganttBuilder = GanttView.newBuilder().setBaseView(viewBuilders[0]);
    XPath xpath = XPath.newInstance("view[@id='gantt-chart']//option");
    List<Element> options = xpath.selectNodes(elProject);
    for (Element elOption : options) {
      ViewProto.GanttView.TaskLabel.Position position = ourLabelPositions.get(elOption.getAttributeValue("id"));
      if (position == null) {
        continue;
      }
      ganttBuilder.addLabel(
          ViewProto.GanttView.TaskLabel.newBuilder().setPosition(position).setPropertyId(elOption.getAttributeValue("value")));
    }
    fileBuilder.addGanttView(ganttBuilder);
    return fileBuilder;
  }

  private static View.Builder readResourceView(Element elProject) {
    return ViewProto.View.newBuilder().setTableWidth(Integer.parseInt(elProject.getAttributeValue("resource-divider-location")))
        .setViewportStartDate(elProject.getAttributeValue("view-date"));
  }

  private static View.Builder readGanttView(Element elProject) throws JDOMException {
    View.Builder builder = ViewProto.View.newBuilder().setTableWidth(Integer.parseInt(elProject.getAttributeValue("gantt-divider-location")))
        .setViewportStartDate(elProject.getAttributeValue("view-date"));
    XPath xpath = XPath.newInstance("tasks//task[@expand='true']");
    List<Element> expandedTasks = xpath.selectNodes(elProject);
    for (Element task : expandedTasks) {
      builder.addExpandedNode(Integer.parseInt(task.getAttributeValue("id")));
    }
    return builder;
  }

  public static void main(String[] args) throws Exception {
    FileProto.File file = readFile(readDocument(new FileInputStream(args[0])));
    file.writeTo(System.out);
  }
}
