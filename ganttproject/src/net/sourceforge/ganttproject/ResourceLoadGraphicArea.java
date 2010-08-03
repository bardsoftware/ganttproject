/***************************************************************************
 * GanttGraphicArea.java  -  description
 * -------------------
 * begin                : dec 2002
 * copyright            : (C) 2002 by Thomas Alexandre
 * email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package net.sourceforge.ganttproject;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JTable;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import net.sourceforge.ganttproject.calendar.CalendarFactory;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartModelResource;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.RenderedChartImage;
import net.sourceforge.ganttproject.chart.RenderedResourceChartImage;
import net.sourceforge.ganttproject.chart.ResourceChart;
import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.AssignmentNode;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ProjectResource;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.gregorian.GregorianCalendar;

/**
 * Classe for the graphic part of the soft
 */
public class ResourceLoadGraphicArea extends ChartComponentBase implements
        ResourceChart {

    /** This value is connected to the GanttTRee Scrollbar to move up or down */
    private int margY;

    /* Render the ganttproject version */
    private boolean drawVersion = false;

    /* ! The main application */
    private GanttProject appli;

    private ChartModelResource myChartModel;

    /** Constructor */
    public ResourceLoadGraphicArea(GanttProject app, ZoomManager zoomManager) {
        super((IGanttProject) app, (UIFacade) app, zoomManager);
        this.setBackground(Color.WHITE);
        myChartModel = new ChartModelResource(getTaskManager(),
                (HumanResourceManager) app.getHumanResourceManager(),
                getTimeUnitStack(), getUIConfiguration(), (ResourceChart) this);
        getViewState().addStateListener(myChartModel);
        getViewState().setStartDate(CalendarFactory.newCalendar().getTime());
        margY = 0;
        appli = app;

    }

    /** The size of the panel. */
    public Dimension getPreferredSize() {
        return new Dimension(465, 600);
    }


    /** draw the panel */
    public void paintComponent(Graphics g) {
        myChartModel.setBounds(getSize());
        myChartImplementation.paintComponent(g);
    }

    protected int getHeaderHeight() {
        return 0;
    }
    
    protected int getRowHeight() {
    	return 20;
    }

    public void drawGPVersion(Graphics g) {
        g.setColor(Color.black);
        g.setFont(Fonts.GP_VERSION_FONT);
        g.drawString("GanttProject (" + GanttProject.version + ")", 3,
                getHeight() - 8);
    }

    /** Search for a coef on the arraylist */
    public int indexOf(ArrayList listOfParam, String coef) {
        for (int i = 0; i < listOfParam.size(); i++)
            if (coef == listOfParam.get(i).toString())
                return i;
        return -1;
    }

    /** Change the velue connected to the JTree's Scrollbar */
    public void setScrollBar(int v) {
        margY = v;
        getChartModel().getChartUIConfiguration().setYOffSet(v);
    }

//    /** Return the value of the JTree's Scrollbar */
//    public int getScrollBar() {
//        return margY;
//    }

    public BufferedImage getChart(GanttExportSettings settings) {
        RenderedChartImage renderedImage = (RenderedChartImage) getRenderedImage(settings);
        BufferedImage result = renderedImage.getWholeImage();
        repaint();
        return result;
    }
    /** Return an image with the gantt chart */

    public RenderedImage getRenderedImage(GanttExportSettings settings) {
        Date dateStart = settings.getStartDate() == null ? getStartDate()
                : settings.getStartDate();
        Date dateEnd = settings.getEndDate() == null ? getEndDate() : settings
                .getEndDate();
        if (dateStart.after(dateEnd)) {
            Date tmp = (Date) dateStart.clone();
            dateStart = (Date) dateEnd.clone();
            dateEnd = tmp;
        }
        TaskLength printedLength = getTaskManager().createLength(
                getViewState().getBottomTimeUnit(), dateStart, dateEnd);
        int chartWidth = (int) ((printedLength.getLength(getViewState()
                .getBottomTimeUnit()) + 1) * getViewState()
                .getBottomUnitWidth());
        if (chartWidth<getWidth()) {
            chartWidth = getWidth();
        }

        ResourceTreeImageGenerator resourceTreeGenerator = new ResourceTreeImageGenerator(getHumanResourceManager()) {
            protected boolean isAssignmentVisible(ResourceAssignment assignment) {
                AssignmentNode an = appli.getResourcePanel()
                        .getResourceTreeTableModel().getNodeForAssigment(assignment);
                return appli.getResourcePanel().getResourceTreeTable().isVisible(an);
            }
            protected int getRowHeight() {
            	return ResourceLoadGraphicArea.this.getRowHeight();
            }
        };
        BufferedImage resourceTreeImage = resourceTreeGenerator.createImage();
        final int chartHeight = resourceTreeImage.getHeight();
        
        RenderedResourceChartImage renderedImage = new RenderedResourceChartImage(myChartModel, myChartImplementation,  resourceTreeImage, chartWidth, chartHeight);
        return renderedImage;
    }

    private HumanResourceManager getHumanResourceManager() {
        return (HumanResourceManager) appli.getHumanResourceManager();
    }


    public String getName() {
        return GanttLanguage.getInstance().getText("resourcesChart");
    }

    public Date getStartDate() {
        // return this.beg.getTime();
        return getTaskManager().getProjectStart();
    }

    public Date getEndDate() {
        // return this.end.getTime();
        TaskLength projectLength = getTaskManager().getProjectLength();
        GanttCalendar pstart = new GanttCalendar(getTaskManager()
                .getProjectStart());
        pstart.add((int) projectLength.getLength());
        GanttCalendar end = pstart.Clone();
        return end.getTime();
    }

    protected ChartModelBase getChartModel() {
        return myChartModel;
    }

    protected MouseListener getMouseListener() {
        if (myMouseListener == null) {
            myMouseListener = new MouseListenerBase() {
                protected Action[] getPopupMenuActions() {
                    return new Action[] { getOptionsDialogAction()};
                }
                
            };
        }
        return myMouseListener;
    }

    protected MouseMotionListener getMouseMotionListener() {
        if (myMouseMotionListener == null) {
            myMouseMotionListener = new MouseMotionListenerBase();
        }
        return myMouseMotionListener;
    }

    protected AbstractChartImplementation getImplementation() {
        return myChartImplementation;
    }

    public boolean isExpanded(ProjectResource resource) {
        return true;
    }

    private MouseMotionListener myMouseMotionListener;

    private MouseListener myMouseListener;

    private void setupBeforePaint() {
        myChartModel.setHeaderHeight(getHeaderHeight());
        myChartModel.setBottomUnitWidth(getViewState().getBottomUnitWidth());
        myChartModel.setRowHeight(getRowHeight());// myChartModel.setRowHeight(tree.getJTree().getRowHeight());
        myChartModel.setTopTimeUnit(getViewState().getTopTimeUnit());
        myChartModel.setBottomTimeUnit(getViewState().getBottomTimeUnit());        
    }
    private AbstractChartImplementation myChartImplementation = new AbstractChartImplementation() {
        public void paintComponent(Graphics g) {
            synchronized (ChartModelBase.STATIC_MUTEX) {
                super.paintComponent(g);
                ResourceLoadGraphicArea.super.paintComponent(g);
                setupBeforePaint();
                myChartModel.paint(g);

                if (drawVersion) {
                    drawGPVersion(g);
                }
            }
        }
		public ChartSelection getSelection() {
			ChartSelectionImpl result = new ChartSelectionImpl() {
				public boolean isEmpty() {
					return false;
				}
				public void startCopyClipboardTransaction() {
					super.startCopyClipboardTransaction();
					appli.getResourcePanel().copySelection();
				}

				public void startMoveClipboardTransaction() {
					super.startMoveClipboardTransaction();
					appli.getResourcePanel().cutSelection();
				}
			};
			return result;
		}
		public IStatus canPaste(ChartSelection selection) {
			return Status.OK_STATUS;
		}
		public void paste(ChartSelection selection) {
			appli.getResourcePanel().pasteSelection();
		}
                
    };
    
    public void setTaskManager(TaskManager taskManager) {
        // TODO Auto-generated method stub

    }

    public void reset() {
        // TODO Auto-generated method stub

    }

    public Icon getIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    public Chart createCopy() {
        setupBeforePaint();
        return super.createCopy();
    }
    
    
}
