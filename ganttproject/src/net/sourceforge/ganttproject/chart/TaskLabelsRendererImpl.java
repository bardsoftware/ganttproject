/*
 * This code is provided under the terms of GPL version 3.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.Mediator;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.HAlignment;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Label;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Text;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.VAlignment;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.CustomPropertyEvent;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskProperties;
import net.sourceforge.ganttproject.util.TextLengthCalculator;

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

  private GraphicPrimitiveContainer myCanvas;

  private ChartOptionGroup myOptionGroup;

  private static List<String> ourInfoList;

  private TaskProperties myLabelFormatter;

  private Font myFont;

  private final ChartModelImpl myModel;

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

  TaskLabelsRendererImpl(ChartModelImpl model, GraphicPrimitiveContainer canvas) {
    myCanvas = canvas;
    myLabelFormatter = new TaskProperties(model.getTimeUnitStack());
    DefaultEnumerationOption<String> deo0 = new DefaultEnumerationOption<String>("taskLabelUp", ourInfoList);
    DefaultEnumerationOption<String> deo1 = new DefaultEnumerationOption<String>("taskLabelDown", ourInfoList);
    DefaultEnumerationOption<String> deo2 = new DefaultEnumerationOption<String>("taskLabelLeft", ourInfoList);
    DefaultEnumerationOption<String> deo3 = new DefaultEnumerationOption<String>("taskLabelRight", ourInfoList);

    Mediator.addChangeValueDispatcher(deo0);
    Mediator.addChangeValueDispatcher(deo1);
    Mediator.addChangeValueDispatcher(deo2);
    Mediator.addChangeValueDispatcher(deo3);

    myLabelOptions = new EnumerationOption[] { deo0, deo1, deo2, deo3 };
    myOptionGroup = new ChartOptionGroup("ganttChartDetails", myLabelOptions, model.getOptionEventDispatcher());
    // model.getTaskManager().getCustomColumnStorage().addCustomColumnsListener(this);
    myFont = model.getChartUIConfiguration().getChartFont();
    myModel = model;
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

  public void customPropertyChange(CustomPropertyEvent event) {
    int type = event.getType();
    if (type == CustomPropertyEvent.EVENT_ADD) {
      addOption(event.getColName());
    } else if (type == CustomPropertyEvent.EVENT_REMOVE) {
      removeOption((event.getColName()));
    }
  }

  private GraphicPrimitiveContainer getPrimitiveContainer() {
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

  protected static final int MAX_TIMELINE_LABEL_WIDTH = 200;

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

  public Text createTimelineLabel(Rectangle rect, final Task task) {
    final Text text = getPrimitiveContainer().createText(rect.getLeftX(), myModel.getChartUIConfiguration().getSpanningHeaderHeight(), "");
    text.setSelector(new TextSelector() {
      @Override
      public Label[] getLabels(TextLengthCalculator textLengthCalculator) {
        int height = textLengthCalculator.getTextHeight(task.getName());
        int fullLength = textLengthCalculator.getTextLength(task.getName());
        Label result;
        if (fullLength <= MAX_TIMELINE_LABEL_WIDTH) {
          result = text.createLabel(task.getName(), fullLength, height);
        } else {
          int idLength = textLengthCalculator.getTextLength(String.valueOf(task.getTaskID()));
          result = text.createLabel("#" + String.valueOf(task.getTaskID()), idLength, height);
        }
        return new Label[] {result};
      }
    });
    text.setStyle("text.timeline.label");
    getPrimitiveContainer().bind(text, task);
    return text;
  }

}