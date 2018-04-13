/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package biz.ganttproject.core.chart.canvas;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;



/**
 * Stores the available primitives and their information (used for painting) and
 * provides methods to retrieve them
 *
 * @author bard
 */
public class Canvas {
  private ArrayList<Rectangle> myRectangles = new ArrayList<Rectangle>();

  private ArrayList<Line> myLines = new ArrayList<Line>();

  private ArrayList<Text> myTexts = new ArrayList<Text>();

  private Map<Object, Shape> myModelObject2primitive = new WeakHashMap<Object, Shape>();

  private List<Canvas> myLayers = new ArrayList<Canvas>();

  private int myDeltaX;

  private int myDeltaY;

  private List<TextGroup> myTextGroups = new ArrayList<TextGroup>();

  private final DummySpatialIndex<Text> myTextIndex = new DummySpatialIndex<Text>();

  private final DummySpatialIndex<Polygon> myPolygonIndex = new DummySpatialIndex<Canvas.Polygon>();

  /** Horizontal alignments for texts */
  public enum HAlignment {
    CENTER, LEFT, RIGHT
  };

  /** Vertical alignments for texts */
  public enum VAlignment {
    CENTER, TOP, BOTTOM
  };

  public static class Shape {
    private Color myBackgroundColor;

    private Color myForegroundColor;

    private String myStyleName;

    private Object myModelObject;

    private boolean isVisible = true;

    private LinkedHashSet<String> myStyles;

    private Float myOpacity = null;

    private LinkedHashSet<String> getStyles() {
      if (myStyles == null) {
        myStyles = new LinkedHashSet<String>();
      }
      return myStyles;
    }

    public void addStyle(String style) {
      getStyles().add(style);
    }

    public boolean hasStyle(String style) {
      return getStyles().contains(style);
    }

    public void setStyle(String styleName) {
      myStyleName = styleName;
    }

    public String getStyle() {
      return myStyleName;
    }

    public Color getBackgroundColor() {
      return myBackgroundColor;
    }

    public void setBackgroundColor(Color myBackgroundColor) {
      this.myBackgroundColor = myBackgroundColor;
    }

    public Color getForegroundColor() {
      return myForegroundColor;
    }

    public void setForegroundColor(Color myForegroundColor) {
      this.myForegroundColor = myForegroundColor;
    }

    public Object getModelObject() {
      return myModelObject;
    }

    public void setModelObject(Object modelObject) {
      myModelObject = modelObject;
    }

    public boolean isVisible() {
      return isVisible;
    }

    public void setVisible(boolean visible) {
      isVisible = visible;
    }

    public Float getOpacity() {
      return myOpacity;
    }

    public void setOpacity(float opacity) {
      myOpacity = opacity;
    }
  }

  public static class Polygon extends Shape {
    private final int myLeftX;
    private final int myRightX;
    private final int myTopY;
    private final int myBottomY;

    private final int[] myPointsX;
    private final int[] myPointsY;

    private Polygon(int... points) {
      myPointsX = new int[points.length / 2];
      myPointsY = new int[points.length / 2];

      int leftX = Integer.MAX_VALUE;
      int topY = Integer.MAX_VALUE;
      int rightX = Integer.MIN_VALUE;
      int bottomY = Integer.MIN_VALUE;
      for (int i = 0; i < points.length / 2; i++) {
        leftX = Math.min(leftX, points[i * 2]);
        rightX = Math.max(rightX, points[i * 2]);
        topY = Math.min(topY, points[i * 2 + 1]);
        bottomY = Math.max(bottomY, points[i * 2 + 1]);

        myPointsX[i] = points[i * 2];
        myPointsY[i] = points[i * 2 + 1];
      }
      myLeftX = leftX;
      myRightX = rightX;
      myTopY = topY;
      myBottomY = bottomY;
    }

    public int[] getPointsX() {
      return myPointsX;
    }

    public int[] getPointsY() {
      return myPointsY;
    }

    public int getPointCount() {
      return myPointsX.length;
    }

    public int getLeftX() {
      return myLeftX;
    }

    public int getTopY() {
      return myTopY;
    }

    public int getRightX() {
      return myRightX;
    }

    public int getBottomY() {
      return myBottomY;
    }

    public int getHeight() {
      return myBottomY - myTopY;
    }

    public int getWidth() {
      return myRightX - myLeftX;
    }

    public int getMiddleY() {
      return myTopY + getHeight() / 2;
    }

    public int getMiddleX() {
      return myLeftX + getWidth() / 2;
    }

    @Override
    public String toString() {
      return "Polygon[" + myLeftX + "," + myTopY + ",w=" + getWidth() + ",h=" + getHeight() + ",style=" + getStyle() + "]";
    }
  }

  public static class Rectangle extends Polygon {
    public Paint myPaint;

    private Rectangle(int leftx, int topy, int width, int height) {
      super(leftx, topy, leftx + width, topy + height);
    }

    public Paint getBackgroundPaint() {
      return myPaint;
    }

    public void setBackgroundPaint(Paint paint) {
      myPaint = paint;
    }
  }

  public static class Arrow extends Shape {
    public static Arrow NONE = new Arrow(0, 0);
    public static Arrow FINISH = new Arrow(7, 3);
    private final int myLength;
    private final int myWidth;

    public Arrow(int length, int width) {
      myLength = length;
      myWidth = width;
    }

    public int getLength() {
      return myLength;
    }

    public int getWidth() {
      return myWidth;
    }
  }

  public static class Line extends Shape {
    private final int myStartX;

    private final int myStartY;

    private final int myFinishX;

    private final int myFinishY;

    private Arrow myArrow = Arrow.NONE;

    private Line(int startx, int starty, int finishx, int finishy) {
      myStartX = startx;
      myStartY = starty;
      myFinishX = finishx;
      myFinishY = finishy;
    }

    public int getStartX() {
      return myStartX;
    }

    public int getStartY() {
      return myStartY;
    }

    public int getFinishX() {
      return myFinishX;
    }

    public int getFinishY() {
      return myFinishY;
    }

    public void setArrow(Arrow arrow) {
      myArrow = arrow;
    }

    public Arrow getArrow() {
      return myArrow;
    }
  }

  public static class Label {
    public final String text;
    public final int lengthPx;
    public final int heightPx;
    private final Text myOwner;

    public Label(Text owner, String text, int lengthPx) {
      this(owner, text, lengthPx, Integer.MIN_VALUE);
    }

    public Label(Text owner, String text, int lengthPx, int heightPx) {
      this.myOwner = owner;
      this.text = text;
      this.lengthPx = lengthPx;
      this.heightPx = heightPx;
    }

    public void setVisible(boolean isVisible) {
      if (isVisible && myOwner != null) {
        myOwner.index(this);
      }
    }
  }

  public static class Text extends Shape {
    private final int myLeftX;

    private final int myBottomY;

    private Font myFont;

    private int myMaxLength;

    private HAlignment myHAlignment = HAlignment.LEFT;

    private VAlignment myVAlignment = VAlignment.BOTTOM;

    private TextSelector mySelector;

    private final SpatialIndex<Text> myIndex;

    private Text(int leftX, int bottomY, final String text, SpatialIndex<Text> index) {
      this(leftX, bottomY, (TextSelector)null, index);
      mySelector = new TextSelector() {
        @Override
        public Label[] getLabels(TextMetrics textLengthCalculator) {
          return new Label[] {createLabel(text, textLengthCalculator.getTextLength(text))};
        }
      };
    }

    void index(Label label) {
      assert label.myOwner == this;
      if (myIndex != null && label.heightPx != Integer.MIN_VALUE) {
        myIndex.put(this, myLeftX, myBottomY, label.lengthPx, label.heightPx);
      }
    }

    private Text(int leftX, int bottomY, TextSelector delegateSelector) {
      this(leftX, bottomY, delegateSelector, null);
    }

    private Text(int leftX, int bottomY, TextSelector delegateSelector, SpatialIndex<Text> index) {
      myLeftX = leftX;
      myBottomY = bottomY;
      mySelector = delegateSelector;
      myMaxLength = -1;
      myIndex = index;
    }

    public void setFont(Font font) {
      myFont = font;
    }

    public void setMaxLength(int maxLength) {
      myMaxLength = maxLength;
    }

    public int getMaxLength() {
      return myMaxLength;
    }

    public Font getFont() {
      return myFont;
    }

    public Label[] getLabels(TextMetrics textLengthCalculator) {
      return mySelector.getLabels(textLengthCalculator);
      // String text = mySelector.getText(textLengthCalculator);
      // int length = textLengthCalculator.getTextLength(text);
      // return new Label[] { new Label(text, length) };
    }

    public int getLeftX() {
      return myLeftX;
    }

    public int getBottomY() {
      return myBottomY;
    }

    public void setAlignment(HAlignment halignment, VAlignment valignment) {
      myHAlignment = halignment;
      myVAlignment = valignment;
    }

    public HAlignment getHAlignment() {
      return myHAlignment;
    }

    public VAlignment getVAlignment() {
      return myVAlignment;
    }

    public TextSelector getTextSelector() {
      return mySelector;
    }

    public Label createLabel(String text, int lengthPx) {
      return createLabel(text, lengthPx, Integer.MIN_VALUE);
    }

    public Label createLabel(String text, int lengthPx, int heightPx) {
      return new Label(this, text, lengthPx, heightPx);
    }

    @Override
    public String toString() {
      return String.format("TBox [%d, %d]", myLeftX, myBottomY);
    }

    public void setSelector(TextSelector selector) {
      mySelector = selector;
    }
  }

  public static class TextGroup {
    private List<String> myLineStyles;
    private int myHeight;
    private FontChooser myFontChooser;
    private List<Font> myFonts;
    private List<List<Text>> myLines = new ArrayList<List<Text>>();
    private int myBottomY;
    private int myLeftX;
    private List<Integer> myBaselines = new ArrayList<Integer>();

    public TextGroup(int leftX, int bottomY, int height, String... lineStyles) {
      myLeftX = leftX;
      myBottomY = bottomY;
      myHeight = height;
      myLineStyles = new ArrayList<String>(Arrays.asList(lineStyles));
      for (int i = 0; i < myLineStyles.size(); i++) {
        myLines.add(new ArrayList<Text>());
      }
      myFonts = new ArrayList<Font>();
    }

    public void setFonts(FontChooser fontChooser) {
      for (int totalHeight = getTotalHeight(fontChooser, myBaselines); totalHeight > myHeight;
          totalHeight = getTotalHeight(fontChooser, myBaselines)) {
        int baseSize = fontChooser.decreaseBaseFontSize();
        if (baseSize < 8) {
          break;
        }
      }
      for (String style : myLineStyles) {
        myFonts.add(fontChooser.getFont(style));
      }
      myFontChooser = fontChooser;
    }

    private int getTotalHeight(FontChooser fontChooser, List<Integer> lineBaselines) {
      lineBaselines.clear();
      int totalHeight = 0;
      for (String style : myLineStyles) {
        totalHeight += fontChooser.getMarginTop(style);
        totalHeight += fontChooser.getTextHeight(style);
        totalHeight += fontChooser.getMarginBottom(style);
        lineBaselines.add(totalHeight);
      }
      return totalHeight;
    }

    public Text addText(int x, int y, TextSelector textSelector) {
      Text result = new Text(x, y, textSelector);
      myLines.get(y).add(result);
      return result;
    }

    public int getLineCount() {
      return myLines.size();
    }

    public List<Text> getLine(int i) {
      return myLines.get(i);
    }

    public Font getFont(int i) {
      return myFonts.get(i);
    }

    public Color getColor(int i) {
      return myFontChooser.getColor(myLineStyles.get(i));
    }

    public int getLeftX() {
      return myLeftX;
    }

    public int getBottomY() {
      return myBottomY;
    }

    public int getBottomY(int line) {
      return myBottomY + myBaselines.get(line);
    }
  }

  public Canvas() {
    this(0, 0);
  }

  public Canvas(int deltax, int deltay) {
    myDeltaX = deltax;
    myDeltaY = deltay;
  }

  public void setOffset(int deltax, int deltay) {
    myDeltaX = deltax;
    myDeltaY = deltay;
    // for (GraphicPrimitiveContainer layer : myLayers) {
    // layer.setOffset(deltax, deltay);
    // }
  }

  public Polygon createPolygon(int... points) {
    assert points.length % 2 == 0 : "The number of points must be even";
    Polygon result = new Polygon(points);
    myPolygonIndex.put(result, result.getLeftX(), result.getBottomY(), result.getWidth(), result.getHeight());
    return result;
  }

  public Rectangle createRectangle(int leftx, int topy, int width, int height) {
    Rectangle result = createDetachedRectangle(leftx, topy, width, height);
    myRectangles.add(result);
    return result;
  }

  public Rectangle createDetachedRectangle(int leftx, int topy, int width, int height) {
    if (width < 0) {
      width = -width;
      leftx = leftx - width;
    }
    return new Rectangle(leftx + myDeltaX, topy + myDeltaY, width, height);
  }

  public Line createLine(int startx, int starty, int finishx, int finishy) {
    Line result = new Line(startx + myDeltaX, starty + myDeltaY, finishx + myDeltaX, finishy + myDeltaY);
    myLines.add(result);
    return result;
  }

  public Text createText(int leftx, int bottomy, String text) {
    Text result = new Text(leftx + myDeltaX, bottomy + myDeltaY, text, myTextIndex);
    myTexts.add(result);
    return result;
  }

  public Text createText(int leftx, int bottomy, TextSelector textSelector) {
    Text result = new Text(leftx + myDeltaX, bottomy + myDeltaY, textSelector, myTextIndex);
    myTexts.add(result);
    return result;
  }

  public TextGroup createTextGroup(int leftX, int bottomY, int height, String... styles) {
    TextGroup result = new TextGroup(leftX, bottomY, height, styles);
    myTextGroups.add(result);
    return result;
  }

  public void paint(Painter painter) {
    painter.prePaint();
    for (int i = 0; i < myRectangles.size(); i++) {
      Rectangle next = myRectangles.get(i);
      if (next.isVisible()) {
        painter.paint(next);
      }
    }
    for (Polygon p : myPolygonIndex.values()) {
      if (p.isVisible()) {
        painter.paint(p);
      }
    }
    for (int i = 0; i < myLines.size(); i++) {
      Line next = myLines.get(i);
      if (next.isVisible()) {
        painter.paint(next);
      }
    }
    for (int i = 0; i < myTexts.size(); i++) {
      Text next = myTexts.get(i);
      if (next.isVisible()) {
        painter.paint(next);
      }
    }
    for (TextGroup textGroup : myTextGroups) {
      painter.paint(textGroup);
    }
  }


  public void clear() {
    myTextIndex.clear();
    myPolygonIndex.clear();
    myRectangles.clear();
    myLines.clear();
    myTexts.clear();
    myTextGroups.clear();
    myModelObject2primitive.clear();
    for (Canvas layer : getLayers()) {
      layer.clear();
    }
  }

  public void bind(Shape primitive, Object modelObject) {
    myModelObject2primitive.put(modelObject, primitive);
    primitive.setModelObject(modelObject);
  }

  public Shape getPrimitive(Object modelObject) {
    return myModelObject2primitive.get(modelObject);
  }

  public Shape getPrimitive(int x, int y) {
    return getPrimitive(x, 0, y, 0);
  }

  public Shape getPrimitive(int x, int xThreshold, int y, int yThreshold) {
    Shape result = null;
    for (int i = 0; i < myRectangles.size(); i++) {
      Rectangle next = myRectangles.get(i);
      // System.err.println(" next rectangle="+next);
      if (next.getLeftX() <= x + xThreshold && next.getRightX() >= x - xThreshold
          && next.getTopY() <= y + yThreshold && next.getBottomY() >= y - yThreshold) {
        result = next;
        break;
      }
    }
    if (result != null) {
      return result;
    }
    result = myPolygonIndex.get(x, xThreshold, y, yThreshold);
    if (result != null) {
      return result;
    }
    return myTextIndex.get(x + myDeltaX, y + myDeltaY);
  }

  public List<Canvas> getLayers() {
    return Collections.unmodifiableList(myLayers);
  }

  public Canvas getLayer(int layer) {
    if (layer < 0 || layer >= myLayers.size()) {
      throw new IllegalArgumentException();
    }
    return myLayers.get(layer);
  }

  public Canvas newLayer() {
    Canvas result = new Canvas();
    myLayers.add(result);
    return result;
  }

  public void createLayers(int count) {
    for (int i = 0; i < count; i++) {
      newLayer();
    }
  }
}
