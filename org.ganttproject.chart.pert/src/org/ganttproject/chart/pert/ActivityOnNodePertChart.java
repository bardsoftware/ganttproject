/***************************************************************************
ActivityOnNodePertChart.java - description
Copyright [2005 - ADAE]
This file is part of GanttProject].
***************************************************************************/

/***************************************************************************
 * GanttProject is free software; you can redistribute it and/or modify    *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation; either version 2 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * GanttProject is distributed in the hope that it will be useful,         *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *

***************************************************************************/

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
import java.util.Iterator;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.time.TimeUnit;

import org.ganttproject.chart.pert.PertChartAbstraction.TaskGraphNode;

/**
 * PERT char implementation where nodes are tasks and links succession
 * relations.
 * 
 * @author bbaranne
 * @author Julien Seiler
 * 
 */
public class ActivityOnNodePertChart extends PertChart {

    /**
     * List of abstract nodes.
     */
    private List myTaskGraphNodes;

    /**
     * List of graphical arrows.
     */
    private List myGraphicalArrows;

    /**
     * List of graphical nodes (in relation with abstract nodes)
     */
    private List myGraphicalNodes;

    //private Map myMapPositionListOfNodes;
    private int nbCols;

    /**
     * PERT chart abstraction used to build graph.
     */
    private PertChartAbstraction myPertAbstraction;

    /**
     * Max and min coordinates in the graphics that paints the graphical nodes
     * and arrows.
     */
    private int myMaxX = 1, myMaxY = 1;

    /**
     * The currently mouse pressed graphical node.
     */
    private GraphicalNode myPressedGraphicalNode;

    // private List pressedGraphicalNodes;
    //
    // private List xClickedOffsets;
    //
    // private List yClickedOffsets;

    /**
     * Offset between the mouse pointer when clicked on a graphical node and the
     * top left corner of this same node.
     */
    int myXClickedOffset, myYClickedOffset;

    /**
     * Graphics where PERT chart is painted.
     */
    private Graphics myGraphics;

    private static GanttLanguage ourLanguage = GanttLanguage.getInstance();

    /**
     * Constructs a ActivityOnNodePertChart.
     * 
     */
    public ActivityOnNodePertChart() {
        super(null);
        // pressedGraphicalNodes = new ArrayList();
        // xClickedOffsets = new ArrayList();
        // yClickedOffsets = new ArrayList();
        this.setBackground(Color.WHITE.brighter());

        this.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(final MouseEvent e) {
                if (myPressedGraphicalNode != null) {
                    myPressedGraphicalNode.x = e.getX() - myXClickedOffset;
                    myPressedGraphicalNode.y = e.getY() - myYClickedOffset;
                    if (e.getX() > getPreferredSize().getWidth()) {
                        ActivityOnNodePertChart.this
                                .setPreferredSize(new Dimension(
                                        myPressedGraphicalNode.x + NODE_WIDTH
                                                + X_GAP,
                                        (int) getPreferredSize().getHeight()));
                        revalidate();
                    }
                    if (e.getY() > getPreferredSize().getHeight()) {
                        ActivityOnNodePertChart.this
                                .setPreferredSize(new Dimension(
                                        (int) getPreferredSize().getWidth(),
                                        myPressedGraphicalNode.y + NODE_HEIGHT
                                                + Y_GAP));
                        revalidate();
                    }
                    repaint();
                }
            }

            public void mouseMoved(MouseEvent e) {
                // nothing to do...
            }
        });

        this.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent arg0) {
                // nothing to do...
            }

            public void mouseEntered(MouseEvent arg0) {
                // nothing to do...
            }

            public void mouseExited(MouseEvent arg0) {
                // nothing to do...
            }

            public void mousePressed(MouseEvent e) {
                myPressedGraphicalNode = getGraphicalNode(e.getX(), e.getY());
                if (myPressedGraphicalNode != null) {

                    myXClickedOffset = e.getX() - myPressedGraphicalNode.x;
                    myYClickedOffset = e.getY() - myPressedGraphicalNode.y;

                    myPressedGraphicalNode.backgroundColor = myPressedGraphicalNode.backgroundColor
                            .darker();
                }
                repaint();
            }

            public void mouseReleased(MouseEvent e) {
                if (myPressedGraphicalNode != null) {
                    if (myPressedGraphicalNode.node.isCritical())
                        myPressedGraphicalNode.backgroundColor = GraphicalNode.defaultCriticalColor;
                    else
                        myPressedGraphicalNode.backgroundColor = GraphicalNode.defaultBackgroundColor;
                    myPressedGraphicalNode.x = getGridX(e.getX()
                            - myXClickedOffset + NODE_WIDTH / 2);
                    myPressedGraphicalNode.y = getGridY(e.getY());
                    myPressedGraphicalNode = null;
                    repaint();
                }
                recalculatPreferredSize();
                revalidate();
                repaint();
            }
        });
    }

    /**
     * Recalculated preferred size so that graphics fit with nodes positions.
     * 
     */
    private void recalculatPreferredSize()
    {
        int maxX = 0;
        int maxY = 0;

        Iterator it = myGraphicalNodes.iterator();
        while (it.hasNext()) {
            GraphicalNode gn = (GraphicalNode) it.next();
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
     * Returns <code>true</code> if the point of coordinates <code>x</code>,
     * <code>y</code> is in the rectangle described by is top left corner (<code>rectX</code>,
     * <code>rectY</code>) and dimension (<code>rectWidth</code>,
     * <code>rectHeight</code>), <code>false</code> ortherwise.
     * 
     * @param x
     * @param y
     * @param rectX
     * @param rectY
     * @param rectWidth
     * @param rectHeight
     * @return <code>true</code> if the point of coordinates <code>x</code>,
     *         <code>y</code> is in the rectangle described by is top left
     *         corner (<code>rectX</code>, <code>rectY</code>) and
     *         dimension (<code>rectWidth</code>, <code>rectHeight</code>),
     *         <code>false</code> ortherwise.
     */
    private static boolean isInRectancle(int x, int y, int rectX, int rectY,
            int rectWidth, int rectHeight) {
        return (x > rectX && x < rectX + rectWidth && y > rectY && y < rectY
                + rectHeight);
    }

    /**
     * Returns the GraphicalNode at the <code>x</code>, <code>y</code>
     * position, or <code>null</code> if there is no node.
     * 
     * @param x
     * @param y
     * @return The GraphicalNode at the <code>x</code>, <code>y</code>
     *         position, or <code>null</code> if there is no node.
     */
    private GraphicalNode getGraphicalNode(int x, int y) {
        Iterator it = myGraphicalNodes.iterator();
        while (it.hasNext()) {
            GraphicalNode gn = (GraphicalNode) it.next();
            if (isInRectancle(x, y, gn.x, gn.y, NODE_WIDTH, NODE_HEIGHT))
                return gn;
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    protected void buildPertChart() {
        if (this.myPertAbstraction == null)
        {
            this.myPertAbstraction = new PertChartAbstraction(myTaskManager);
            this.myTaskGraphNodes = myPertAbstraction.getTaskGraphNodes();
            this.myGraphicalNodes = new ArrayList();
            this.myGraphicalArrows = new ArrayList();
            //myMapPositionListOfNodes = new HashMap();
            //this.rowsList = new HashMap();
            this.nbCols = 0;
            this.setBackground(Color.WHITE);
            this.process();
            //System.out.println("Position correction");
            //this.correctPositionBecauseOfSuperTasks();
            this.avoidCrossingNode();
            this.avoidCrossingLine();
            this.removeEmptyColumn();
            this.calculateGraphicalNodesCoordinates();
            this.calculateArrowsCoordinates();
            this.setPreferredSize(new Dimension(myMaxX, myMaxY));
        }
        else
        {
            this.myPertAbstraction = new PertChartAbstraction(myTaskManager);
            myTaskGraphNodes = myPertAbstraction.getTaskGraphNodes();
            updateGraphNodesInfo();
        }
    }

    /**
     * Updates the data for each nodes.
     * 
     */
    private void updateGraphNodesInfo() {
        if (myTaskGraphNodes != null) {
            Iterator it = myTaskGraphNodes.iterator();
            while (it.hasNext()) {
                TaskGraphNode tgn = (TaskGraphNode) it.next();
                int id = tgn.getID();
                if(getGraphicalNodeByID(id) != null)
                    getGraphicalNodeByID(id).updateData(tgn);
            }
        }
    }

    /**
     * 
     * @param taskGraphNode
     * @return
     */
    private boolean isZeroPosition(TaskGraphNode taskGraphNode) {
        Iterator it = myTaskGraphNodes.iterator();
        while (it.hasNext())
        {
            TaskGraphNode t = (TaskGraphNode) it.next();
            if (t.getSuccessors().contains(taskGraphNode))
                return false;
        }
        return true;
    }

    private static int getGridX(int x) {
        int res = X_OFFSET;
        int tmp = 0;
        while (res < x)
        {
            tmp = res;
            res += NODE_WIDTH + X_GAP;
        }
        return tmp;
    }

    private static int getGridY(int y) {
        int res = Y_OFFSET;
        int tmp = 0;
        while (res < y)
        {
            tmp = res;
            res += NODE_HEIGHT + Y_GAP;
        }
        return tmp;
    }

    private void process() {
        Iterator it = myTaskGraphNodes.iterator();
        while (it.hasNext())
        {
            TaskGraphNode tgn = (TaskGraphNode) it.next();
            if (isZeroPosition(tgn))
            {
                add(0, new GraphicalNode(tgn));
            }
        }
        int col = 0;
        List l = getNodesThatAreInASpecificSuccessorPosition(col);
        // ici tous les 0 position sont faits.
        while (l != null)
        {
            Iterator it2 = l.iterator();
            while (it2.hasNext())
            {
                TaskGraphNode tnode = (TaskGraphNode) it2.next();
                GraphicalNode gnode = this.getGraphicalNodeByID(tnode.getID());
                if(gnode == null)
                {
                    gnode = this.createGraphicalNode(tnode);
                }
                else
                {
                    this.remove(gnode);
                }
                
                this.add(col + 1, gnode);
            }
            col++;
            l = getNodesThatAreInASpecificSuccessorPosition(col);
        }
    }

    /**
     * Creates or gets the graphical node corresponding to the taskGrahNode
     * 
     * @param taskGraphNode
     * @return
     */
    private GraphicalNode createGraphicalNode(TaskGraphNode taskGraphNode) {
        GraphicalNode res = getGraphicalNodeByID(taskGraphNode.getID());
        if (res != null)
            return res;
        else
            return new GraphicalNode(taskGraphNode);
    }

    private void correctPositionBecauseOfSuperTasks() {
        TaskContainmentHierarchyFacade hierarchy = myTaskManager
                .getTaskHierarchy();
        Task[] tasks = myTaskManager.getTasks();
        for (int i = 0; i < tasks.length; i++) {
            Task task = tasks[i];
            Task[] nestedTasks = hierarchy.getNestedTasks(task);
            correctPositions(nestedTasks);
        }
    }

    // recursive
    private void correctPositions(Task[] tasks) {
        if (tasks == null)
            return;
        TaskContainmentHierarchyFacade hierarchy = myTaskManager
                .getTaskHierarchy();
        for (int j = 0; j < tasks.length; j++) {
            Task nestedTask = tasks[j];
            GraphicalNode gn = getGraphicalNodeByID(nestedTask.getTaskID());
            int oldPosition = gn.col;
            changePosition(gn, oldPosition + 1);
            correctPositions(hierarchy.getNestedTasks(nestedTask));
        }
    }

    private void changePosition(GraphicalNode graphicalNode, int newCol) {
        //int oldPosition = graphicalNode.col;
        //graphicalNode.col = newCol;
        //boolean remove = ((List) myMapPositionListOfNodes.get(new Integer(
        //        oldPosition))).remove(graphicalNode);
        this.remove(graphicalNode);
        this.add(newCol, graphicalNode);
    }
    
    private void moveDown(GraphicalNode graphicalNode)
    {
        int row = graphicalNode.row;
        while(this.isOccupied(++row, graphicalNode.col));
        graphicalNode.row = row;
        /*int nbRows = ((Integer)this.rowsList.get(new Integer(graphicalNode.col))).intValue();
        if(nbRows-1<row)
        {
            this.rowsList.put(new Integer(graphicalNode.col), new Integer(row+1));
        }*/
    }
    
    private GraphicalNode getNode(int row, int col)
    {
        Iterator inCol = this.getNodeInColumn(col).iterator();
        while(inCol.hasNext())
        {
            GraphicalNode node = (GraphicalNode)inCol.next();
            if (node.row ==row)
                return node;
        }
        return null;
    }
    
    private void moveRight(GraphicalNode graphicalNode)
    {
        Iterator successors = graphicalNode.node.getSuccessors().iterator();
        while(successors.hasNext())
        {
            TaskGraphNode successor = (TaskGraphNode)successors.next();
            this.moveRight(this.getGraphicalNodeByID(successor.getID()));
        }
        int newCol = graphicalNode.col +1;
        if(this.isOccupied(graphicalNode.row, newCol))
            this.moveRight(this.getNode(graphicalNode.row, newCol));
        graphicalNode.col = newCol;
        if(newCol==this.nbCols)
        {
            this.nbCols++;
        }
    }
    
    private void remove(GraphicalNode graphicalNode)
    {
        this.myGraphicalNodes.remove(graphicalNode);
        
        if(graphicalNode.col == -1)
            return;
        
        Iterator gnodes = this.getNodeInColumn(graphicalNode.col).iterator();
        while(gnodes.hasNext())
        {
            GraphicalNode gnode = (GraphicalNode)gnodes.next();
            if(gnode.row>graphicalNode.row)
                gnode.row--;
        }
        
        //int iNbRow = ((Integer)this.rowsList.get(new Integer(graphicalNode.col))).intValue();
        //rowsList.put(new Integer(graphicalNode.col), new Integer(iNbRow-1));
        
        if(graphicalNode.col==this.nbCols-1)
        {
            List list = this.getNodeInColumn(this.nbCols-1);
            while(list.size()==0)
            {
                this.nbCols--;
                list = this.getNodeInColumn(this.nbCols-1);
            }
        }
        
        graphicalNode.row = -1;
        graphicalNode.col = -1;
    }

    private GraphicalNode getGraphicalNodeByID(int id) {
        GraphicalNode res = null;
        Iterator it = myGraphicalNodes.iterator();
        while (it.hasNext()) {
            GraphicalNode gn = (GraphicalNode) it.next();
            if (gn.node.getID() == id) {
                res = gn;
                break;
            }
        }
        return res;
    }

    // ajoute la graphical node dans la map position/liste des successeurs
    private void add(int col, GraphicalNode graphicalNode) {
        /*Integer key = new Integer(position);
        List l = (List) myMapPositionListOfNodes.get(key);

        int oldPosition = graphicalNode.col;
        List lOld = (List) myMapPositionListOfNodes
                .get(new Integer(oldPosition));
        if (lOld != null)
            lOld.remove(graphicalNode);

        if (l == null)
        {
            List l2 = new ArrayList();
            l2.add(graphicalNode);
            myMapPositionListOfNodes.put(key, l2);
            
        }
        else
        {
            l.add(graphicalNode);
        }
        myGraphicalNodes.remove(graphicalNode);
        myGraphicalNodes.add(graphicalNode);
        graphicalNode.col = position;*/
        
        myGraphicalNodes.remove(graphicalNode);
        
        if (this.nbCols-1 < col)
            this.nbCols = col+1;
        
        int row = 0;
        while(this.isOccupied(row, col))
            row++;
        
        graphicalNode.row = row;
        graphicalNode.col = col;
        
        myGraphicalNodes.add(graphicalNode);
        
        //rowsList.put(new Integer(col), new Integer(iNbRow+1));
    }

    private List getNodesThatAreInASpecificSuccessorPosition(int col) {
        /*
        List graphicaleNodes = (List) myMapPositionListOfNodes.get(new Integer(
                position));
        */
        List graphicaleNodes = getNodeInColumn(col);
        
        if (graphicaleNodes.size()==0)
            return null;
        
        List res = new ArrayList();
        for (int i = 0; i < graphicaleNodes.size(); i++) {
            GraphicalNode gn = (GraphicalNode) graphicaleNodes.get(i);
            TaskGraphNode tgn = gn.node;
            res.addAll(tgn.getSuccessors());
        }
        
        return res;
    }
    
    /**
     * Get the list of GraphicalNode that are in a column.
     * 
     * @param col	the column number to look in 
     * @return		the list of GraphicalNode in the colum col
     */
    private List getNodeInColumn(int col)
    {
        List list = new ArrayList();
        
        Iterator gnodes = this.myGraphicalNodes.iterator();
        while(gnodes.hasNext())
        {
            GraphicalNode gnode = (GraphicalNode) gnodes.next();
            if(gnode.col == col)
                list.add(gnode);
        }
        
        return list;
    }
    
    private boolean isOccupied(int row, int col)
    {
        List list = this.getNodeInColumn(col);
        if(list.size()!=0)
        {
            Iterator gnodes = list.iterator();
            while(gnodes.hasNext())
            {
                GraphicalNode gnode = (GraphicalNode)gnodes.next();
                if(gnode.row==row)
                    return true;
            }
        }
        return false;
    }
    
    private List getAncestor(TaskGraphNode tgn)
    {
        List ancestors = new ArrayList();
        Iterator tnodes = this.myTaskGraphNodes.iterator();
        while(tnodes.hasNext())
        {
            TaskGraphNode tnode = (TaskGraphNode)tnodes.next();
            List successor = tnode.getSuccessors();
            if(successor.contains(tgn))
                ancestors.add(tnode);
        }
        
        return ancestors;
    }
    
    private boolean isCrossingNode(GraphicalNode gnode)
    {
        TaskGraphNode tgn = gnode.node;
        List list = this.getAncestor(tgn);
        if(list.size()>0)
        {
            Iterator ancestors = list.iterator();
            while(ancestors.hasNext())
            {
                TaskGraphNode ancestor = (TaskGraphNode)ancestors.next();
                GraphicalNode gancestor = this.getGraphicalNodeByID(ancestor.getID());
                if(gancestor.col<gnode.col-1)
                {
                    for(int col=gnode.col-1; col>gancestor.col; col--)
                    {
                        if(this.isOccupied(gnode.row, col))
                            return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void avoidCrossingNode()
    {
        if(this.nbCols==0)
            return;
        
        int col = this.nbCols-1;
        while(col>0)
        {
            boolean hasmoved = false;
            Iterator gnodes = this.getNodeInColumn(col).iterator();
            while(gnodes.hasNext())
            {
                GraphicalNode gnode = (GraphicalNode)gnodes.next();
                while(this.isCrossingNode(gnode))
                {
                    this.moveDown(gnode);
                    hasmoved = true;
                }
            }
            if(hasmoved && col<this.nbCols-1)
                col++;
            else
                col--;
        }
    }
    
    private boolean isCrossingArrow(GraphicalNode gnode)
    {
        //recherche de la position du successeur le plus haut et le plus bas
        int maxUp = 1000000, maxDown = -1;
        Iterator successors = gnode.node.getSuccessors().iterator();
        while(successors.hasNext())
        {
            GraphicalNode successor = this.getGraphicalNodeByID(((TaskGraphNode)successors.next()).getID());
            if(successor.row<maxUp)
                maxUp = successor.row;
            if(successor.row>maxDown)
                maxDown = successor.row;
        }
        
        //r�cup�ration de toutes les nodes sur la m�me colonne
        List othernodes = this.getNodeInColumn(gnode.col);
        othernodes.remove(gnode);
        
        //parcours des nodes sur la m�me colonne
        Iterator nodes = othernodes.iterator();
        while(nodes.hasNext())
        {
            GraphicalNode othergnode = (GraphicalNode)nodes.next();
            Iterator othersuccessors = othergnode.node.getSuccessors().iterator();
            while(othersuccessors.hasNext())
            {
                TaskGraphNode othersuccessor = (TaskGraphNode)othersuccessors.next();
                GraphicalNode othersuccessornode = this.getGraphicalNodeByID(othersuccessor.getID());
                if(maxUp < gnode.row)
                {
                    //some arrows are going up
                    if(othersuccessornode.row <= gnode.row && !gnode.node.getSuccessors().contains(othersuccessor))
                        return true;
                }
                if(maxDown > gnode.row)
                {
                    //some arrow are going down
                    if(othersuccessornode.row >= gnode.row && !gnode.node.getSuccessors().contains(othersuccessor))
                        return true;
                }
            }
        }
        
        return false;
    }
    
    private void avoidCrossingLine()
    {
        boolean restart = true;
        while(restart)
        {
            restart = false;
            for(int col=0; col<this.nbCols; col++)
            {
                List list = this.getNodeInColumn(col);
                if(list.size()>1)
                {
                    Iterator gnodes = list.iterator();
                    while(gnodes.hasNext())
                    {
                        GraphicalNode gnode = (GraphicalNode)gnodes.next();
                        if(this.isCrossingArrow(gnode))
                        {
                            this.moveRight(gnode);
                            this.avoidCrossingNode();
                            restart = true;
                            break;
                        }
                    }
                    
                    if(restart)
                        break;
                }
            }
        }
    }
    
    private void removeEmptyColumn()
    {
        for(int col=this.nbCols-1; col>=0; col--)
        {
            if(this.getNodeInColumn(col).size()==0)
            {
                if(col!=this.nbCols-1)
                {	
                    for(int c=col+1;c<this.nbCols;c++)
                    {
                        Iterator gnodes = this.getNodeInColumn(c).iterator();
                        while(gnodes.hasNext())
                        {
                            GraphicalNode gnode = (GraphicalNode)gnodes.next();
                            gnode.col--;
                        }
                    }
                }
                this.nbCols--;
            }
        }
    }

    public RenderedImage getRenderedImage(GanttExportSettings settings) {
        return getChart(settings);
    }
    public BufferedImage getChart(GanttExportSettings settings) {
        BufferedImage image = new BufferedImage(myMaxX, myMaxY,
                BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.fillRect(0, 0, myMaxX, myMaxY);
        paint(g);
        return image;
    }

    public String getName() {
        return ourLanguage.getText("pertChartLongName");
    }

    public void reset() {
        this.myPertAbstraction = null;
    }

    public void paint(Graphics g) {
        this.buildPertChart();
        myGraphics = g;
        super.paint(g);
        for (int i = 0; i < myGraphicalNodes.size(); i++) {
            ((GraphicalNode) myGraphicalNodes.get(i)).paint(g);
        }
        for (int i = 0; i < myGraphicalArrows.size(); i++) {
            ((GraphicalArrow) myGraphicalArrows.get(i)).paintMe(g);
        }
    }

    private void calculateGraphicalNodesCoordinates()
    {
        /*int nb = 0;
        int col = 0;
        int currentX = 0, currentY = 0;
        //List graphicalNodesByPosition = (List) myMapPositionListOfNodes
        //        .get(new Integer(position));
        while (col<this.nbCols)
        {
            // parcours de toutes les
            // positions
            List graphicalNodesByCol = this.getNodeInColumn(col);
            
            currentY = 0;
            Iterator it = graphicalNodesByCol.iterator();
            while (it.hasNext())
            {
                GraphicalNode gn = (GraphicalNode) it.next();
                gn.x += currentX;
                gn.y += gn.row*(NODE_HEIGHT + Y_GAP);//currentY;
                //currentY += NODE_HEIGHT + Y_GAP;
                myMaxY = gn.y > myMaxY ? gn.y : myMaxY;
            }
            //myMaxY = currentY > myMaxY ? currentY : myMaxY;
            currentX += NODE_WIDTH + X_GAP;
            //graphicalNodesByC = (List) myMapPositionListOfNodes
            //        .get(new Integer(++col));
            col++;
        }
        myMaxX = currentX;
        */
        this.myMaxX = 0;
        this.myMaxY = 0;
        Iterator gnodes = this.myGraphicalNodes.iterator();
        while(gnodes.hasNext())
        {
            GraphicalNode gnode = (GraphicalNode)gnodes.next();
            gnode.x += (NODE_WIDTH + X_GAP) * gnode.col;
            gnode.y += (NODE_HEIGHT + Y_GAP) * gnode.row;
            
            this.myMaxX = gnode.x > this.myMaxX ? gnode.x : this.myMaxX;
            this.myMaxY = gnode.y > this.myMaxY ? gnode.y : this.myMaxY;
        }
        
        this.myMaxX += NODE_WIDTH + X_GAP;
        this.myMaxY += NODE_HEIGHT + Y_GAP;
    }

    private void calculateArrowsCoordinates() {
        Iterator it = myGraphicalNodes.iterator();
        while (it.hasNext()) {
            GraphicalNode gn = (GraphicalNode) it.next();
            Iterator itSuccessors = gn.node.getSuccessors().iterator();
            while (itSuccessors.hasNext()) {
                TaskGraphNode tgn = (TaskGraphNode) itSuccessors.next();
                int id = tgn.getID();

                GraphicalArrow arrow = new GraphicalArrow(gn,
                        getGraphicalNodeByID(id));
                myGraphicalArrows.add(arrow);
            }
        }
    }

    public Icon getIcon() {
    	URL iconUrl = getClass().getResource("/icons/pert_16.gif");
        return iconUrl==null ? null : new ImageIcon(iconUrl);
    }

    /**
     * @inheritDoc
     */
    public Object getAdapter(Class adapter) {
        if (adapter.equals(Container.class) || adapter.equals(Chart.class)) {
            return this;
        }
        else {
            return null;
        }
    }

    /**
     * Graphical node that is rendered on graphics.
     * 
     * @author bbaranne
     * 
     */
    private static class GraphicalNode extends JComponent {
        static int xName = 10;

        static int yName = 0;

        private TaskGraphNode node;

        private int col=-1; // conditionne le X
        private int row=-1;

        private final static Color defaultBackgroundColor = new Color(0.9f,
                0.9f, 0.9f);

        private final static Color defaultCriticalColor = new Color(250, 250,
                115).brighter();

        private Color backgroundColor = null;

        int x = X_OFFSET, y = X_OFFSET;

        GraphicalNode(TaskGraphNode node) 
        {
            this.row = -1;
            this.col = -1;
            this.node = node;
            this.backgroundColor = defaultBackgroundColor;
            if (node.isCritical())
                this.backgroundColor = defaultCriticalColor;
        }

        /**
         * Updates the linked abstract node.
         * 
         * @param node
         *            new linked abtract node.
         */
        public void updateData(TaskGraphNode node) {
            this.node = node;
        }

        /**
         * Paints the graphical node.
         * 
         * @param g
         *            Graphics where the graphical node is to be painted.
         */
        public void paint(Graphics g) {
            if (node.isCritical())
                this.backgroundColor = defaultCriticalColor;
            else
                this.backgroundColor = defaultBackgroundColor;
            paintMe(g);
        }

        /**
         * Paints the graphical node.
         * 
         * @param g
         *            Graphics where the graphical node is to be painted.
         */
        private void paintMe(Graphics g) {
            g.setFont(g.getFont().deriveFont(11f).deriveFont(Font.BOLD));
            FontMetrics fontMetrics = g.getFontMetrics(g.getFont());

            int type = this.node.getType();
            Color color = NORMAL_COLOR;
            switch (type) {
            case PertChartAbstraction.Type.NORMAL: {
                color = NORMAL_COLOR;
                break;
            }
            case PertChartAbstraction.Type.SUPER: {
                color = SUPER_COLOR;
                break;
            }
            case PertChartAbstraction.Type.MILESTONE: {
                color = MILESTONE_COLOR;
                break;
            }
            }
            g.setColor(this.backgroundColor);

            g.fillRoundRect(x, y, NODE_WIDTH, NODE_HEIGHT, 16, 16);
            g.setColor(color);
            g.drawRoundRect(x, y, NODE_WIDTH, NODE_HEIGHT, 16, 16);
            g.drawRoundRect(x + 1, y + 1, NODE_WIDTH - 2, NODE_HEIGHT - 2, 14,
                    14);

            g.drawLine(x, y + yName + fontMetrics.getHeight() + Y_OFFSET, x
                    + NODE_WIDTH, y + yName + fontMetrics.getHeight()
                    + Y_OFFSET);

            g.setColor(Color.BLACK);
            String name = node.getName();
            int nameWidth = fontMetrics.stringWidth(name);

            g.drawString(getTruncatedString(name, NODE_WIDTH - xName,
                    fontMetrics), x + xName, y + yName
                    + fontMetrics.getHeight());

            g.setFont(g.getFont().deriveFont(Font.PLAIN));
            fontMetrics = g.getFontMetrics(g.getFont());

            g.setColor(Color.BLACK);
            g.drawString(ourLanguage.getText("start")
                    + ": "
                    + node.getStartDate().toString(), x + xName, (int) (y
                    + yName + 2.3 * fontMetrics.getHeight()));
            g.drawString(ourLanguage.getText("end")
                    + ": "
                    + node.getEndDate().toString(), x + xName,
                    (int) (y + yName + 3.3 * fontMetrics.getHeight()));

            if (node.getDuration() != null)
                g.drawString(ourLanguage.getText("duration")
                        + ": "
                        + node.getDuration().getLength(), x + xName, (int) (y
                        + yName + 4.3 * fontMetrics.getHeight()));

        }

        /**
         * Truncated the <code>str</code> String according to
         * <code>width</code> and <code>fontMetrics</code>. Returns the
         * truncated String.
         * 
         * @param str
         * @param width
         * @param fontMetrics
         * @return Returns the truncated String.
         */
        private static String getTruncatedString(String str, int width,
                FontMetrics fontMetrics) {
            String res = str;
            int strWidth = fontMetrics.stringWidth(str);
            int maxwidth = 0;
            int i;
            if (strWidth > width) {
                for (i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    int cWidth = fontMetrics.charWidth(c);
                    maxwidth += cWidth;
                    if (maxwidth > width)
                        break;
                }
                res = str.substring(0, i - 2);
                res += "...";
            }
            return res;
        }

        public boolean equals(Object o) {
            if (o instanceof GraphicalNode) {
                return node.equals(((GraphicalNode) o).node);
            }
            return false;
        }

        public String toString() {
            return "[" + node.getName() + " (" + col + ") "
                    + node.getSuccessors() + "]";
        }
    }

    /**
     * Graphical arrow that is rendered on graphics.
     * 
     * @author bbaranne
     * 
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

            int[] xS = { arrowToX, arrowToX - ARROW_WIDTH,
                    arrowToX - ARROW_WIDTH };
            int[] yS = { arrowToY, arrowToY - ARROW_HEIGHT / 2,
                    arrowToY + ARROW_HEIGHT / 2 };
            int nb = xS.length;

            g.fillPolygon(xS, yS, nb); // fl�che

            if (arrowFromY != arrowToY) {
                int[] middleLineX = {
                        arrowFromX + X_GAP / 2 - ARROW_CORNER_WIDTH,
                        arrowFromX + X_GAP / 2, arrowFromX + X_GAP / 2,
                        arrowFromX + X_GAP / 2 + ARROW_CORNER_WIDTH };
                int[] middleLineY = {
                        arrowFromY,
                        (arrowFromY < arrowToY ? arrowFromY
                                + ARROW_CORNER_WIDTH : arrowFromY
                                - ARROW_CORNER_WIDTH),
                        (arrowFromY < arrowToY ? arrowToY - ARROW_CORNER_WIDTH
                                : arrowToY + ARROW_CORNER_WIDTH), arrowToY };
                int middleLineNb = middleLineX.length;
                g.drawPolyline(middleLineX, middleLineY, middleLineNb);

                g.drawLine(arrowFromX, arrowFromY, middleLineX[0],
                        middleLineY[0]);
                g.drawLine(arrowFromX + X_GAP / 2 + ARROW_CORNER_WIDTH,
                        arrowToY, arrowToX - ARROW_WIDTH, arrowToY);
            } else {
                g.drawLine(arrowFromX, arrowFromY, arrowToX, arrowToY);
            }

            // g.drawString(from.node.getName(),arrowFromX+5,arrowFromY+15);
            // g.drawString(to.node.getName(),arrowFromX+50,arrowFromY+15);

        }
    }

    /**
     * Graphical nodes width.
     */
    private final static int NODE_WIDTH = 110;//205;

    /**
     * Graphical nodes height.
     */
    private final static int NODE_HEIGHT = 70;

    /**
     * Gap between two TaskGraphNodes with the same X coordinate.
     */
    private final static int X_GAP = 30;//60;

    /**
     * Gap between two TaskGraphNodes with the same Y coordinate.
     */
    private final static int Y_GAP = 15;//30;

    private final static int ARROW_HEIGHT = 10;

    private final static int ARROW_WIDTH = 15;

    private final static int ARROW_CORNER_WIDTH = 6;

    /**
     * X offset for the top left task graph node.
     */
    private final static int X_OFFSET = 5;

    /**
     * Y offset for the top left task graph node.
     */
    private final static int Y_OFFSET = 5;

    /**
     * Color of the border of normal tasks.
     */
    private final static Color NORMAL_COLOR = Color.BLUE.brighter();

    /**
     * Color of the border of supertasks.
     */
    private final static Color SUPER_COLOR = Color.RED;

    /**
     * Color of the border of milestones.
     */
    private final static Color MILESTONE_COLOR = Color.BLACK;

    /**
     * Color of the arrows.
     */
    private final static Color ARROW_COLOR = Color.GRAY;


    public TaskLength calculateLength(int posX) {
        // TODO Auto-generated method stub
        return null;
    }

    public IGanttProject getProject() {
        // TODO Auto-generated method stub
        return null;
    }

    public void paintChart(Graphics g) {
        // TODO Auto-generated method stub
        
    }

    public void resetRenderers() {
        // TODO Auto-generated method stub
        
    }

    public void scrollBy(int days) {
        // TODO Auto-generated method stub
        
    }

    public void setBottomUnit(TimeUnit bottomUnit) {
        // TODO Auto-generated method stub
        
    }

    public void setBottomUnitWidth(int width) {
        // TODO Auto-generated method stub
        
    }

    public void setDimensions(int height, int width) {
        // TODO Auto-generated method stub
        
    }

    public void setStartDate(Date startDate) {
        // TODO Auto-generated method stub
        
    }

    public void setTopUnit(TimeUnit topUnit) {
        // TODO Auto-generated method stub
        
    }

    public ChartModelBase getModel() {
        // TODO Auto-generated method stub
        return null;
    }

}
