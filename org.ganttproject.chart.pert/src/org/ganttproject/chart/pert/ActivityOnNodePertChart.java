/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 Bernoit Baranne, Julien Seiler, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.ganttproject.chart.pert;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.export.ChartImageVisitor;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.StringUtils;

import org.ganttproject.chart.pert.PertChartAbstraction.TaskGraphNode;

/**
 * PERT chart implementation where nodes are tasks and links succession
 * relations.
 *
 * @author bbaranne
 * @author Julien Seiler
 *
 */
public class ActivityOnNodePertChart extends PertChart {

  /** List of abstract nodes. */
  private List<TaskGraphNode> myTaskGraphNodes;

  /** List of graphical arrows. */
  private List<GraphicalArrow> myGraphicalArrows;

  /** List of graphical nodes (in relation with abstract nodes) */
  private List<GraphicalNode> myGraphicalNodes;

  // private Map myMapPositionListOfNodes;

  /** Number of columns */
  private int nbCols;

  /** PERT chart abstraction used to build graph. */
  private PertChartAbstraction myPertAbstraction;

  /**
   * Max and min coordinates in the graphics that paints the graphical nodes and
   * arrows.
   */
  private int myMaxX = 1, myMaxY = 1;

  /** The currently mouse pressed graphical node. */
  private GraphicalNode myPressedGraphicalNode;

  /**
   * Offset between the mouse pointer when clicked on a graphical node and the
   * top left corner of this same node.
   */
  int myXClickedOffset, myYClickedOffset;

  private final static GanttLanguage language = GanttLanguage.getInstance();

  /** Graphical nodes width. */
  private final static int NODE_WIDTH = 110;// 205;

  /** Graphical nodes height. */
  private final static int NODE_HEIGHT = 70;

  /** Gap between two TaskGraphNodes with the same X coordinate. */
  private final static int X_GAP = 30;// 60;

  /** Gap between two TaskGraphNodes with the same Y coordinate. */
  private final static int Y_GAP = 15;// 30;

  private final static int ARROW_HEIGHT = 10;

  private final static int ARROW_WIDTH = 15;

  private final static int ARROW_CORNER_WIDTH = 6;

  /** X offset for the top left task graph node. */
  private final static int X_OFFSET = 5;

  /** Y offset for the top left task graph node. */
  private final static int Y_OFFSET = 5;

  /** Color of the border of normal tasks. */
  private final static Color NORMAL_COLOR = Color.BLUE.brighter();

  /** Color of the border of supertasks. */
  private final static Color SUPER_COLOR = Color.RED;

  /** Color of the border of milestones. */
  private final static Color MILESTONE_COLOR = Color.BLACK;

  /** Color of the arrows. */
  private final static Color ARROW_COLOR = Color.GRAY;

  private final JScrollPane myScrollPane;

  public ActivityOnNodePertChart() {
    setBackground(Color.WHITE.brighter());

    this.addMouseMotionListener(new MouseMotionListener() {
      @Override
      public void mouseDragged(final MouseEvent e) {
        if (myPressedGraphicalNode != null) {
          myPressedGraphicalNode.x = e.getX() - myXClickedOffset;
          myPressedGraphicalNode.y = e.getY() - myYClickedOffset;
          if (e.getX() > getPreferredSize().getWidth()) {
            ActivityOnNodePertChart.this.setPreferredSize(new Dimension(myPressedGraphicalNode.x + NODE_WIDTH + X_GAP,
                (int) getPreferredSize().getHeight()));
            revalidate();
          }
          if (e.getY() > getPreferredSize().getHeight()) {
            ActivityOnNodePertChart.this.setPreferredSize(new Dimension((int) getPreferredSize().getWidth(),
                myPressedGraphicalNode.y + NODE_HEIGHT + Y_GAP));
            revalidate();
          }
          repaint();
        }
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        // nothing to do...
      }
    });

    this.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(MouseEvent arg0) {
        // nothing to do...
      }

      @Override
      public void mouseEntered(MouseEvent arg0) {
        // nothing to do...
      }

      @Override
      public void mouseExited(MouseEvent arg0) {
        // nothing to do...
      }

      @Override
      public void mousePressed(MouseEvent e) {
        myPressedGraphicalNode = getGraphicalNode(e.getX(), e.getY());
        if (myPressedGraphicalNode != null) {
          myXClickedOffset = e.getX() - myPressedGraphicalNode.x;
          myYClickedOffset = e.getY() - myPressedGraphicalNode.y;

          myPressedGraphicalNode.backgroundColor = myPressedGraphicalNode.backgroundColor.darker();
        }
        repaint();
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (myPressedGraphicalNode != null) {
          if (myPressedGraphicalNode.node.isCritical()) {
            myPressedGraphicalNode.backgroundColor = GraphicalNode.defaultCriticalColor;
          } else {
            myPressedGraphicalNode.backgroundColor = GraphicalNode.defaultBackgroundColor;
          }
          myPressedGraphicalNode.x = getGridX(e.getX() - myXClickedOffset + NODE_WIDTH / 2);
          myPressedGraphicalNode.y = getGridY(e.getY());
          myPressedGraphicalNode = null;
          repaint();
        }
        recalculatPreferredSize();
        revalidate();
        repaint();
      }
    });
    myScrollPane = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  }

  /** Recalculate preferred size so that graphics fit with nodes positions. */
  private void recalculatPreferredSize() {
    int maxX = 0;
    int maxY = 0;

    for (GraphicalNode gn : myGraphicalNodes) {
      int x = gn.x + NODE_WIDTH;
      int y = gn.y + NODE_HEIGHT;
      maxX = Math.max(maxX, x);
      maxY = Math.max(maxY, y);
    }
    setPreferredSize(new Dimension(maxX, maxY));
    myMaxX = maxX;
    myMaxY = maxY;
  }

  /**
   * @return <code>true</code> if the point of coordinates <code>x</code>,
   *         <code>y</code> is in the rectangle described by is top left corner
   *         (<code>rectX</code>, <code>rectY</code>) and dimension (
   *         <code>rectWidth</code>, <code>rectHeight</code>),
   *         <code>false</code> otherwise.
   */
  private static boolean isInRectancle(int x, int y, int rectX, int rectY, int rectWidth, int rectHeight) {
    return (x > rectX && x < rectX + rectWidth && y > rectY && y < rectY + rectHeight);
  }

  /**
   * @return The GraphicalNode at the <code>x</code>, <code>y</code> position,
   *         or <code>null</code> if there is no node.
   */
  private GraphicalNode getGraphicalNode(int x, int y) {
    for (GraphicalNode gn : myGraphicalNodes) {
      if (isInRectancle(x, y, gn.x, gn.y, NODE_WIDTH, NODE_HEIGHT)) {
        return gn;
      }
    }
    return null;
  }

  @Override
  protected void buildPertChart() {
    if (myPertAbstraction == null) {
      myPertAbstraction = new PertChartAbstraction(myTaskManager);
      myTaskGraphNodes = myPertAbstraction.getTaskGraphNodes();
      myGraphicalNodes = new ArrayList<GraphicalNode>();
      myGraphicalArrows = new ArrayList<GraphicalArrow>();
      // myMapPositionListOfNodes = new HashMap();
      // rowsList = new HashMap();
      nbCols = 0;
      setBackground(Color.WHITE);
      this.process();
      // System.out.println("Position correction");
      // correctPositionBecauseOfSuperTasks();
      avoidCrossingNode();
      avoidCrossingLine();
      removeEmptyColumn();
      calculateGraphicalNodesCoordinates();
      calculateArrowsCoordinates();
      setPreferredSize(new Dimension(myMaxX, myMaxY));
    } else {
      myPertAbstraction = new PertChartAbstraction(myTaskManager);
      myTaskGraphNodes = myPertAbstraction.getTaskGraphNodes();
      updateGraphNodesInfo();
    }
  }

  /** Updates the data for each nodes. */
  private void updateGraphNodesInfo() {
    if (myTaskGraphNodes != null) {
      for (TaskGraphNode tgn : myTaskGraphNodes) {
        GraphicalNode graphicalNode = getGraphicalNodeByID(tgn.getID());
        if (graphicalNode != null) {
          graphicalNode.updateData(tgn);
        }
      }
    }
  }

  private boolean isZeroPosition(TaskGraphNode taskGraphNode) {
    for (TaskGraphNode t : myTaskGraphNodes) {
      if (t.getSuccessors().contains(taskGraphNode)) {
        return false;
      }
    }
    return true;
  }

  private static int getGridX(int x) {
    int res = X_OFFSET;
    int tmp = 0;
    while (res < x) {
      tmp = res;
      res += NODE_WIDTH + X_GAP;
    }
    return tmp;
  }

  private static int getGridY(int y) {
    int res = Y_OFFSET;
    int tmp = 0;
    while (res < y) {
      tmp = res;
      res += NODE_HEIGHT + Y_GAP;
    }
    return tmp;
  }

  private void process() {
    for (TaskGraphNode tgn : myTaskGraphNodes) {
      if (isZeroPosition(tgn)) {
        add(0, new GraphicalNode(tgn));
      }
    }

    // TODO Translate:
    // ici tous les 0 position sont faits.
    int col = 0;
    List<TaskGraphNode> l = getNodesThatAreInASpecificSuccessorPosition(col);
    while (l != null) {
      for (TaskGraphNode tnode : l) {
        GraphicalNode gnode = getGraphicalNodeByID(tnode.getID());
        if (gnode == null) {
          gnode = createGraphicalNode(tnode);
        } else {
          remove(gnode);
        }
        add(col + 1, gnode);
      }
      col++;
      l = getNodesThatAreInASpecificSuccessorPosition(col);
    }
  }

  /**
   * Creates or gets the graphical node corresponding to the taskGrahNode
   *
   * @param taskGraphNode
   */
  private GraphicalNode createGraphicalNode(TaskGraphNode taskGraphNode) {
    GraphicalNode res = getGraphicalNodeByID(taskGraphNode.getID());
    if (res != null) {
      return res;
    }
    return new GraphicalNode(taskGraphNode);
  }

  private void moveDown(GraphicalNode graphicalNode) {
    int row = graphicalNode.row;
    while (this.isOccupied(++row, graphicalNode.col))
      ;
    graphicalNode.row = row;
  }

  private GraphicalNode getNode(int row, int col) {
    for (GraphicalNode node : getNodeInColumn(col)) {
      if (node.row == row) {
        return node;
      }
    }
    return null;
  }

  private void moveRight(GraphicalNode graphicalNode) {
    for (TaskGraphNode successor : graphicalNode.node.getSuccessors()) {
      moveRight(getGraphicalNodeByID(successor.getID()));
    }
    int newCol = graphicalNode.col + 1;
    if (isOccupied(graphicalNode.row, newCol)) {
      moveRight(getNode(graphicalNode.row, newCol));
    }
    graphicalNode.col = newCol;
    if (newCol == nbCols) {
      this.nbCols++;
    }
  }

  private void remove(GraphicalNode graphicalNode) {
    myGraphicalNodes.remove(graphicalNode);

    if (graphicalNode.col == -1) {
      return;
    }

    for (GraphicalNode gnode : getNodeInColumn(graphicalNode.col)) {
      if (gnode.row > graphicalNode.row) {
        gnode.row--;
      }
    }

    // int iNbRow = ((Integer)this.rowsList.get(new
    // Integer(graphicalNode.col))).intValue();
    // rowsList.put(new Integer(graphicalNode.col), new Integer(iNbRow-1));

    if (graphicalNode.col == nbCols - 1) {
      List<GraphicalNode> list = getNodeInColumn(this.nbCols - 1);
      while (list.size() == 0) {
        nbCols--;
        list = getNodeInColumn(nbCols - 1);
      }
    }

    graphicalNode.row = -1;
    graphicalNode.col = -1;
  }

  private GraphicalNode getGraphicalNodeByID(int id) {
    for (GraphicalNode gn : myGraphicalNodes) {
      if (gn.node.getID() == id) {
        return gn;
      }
    }
    return null;
  }

  // TODO Translate:
  /** ajoute la graphical node dans la map position/liste des successeurs */
  private void add(int col, GraphicalNode graphicalNode) {
    myGraphicalNodes.remove(graphicalNode);

    if (nbCols - 1 < col) {
      nbCols = col + 1;
    }

    int row = 0;
    while (isOccupied(row, col)) {
      row++;
    }

    graphicalNode.row = row;
    graphicalNode.col = col;

    myGraphicalNodes.add(graphicalNode);

    // rowsList.put(new Integer(col), new Integer(iNbRow+1));
  }

  private List<TaskGraphNode> getNodesThatAreInASpecificSuccessorPosition(int col) {
    List<GraphicalNode> graphicaleNodes = getNodeInColumn(col);
    if (graphicaleNodes.size() == 0) {
      return null;
    }

    List<TaskGraphNode> res = new ArrayList<TaskGraphNode>();
    for (GraphicalNode gn : graphicaleNodes) {
      res.addAll(gn.node.getSuccessors());
    }

    return res;
  }

  /**
   * Get the list of GraphicalNode that are in a column.
   *
   * @param col
   *          the column number to look in
   * @return the list of GraphicalNode in the col
   */
  private List<GraphicalNode> getNodeInColumn(int col) {
    List<GraphicalNode> list = new ArrayList<GraphicalNode>();
    for (GraphicalNode gnode : myGraphicalNodes) {
      if (gnode.col == col) {
        list.add(gnode);
      }
    }
    return list;
  }

  private boolean isOccupied(int row, int col) {
    for (GraphicalNode gnode : getNodeInColumn(col)) {
      if (gnode.row == row) {
        return true;
      }
    }
    return false;
  }

  private List<TaskGraphNode> getAncestor(TaskGraphNode tgn) {
    List<TaskGraphNode> ancestors = new ArrayList<TaskGraphNode>();
    for (TaskGraphNode tnode : myTaskGraphNodes) {
      List<TaskGraphNode> successor = tnode.getSuccessors();
      if (successor.contains(tgn)) {
        ancestors.add(tnode);
      }
    }
    return ancestors;
  }

  private boolean isCrossingNode(GraphicalNode gnode) {
    for (TaskGraphNode ancestor : getAncestor(gnode.node)) {
      GraphicalNode gancestor = getGraphicalNodeByID(ancestor.getID());
      if (gancestor.col < gnode.col - 1) {
        for (int col = gnode.col - 1; col > gancestor.col; col--) {
          if (this.isOccupied(gnode.row, col)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void avoidCrossingNode() {
    if (nbCols == 0) {
      return;
    }

    int col = nbCols - 1;
    while (col > 0) {
      boolean hasmoved = false;
      for (GraphicalNode gnode : getNodeInColumn(col)) {
        while (isCrossingNode(gnode)) {
          moveDown(gnode);
          hasmoved = true;
        }
      }
      if (hasmoved && col < nbCols - 1) {
        col++;
      } else {
        col--;
      }
    }
  }

  private boolean isCrossingArrow(GraphicalNode gnode) {
    // search for the successors with the highest and lowest position
    int maxUp = Integer.MAX_VALUE, maxDown = -1;
    for (TaskGraphNode successorNode : gnode.node.getSuccessors()) {
      GraphicalNode successor = getGraphicalNodeByID(successorNode.getID());
      if (successor.row < maxUp) {
        maxUp = successor.row;
      }
      if (successor.row > maxDown) {
        maxDown = successor.row;
      }
    }

    // Find all other nodes on the same column
    List<GraphicalNode> otherNodes = this.getNodeInColumn(gnode.col);
    otherNodes.remove(gnode);

    // TODO Translate
    // parcours des nodes sur la même colonne
    List<TaskGraphNode> gnodeSuccessors = gnode.node.getSuccessors();
    for (GraphicalNode otherNode : otherNodes) {
      for (TaskGraphNode otherSuccessor : otherNode.node.getSuccessors()) {
        GraphicalNode otherSuccessorNode = getGraphicalNodeByID(otherSuccessor.getID());
        if (maxUp < gnode.row) {
          // some arrows are going up
          if (otherSuccessorNode.row <= gnode.row && !gnodeSuccessors.contains(otherSuccessor)) {
            return true;
          }
        }
        if (maxDown > gnode.row) {
          // some arrow are going down
          if (otherSuccessorNode.row >= gnode.row && !gnodeSuccessors.contains(otherSuccessor)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void avoidCrossingLine() {
    boolean restart;
    do {
      restart = false;
      for (int col = 0; col < nbCols && restart == false; col++) {
        List<GraphicalNode> list = getNodeInColumn(col);
        if (list.size() > 1) {
          for (GraphicalNode gnode : list) {
            if (isCrossingArrow(gnode)) {
              moveRight(gnode);
              avoidCrossingNode();
              // Stop processing and start over
              restart = true;
              break;
            }
          }
        }
      }
    } while (restart);
  }

  private void removeEmptyColumn() {
    for (int col = nbCols - 1; col >= 0; col--) {
      if (this.getNodeInColumn(col).size() == 0) {
        if (col != nbCols - 1) {
          for (int c = col + 1; c < nbCols; c++) {
            for (GraphicalNode gnode : getNodeInColumn(c)) {
              gnode.col--;
            }
          }
        }
        nbCols--;
      }
    }
  }

  @Override
  public void buildImage(GanttExportSettings settings, ChartImageVisitor imageVisitor) {
    // TODO Auto-generated method stub

  }

  @Override
  public RenderedImage getRenderedImage(GanttExportSettings settings) {
    BufferedImage image = new BufferedImage(myMaxX, myMaxY, BufferedImage.TYPE_INT_RGB);
    Graphics g = image.getGraphics();
    g.fillRect(0, 0, myMaxX, myMaxY);
    paint(g);
    return image;
  }

  @Override
  public String getName() {
    return language.getText("pertChartLongName");
  }

  @Override
  public void reset() {
    myPertAbstraction = null;
  }

  @Override
  public void paint(Graphics g) {
    this.buildPertChart();
    super.paint(g);
    for (int i = 0; i < myGraphicalNodes.size(); i++) {
      myGraphicalNodes.get(i).paint(g);
    }
    for (int i = 0; i < myGraphicalArrows.size(); i++) {
      myGraphicalArrows.get(i).paintMe(g);
    }
  }

  private void calculateGraphicalNodesCoordinates() {
    myMaxX = 0;
    myMaxY = 0;
    for (GraphicalNode gnode : myGraphicalNodes) {
      gnode.x += (NODE_WIDTH + X_GAP) * gnode.col;
      gnode.y += (NODE_HEIGHT + Y_GAP) * gnode.row;

      myMaxX = gnode.x > myMaxX ? gnode.x : myMaxX;
      myMaxY = gnode.y > myMaxY ? gnode.y : myMaxY;
    }

    myMaxX += NODE_WIDTH + X_GAP;
    myMaxY += NODE_HEIGHT + Y_GAP;
  }

  private void calculateArrowsCoordinates() {
    for (GraphicalNode gn : myGraphicalNodes) {
      for (TaskGraphNode tgn : gn.node.getSuccessors()) {
        int id = tgn.getID();
        GraphicalArrow arrow = new GraphicalArrow(gn, getGraphicalNodeByID(id));
        myGraphicalArrows.add(arrow);
      }
    }
  }

  public Icon getIcon() {
    URL iconUrl = getClass().getResource("/icons/pert_16.gif");
    return iconUrl == null ? null : new ImageIcon(iconUrl);
  }

  @Override
  public Object getAdapter(Class adapter) {
    if (adapter.equals(Chart.class)) {
      return this;
    }
    if (adapter.equals(Container.class)) {
      return myScrollPane;
    }
    return null;
  }

  /**
   * Graphical node that is rendered on graphics.
   *
   * @author bbaranne
   */
  private static class GraphicalNode extends JComponent {
    static int xName = 10;

    static int yName = 0;

    private TaskGraphNode node;

    private int col = -1; // determines X
    private int row = -1;

    private final static Color defaultBackgroundColor = new Color(0.9f, 0.9f, 0.9f);

    private final static Color defaultCriticalColor = new Color(250, 250, 115).brighter();

    private Color backgroundColor = null;

    int x = X_OFFSET, y = X_OFFSET;

    GraphicalNode(TaskGraphNode node) {
      this.row = -1;
      this.col = -1;
      this.node = node;
      this.backgroundColor = defaultBackgroundColor;
      if (node.isCritical()) {
        this.backgroundColor = defaultCriticalColor;
      }
    }

    /**
     * Updates the linked abstract node.
     *
     * @param node
     *          new linked abstract node.
     */
    public void updateData(TaskGraphNode node) {
      this.node = node;
    }

    /**
     * Paints the graphical node.
     *
     * @param g
     *          Graphics where the graphical node is to be painted.
     */
    @Override
    public void paint(Graphics g) {
      if (node.isCritical()) {
        this.backgroundColor = defaultCriticalColor;
      } else {
        this.backgroundColor = defaultBackgroundColor;
      }
      paintMe(g);
    }

    /**
     * Paints the graphical node.
     *
     * @param g
     *          Graphics where the graphical node is to be painted.
     */
    private void paintMe(Graphics g) {
      g.setFont(g.getFont().deriveFont(11f).deriveFont(Font.BOLD));
      FontMetrics fontMetrics = g.getFontMetrics(g.getFont());

      int type = this.node.getType();
      Color color;
      switch (type) {
      case PertChartAbstraction.Type.NORMAL:
        color = NORMAL_COLOR;
        break;
      case PertChartAbstraction.Type.SUPER:
        color = SUPER_COLOR;
        break;
      case PertChartAbstraction.Type.MILESTONE:
        color = MILESTONE_COLOR;
        break;
      default:
        color = NORMAL_COLOR;
      }
      g.setColor(this.backgroundColor);

      g.fillRoundRect(x, y, NODE_WIDTH, NODE_HEIGHT, 16, 16);
      g.setColor(color);
      g.drawRoundRect(x, y, NODE_WIDTH, NODE_HEIGHT, 16, 16);
      g.drawRoundRect(x + 1, y + 1, NODE_WIDTH - 2, NODE_HEIGHT - 2, 14, 14);

      g.drawLine(x, y + yName + fontMetrics.getHeight() + Y_OFFSET, x + NODE_WIDTH, y + yName + fontMetrics.getHeight()
          + Y_OFFSET);

      g.setColor(Color.BLACK);
      String name = node.getName();

      g.drawString(StringUtils.getTruncatedString(name, NODE_WIDTH - xName, fontMetrics), x + xName, y + yName
          + fontMetrics.getHeight());

      g.setFont(g.getFont().deriveFont(Font.PLAIN));
      fontMetrics = g.getFontMetrics(g.getFont());

      g.setColor(Color.BLACK);
      g.drawString(language.getText("start") + ": " + node.getStartDate().toString(), x + xName,
          (int) (y + yName + 2.3 * fontMetrics.getHeight()));
      g.drawString(language.getText("end") + ": " + node.getEndDate().toString(), x + xName,
          (int) (y + yName + 3.3 * fontMetrics.getHeight()));

      if (node.getDuration() != null)
        g.drawString(language.getText("duration") + ": " + node.getDuration().getLength(), x + xName,
            (int) (y + yName + 4.3 * fontMetrics.getHeight()));
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof GraphicalNode) {
        return node.equals(((GraphicalNode) o).node);
      }
      return false;
    }

    @Override
    public String toString() {
      return "[" + node.getName() + " (" + col + ") " + node.getSuccessors() + "]";
    }
  }

  /**
   * Graphical arrow that is rendered on graphics.
   *
   * @author bbaranne
   */
  private static class GraphicalArrow {
    GraphicalNode from;

    GraphicalNode to;

    GraphicalArrow(GraphicalNode from, GraphicalNode to) {
      this.from = from;
      this.to = to;
    }

    private void paintMe(Graphics g) {
      g.setColor(ARROW_COLOR);

      int arrowFromX, arrowFromY;
      int arrowToX, arrowToY;

      arrowFromX = from.x + NODE_WIDTH;
      arrowFromY = from.y + NODE_HEIGHT / 2;
      arrowToX = to.x;
      arrowToY = to.y + NODE_HEIGHT / 2;

      int[] xS = { arrowToX, arrowToX - ARROW_WIDTH, arrowToX - ARROW_WIDTH };
      int[] yS = { arrowToY, arrowToY - ARROW_HEIGHT / 2, arrowToY + ARROW_HEIGHT / 2 };
      int nb = xS.length;

      g.fillPolygon(xS, yS, nb); // flèche

      if (arrowFromY != arrowToY) {
        int[] middleLineX = { arrowFromX + X_GAP / 2 - ARROW_CORNER_WIDTH, arrowFromX + X_GAP / 2,
            arrowFromX + X_GAP / 2, arrowFromX + X_GAP / 2 + ARROW_CORNER_WIDTH };
        int[] middleLineY = { arrowFromY,
            (arrowFromY < arrowToY ? arrowFromY + ARROW_CORNER_WIDTH : arrowFromY - ARROW_CORNER_WIDTH),
            (arrowFromY < arrowToY ? arrowToY - ARROW_CORNER_WIDTH : arrowToY + ARROW_CORNER_WIDTH), arrowToY };
        int middleLineNb = middleLineX.length;
        g.drawPolyline(middleLineX, middleLineY, middleLineNb);

        g.drawLine(arrowFromX, arrowFromY, middleLineX[0], middleLineY[0]);
        g.drawLine(arrowFromX + X_GAP / 2 + ARROW_CORNER_WIDTH, arrowToY, arrowToX - ARROW_WIDTH, arrowToY);
      } else {
        g.drawLine(arrowFromX, arrowFromY, arrowToX, arrowToY);
      }

      // g.drawString(from.node.getName(),arrowFromX+5,arrowFromY+15);
      // g.drawString(to.node.getName(),arrowFromX+50,arrowFromY+15);
    }
  }

  @Override
  public IGanttProject getProject() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setDimensions(int height, int width) {
    // TODO Auto-generated method stub
  }

  @Override
  public void setStartDate(Date startDate) {
    // TODO Auto-generated method stub
  }
}
