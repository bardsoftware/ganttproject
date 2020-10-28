/*
Copyright 2004-2012 GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package biz.ganttproject.core.chart.scene.gantt;

import java.util.ArrayList;
import java.util.List;

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.HAlignment;
import biz.ganttproject.core.chart.canvas.Canvas.Polygon;
import biz.ganttproject.core.chart.canvas.Canvas.Text;
import biz.ganttproject.core.chart.canvas.Canvas.VAlignment;
import biz.ganttproject.core.chart.scene.BarChartActivity;
import biz.ganttproject.core.option.EnumerationOption;

/**
 * This class is responsible for rendering text labels on the sides of task bars.
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class TaskLabelSceneBuilder<T> {
  public static final String ID_TASK_DATES = "taskDates";

  public static final String ID_TASK_NAME = "name";

  public static final String ID_TASK_LENGTH = "length";

  public static final String ID_TASK_ADVANCEMENT = "advancement";

  public static final String ID_TASK_COORDINATOR = "coordinator";

  public static final String ID_TASK_RESOURCES = "resources";

  public static final String ID_TASK_ID = "id";

  public static final String ID_TASK_PREDECESSORS = "predecessors";

  public static final int UP = 0;

  public static final int DOWN = 1;

  public static final int LEFT = 2;

  public static final int RIGHT = 3;

  private EnumerationOption[] myLabelOptions;

  private Canvas myCanvas;

  private static List<String> ourInfoList;

  private final TaskApi<T> myTaskApi;

  private final InputApi myInputApi;

  public interface TaskApi<T> {
    Object getProperty(T task, String propertyID);
  }

  public interface InputApi {
    EnumerationOption getTopLabelOption();
    EnumerationOption getBottomLabelOption();
    EnumerationOption getLeftLabelOption();
    EnumerationOption getRightLabelOption();

    int getFontSize();
  }

  static {
    ourInfoList = new ArrayList<String>();
    ourInfoList.add("");
    ourInfoList.add(ID_TASK_ID);
    ourInfoList.add(ID_TASK_DATES);
    ourInfoList.add(ID_TASK_NAME);
    ourInfoList.add(ID_TASK_LENGTH);
    ourInfoList.add(ID_TASK_ADVANCEMENT);
    ourInfoList.add(ID_TASK_COORDINATOR);
    ourInfoList.add(ID_TASK_RESOURCES);
    ourInfoList.add(ID_TASK_PREDECESSORS);
  }

  public TaskLabelSceneBuilder(TaskApi<T> taskApi, InputApi inputApi, Canvas canvas) {
    myCanvas = canvas;
    myTaskApi = taskApi;
    myInputApi = inputApi;

    myLabelOptions = new EnumerationOption[] { inputApi.getTopLabelOption(), inputApi.getBottomLabelOption(), inputApi.getLeftLabelOption(), inputApi.getRightLabelOption() };
  }

  public void renderLabels(List<Canvas.Polygon> activityRectangles) {
    Polygon lastRectangle = activityRectangles.get(activityRectangles.size() - 1);

    if (lastRectangle.isVisible()) {
      createRightSideText(lastRectangle);
      createDownSideText(lastRectangle);
      createUpSideText(lastRectangle);
    }
    Polygon firstRectangle = activityRectangles.get(0);
    if (firstRectangle.isVisible()) {
      createLeftSideText(firstRectangle);
    }
  }
  
  private void createRightSideText(Polygon rectangle) {
    BarChartActivity<T> activity = (BarChartActivity<T>) rectangle.getModelObject();
    String text = "";
    int xText, yText;

    text = getTaskLabel(activity.getOwner(), RIGHT);

    if (text.length() != 0) {
      xText = rectangle.getRightX() + 9;
      yText = rectangle.getMiddleY();
      Text textPrimitive = processText(xText, yText, text);
      textPrimitive.setAlignment(HAlignment.LEFT, VAlignment.CENTER);
    }
  }

  private void createDownSideText(Polygon rectangle) {
    BarChartActivity<T> activity = (BarChartActivity<T>) rectangle.getModelObject();
    String text = getTaskLabel(activity.getOwner(), DOWN);

    if (text.length() > 0) {
      int xOrigin = rectangle.getRightX();
      int yOrigin = rectangle.getBottomY() + TINY_SPACE;
      Text textPrimitive = processText(xOrigin, yOrigin, text);
      textPrimitive.setAlignment(HAlignment.RIGHT, VAlignment.TOP);
    }
  }

  private void createUpSideText(Polygon rectangle) {
    BarChartActivity<T> activity = (BarChartActivity<T>) rectangle.getModelObject();
    String text = getTaskLabel(activity.getOwner(), UP);
    if (text.length() > 0) {
      int xOrigin = rectangle.getRightX();
      int yOrigin = rectangle.getTopY() - TINY_SPACE;
      Text textPrimitive = processText(xOrigin, yOrigin, text);
      textPrimitive.setAlignment(HAlignment.RIGHT, VAlignment.BOTTOM);
    }
  }

  private void createLeftSideText(Polygon rectangle) {
    BarChartActivity<T> activity = (BarChartActivity<T>) rectangle.getModelObject();
    String text = getTaskLabel(activity.getOwner(), LEFT);

    if (text.length() > 0) {
      int xOrigin = rectangle.getLeftX() - 9;
      int yOrigin = rectangle.getMiddleY();
      Text textPrimitive = processText(xOrigin, yOrigin, text);
      textPrimitive.setAlignment(HAlignment.RIGHT, VAlignment.CENTER);
    }
  }

  private Text processText(int xorigin, int yorigin, String text) {
    return processText(xorigin, yorigin, text, "text.ganttinfo");
  }

  private Text processText(int xorigin, int yorigin, String text, String style) {
    Text res = getPrimitiveContainer().createText(xorigin, yorigin, text);
    res.setStyle(style);
    return res;
  }


  private String getTaskLabel(T task, int position) {
    StringBuffer result = new StringBuffer();
    Object property = myTaskApi.getProperty(task, myLabelOptions[position].getValue());
    if (property != null) {
      result.append(property);
    }
    return result.toString();
  }

  private Canvas getPrimitiveContainer() {
    return myCanvas;
  }

  public boolean isTextUp() {
    return myLabelOptions[UP].getValue() != null && myLabelOptions[UP].getValue().length() != 0;
  }

  public boolean isTextDown() {
    return myLabelOptions[DOWN].getValue() != null && myLabelOptions[DOWN].getValue().length() != 0;
  }

  boolean isOnlyUp() {
    return isTextUp() && !isTextDown();
  }

  boolean isOnlyDown() {
    return isTextDown() && !isTextUp();
  }

  static final int TINY_SPACE = 2;
  static final int MEDIUM_SPACE = 3;
  static final int LARGE_SPACE = 4;

  public int calculateRowHeight() {
    boolean textUP = isTextUp();
    boolean textDOWN = isTextDown();

    int result;
    if (textUP && textDOWN) {
      result = getFontHeight() * 3 + 4 * TINY_SPACE;
    } else if (textUP || textDOWN) {
      result = getFontHeight() * 2 + 3 * MEDIUM_SPACE;
    } else {
      result = getFontHeight() + 2 * LARGE_SPACE;
    }
    return result;
  }

  public void stripVerticalLabelSpace(java.awt.Rectangle nextBounds) {
    if (isTextUp()) {
      nextBounds.y += getFontHeight();
    }

    int space;
    if (isTextUp() && isTextDown()) {
      space = TINY_SPACE * 2;
    } else if (!isTextUp() && !isTextDown()) {
      space = LARGE_SPACE;
    } else if (isTextUp()) {
      space = MEDIUM_SPACE * 2;
    } else {
      space = MEDIUM_SPACE;
    }
    nextBounds.y += space;
  }

  public int getFontHeight() {
    return myInputApi.getFontSize();
  }
}