/*
 * This code is provided under the terms of GPL version 3.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.HAlignment;
import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;
import biz.ganttproject.core.chart.canvas.Canvas.Text;
import biz.ganttproject.core.chart.canvas.Canvas.VAlignment;
import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.GPOptionGroup;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskProperties;

/**
 * This class is responsible for rendering text labels on the sides of task bars
 * It keeps the rendering options and it lays out the labels on rendering time
 */
class TaskLabelsRendererImpl {
  public static final int UP = 0;

  public static final int DOWN = 1;

  public static final int LEFT = 2;

  public static final int RIGHT = 3;

  private EnumerationOption[] myLabelOptions;

  private Canvas myCanvas;

  private ChartOptionGroup myOptionGroup;

  private static List<String> ourInfoList;

  private TaskProperties myLabelFormatter;

  private Font myFont;

  static {
    ourInfoList = new ArrayList<String>();
    ourInfoList.add("");
    ourInfoList.add("id");
    ourInfoList.add("taskDates");
    ourInfoList.add("name");
    ourInfoList.add("length");
    ourInfoList.add("advancement");
    ourInfoList.add("coordinator");
    ourInfoList.add("resources");
    ourInfoList.add("predecessors");
  }

  TaskLabelsRendererImpl(ChartModelImpl model, Canvas canvas) {
    myCanvas = canvas;
    myLabelFormatter = new TaskProperties(model.getTimeUnitStack());
    DefaultEnumerationOption<String> deo0 = new DefaultEnumerationOption<String>("taskLabelUp", ourInfoList);
    DefaultEnumerationOption<String> deo1 = new DefaultEnumerationOption<String>("taskLabelDown", ourInfoList);
    DefaultEnumerationOption<String> deo2 = new DefaultEnumerationOption<String>("taskLabelLeft", ourInfoList);
    DefaultEnumerationOption<String> deo3 = new DefaultEnumerationOption<String>("taskLabelRight", ourInfoList);

    myLabelOptions = new EnumerationOption[] { deo0, deo1, deo2, deo3 };
    myOptionGroup = new ChartOptionGroup("ganttChartDetails", myLabelOptions, model.getOptionEventDispatcher());
    // model.getTaskManager().getCustomColumnStorage().addCustomColumnsListener(this);
    myFont = model.getChartUIConfiguration().getChartFont();
  }

  private void addOption(String name) {
    ourInfoList.add(name);
  }

  private void removeOption(String name) {
    ourInfoList.remove(name);
  }

  GPOptionGroup getOptionGroup() {
    return myOptionGroup;
  }

  void createRightSideText(Rectangle rectangle) {
    TaskActivity activity = (TaskActivity) rectangle.getModelObject();
    String text = "";
    int xText, yText;

    text = getTaskLabel(activity.getTask(), RIGHT);

    if (text.length() != 0) {
      xText = rectangle.getRightX() + 9;
      yText = rectangle.getMiddleY();
      Text textPrimitive = processText(xText, yText, text);
      textPrimitive.setAlignment(HAlignment.LEFT, VAlignment.CENTER);
    }
  }

  void createDownSideText(Rectangle rectangle) {
    TaskActivity activity = (TaskActivity) rectangle.getModelObject();
    String text = getTaskLabel(activity.getTask(), DOWN);

    if (text.length() > 0) {
      int xOrigin = rectangle.getRightX();
      int yOrigin = rectangle.getBottomY() + 2;
      Text textPrimitive = processText(xOrigin, yOrigin, text);
      textPrimitive.setAlignment(HAlignment.RIGHT, VAlignment.TOP);
    }
  }

  void createUpSideText(Rectangle rectangle) {
    TaskActivity activity = (TaskActivity) rectangle.getModelObject();
    String text = getTaskLabel(activity.getTask(), UP);
    if (text.length() > 0) {
      int xOrigin = rectangle.getRightX();
      int yOrigin = rectangle.myTopY - 3;
      Text textPrimitive = processText(xOrigin, yOrigin, text);
      textPrimitive.setAlignment(HAlignment.RIGHT, VAlignment.BOTTOM);
    }
  }

  void createLeftSideText(Rectangle rectangle) {
    TaskActivity activity = (TaskActivity) rectangle.getModelObject();
    String text = getTaskLabel(activity.getTask(), LEFT);

    if (text.length() > 0) {
      int xOrigin = rectangle.myLeftX - 9;
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


  private String getTaskLabel(Task task, int position) {
    StringBuffer result = new StringBuffer();
    Object property = myLabelFormatter.getProperty(task, myLabelOptions[position].getValue());
    if (property != null) {
      if (property instanceof Boolean)
        if (((Boolean) property).booleanValue())
          result.append(getLanguage().getText("yes"));
        else
          result.append(getLanguage().getText("no"));
      else
        result.append(property);
    }
    return result.toString();
  }

  private Canvas getPrimitiveContainer() {
    return myCanvas;
  }

  private GanttLanguage getLanguage() {
    return GanttLanguage.getInstance();
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

  static final int TINY_SPACE = 1;
  static final int MEDIUM_SPACE = 2;
  static final int LARGE_SPACE = 4;

  int calculateRowHeight() {
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
    // int topy = nextBounds.y;
    // topy = topy + (getRowHeight() - 20) / 2;
    // if (myLabelsRenderer.isOnlyDown())
    // topy = topy - 6;
    // else if (myLabelsRenderer.isOnlyUp())
    // topy = topy + 6;
    // if (myModel.isPrevious())
    // topy = topy - 5;
  }

  int getFontHeight() {
    return myFont.getSize();
  }
}