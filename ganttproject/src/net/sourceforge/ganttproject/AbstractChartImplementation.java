/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartRendererBase;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartSelectionListener;
import net.sourceforge.ganttproject.chart.ChartUIConfiguration;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.chart.export.ChartDimensions;
import net.sourceforge.ganttproject.chart.export.ChartImageBuilder;
import net.sourceforge.ganttproject.chart.export.ChartImageVisitor;
import net.sourceforge.ganttproject.chart.export.RenderedChartImage;
import net.sourceforge.ganttproject.chart.mouse.MouseInteraction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.zoom.ZoomEvent;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.time.TimeUnit;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class AbstractChartImplementation implements TimelineChart, ZoomListener {
    public static final ImageIcon LOGO = new ImageIcon(AbstractChartImplementation.class.getResource("/icons/big.png"));
    private static final int HEADER_OFFSET = LOGO.getIconHeight();

    private final ChartModelBase myChartModel;
    private final IGanttProject myProject;
    private Set<ChartSelectionListener> mySelectionListeners= new LinkedHashSet<ChartSelectionListener>();
    private final ChartComponentBase myChartComponent;
    private MouseInteraction myActiveInteraction;
    private final UIFacade myUiFacade;

    public AbstractChartImplementation(IGanttProject project, UIFacade uiFacade, ChartModelBase chartModel, ChartComponentBase chartComponent) {
        assert chartModel != null;
        myUiFacade = uiFacade;
        myChartModel = chartModel;
        myProject = project;
        myChartComponent = chartComponent;
        uiFacade.getTaskSelectionManager().addSelectionListener(new TaskSelectionManager.Listener() {
            @Override
            public void userInputConsumerChanged(Object newConsumer) {
                fireSelectionChanged();
            }

            @Override
            public void selectionChanged(List<Task> currentSelection) {
                fireSelectionChanged();
            }
        });
    }

    @Override
    public void init(IGanttProject project) {
        // Skip as we already have a project instance.
    }

    protected void setCursor(Cursor cursor) {
        myChartComponent.setCursor(cursor);
    }

    protected UIFacade getUIFacade() {
        return myUiFacade;
    }

    @Override
    public IGanttProject getProject() {
        return myProject;
    }

    public void beginScrollViewInteraction(MouseEvent e) {
        setActiveInteraction(myChartComponent.newScrollViewInteraction(e));
    }

    public MouseInteraction finishInteraction() {
        try {
            if (getActiveInteraction() != null) {
                getActiveInteraction().finish();
            }
            return getActiveInteraction();
        } finally {
            setActiveInteraction(null);
        }
    }

    protected void setActiveInteraction(MouseInteraction myActiveInteraction) {
        this.myActiveInteraction = myActiveInteraction;
    }

    public MouseInteraction getActiveInteraction() {
        return myActiveInteraction;
    }

    @Override
    public void zoomChanged(ZoomEvent e) {
        myChartComponent.invalidate();
        myChartComponent.repaint();
    }

    public void paintChart(Graphics g) {
        getChartModel().paint(g);
    }

    protected ChartModelBase getChartModel() {
        return myChartModel;
    }

    /////////////////////////////////////////////////////////////
    // interface Chart
    @Override
    public void buildImage(GanttExportSettings settings, ChartImageVisitor imageVisitor) {
        ChartModelBase modelCopy = getChartModel().createCopy();
        modelCopy.setBounds(myChartComponent.getSize());
        if (settings.getStartDate() == null) {
            settings.setStartDate(modelCopy.getStartDate());
        }
        if (settings.getEndDate() == null) {
            settings.setEndDate(modelCopy.getEndDate());
        }
        if (settings.isCommandLineMode()) {
            myChartComponent.getTreeTable().getTable().getTableHeader().setVisible(true);
            myChartComponent.getTreeTable().doLayout();
            myChartComponent.getTreeTable().getTable().setRowHeight(modelCopy.calculateRowHeight());
            myChartComponent.getTreeTable().autoFitColumns();
        }
        ChartImageBuilder builder = new ChartImageBuilder(settings, modelCopy, myChartComponent.getTreeTable());
        builder.buildImage(imageVisitor);
    }

    @Override
    public RenderedImage getRenderedImage(GanttExportSettings settings) {
        class ChartImageVisitorImpl implements ChartImageVisitor {
            private RenderedChartImage myRenderedImage;
            private Graphics2D myGraphics;
            private BufferedImage myTreeImage;

            @Override
            public void acceptLogo(ChartDimensions d, Image logo) {
                Graphics2D g = getGraphics(d);
                g.setBackground(Color.WHITE);
                g.clearRect(0, 0, d.getTreeWidth(), d.getLogoHeight());
                // Hack: by adding 35, the left part of the logo becomes visible, otherwise it gets chopped off
                g.drawImage(logo, 35, 0, null);
            }

            @Override
            public void acceptTable(ChartDimensions d, Component header, Component table) {
                Graphics2D g = getGraphics(d);
                g.translate(0, d.getLogoHeight());
                header.print(g);

                g.translate(0, d.getTableHeaderHeight());
                table.print(g);
            }

            @Override
            public void acceptChart(ChartDimensions d, ChartModel model) {
                myRenderedImage = new RenderedChartImage(model, myTreeImage, d.getChartWidth(), d.getChartHeight()
                        + d.getLogoHeight(), d.getLogoHeight());
            }

            private Graphics2D getGraphics(ChartDimensions d) {
                if (myGraphics == null) {
                    myTreeImage  = new BufferedImage(d.getTreeWidth(), d.getChartHeight() + d.getLogoHeight(), BufferedImage.TYPE_INT_RGB);
                    myGraphics = myTreeImage.createGraphics();
                }
                return myGraphics;
            }
        };
        ChartImageVisitorImpl visitor = new ChartImageVisitorImpl();
        buildImage(settings, visitor);
        return visitor.myRenderedImage;
    }

    @Override
    public Date getStartDate() {
        return getChartModel().getStartDate();
    }

    @Override
    public void setStartDate(Date startDate) {
        startDate = getBottomTimeUnit().adjustLeft(startDate);
        getChartModel().setStartDate(startDate);
    }

    @Override
    public void scrollBy(TaskLength duration) {
        setStartDate(getChartModel().getTaskManager().shift(getStartDate(), duration));
    }

    @Override
    public void setStartOffset(int pixels) {
        getChartModel().setHorizontalOffset(pixels);
    }

    private TimeUnit getBottomTimeUnit() {
        return getChartModel().getBottomUnit();
    }

    @Override
    public Date getEndDate() {
        return getChartModel().getEndDate();
    }

    @Override
    public void setDimensions(int height, int width) {
        Dimension bounds = new Dimension(width, height);
        getChartModel().setBounds(bounds);
    }

    @Override
    public void setBottomUnit(TimeUnit bottomUnit) {
        getChartModel().setBottomTimeUnit(bottomUnit);
    }

    @Override
    public void setTopUnit(TimeUnit topUnit) {
        getChartModel().setTopTimeUnit(topUnit);
    }

    @Override
    public void setBottomUnitWidth(int width) {
        getChartModel().setBottomUnitWidth(width);
    }

    @Override
    public String getName() {
        return myChartComponent.getName();
    }

    @Override
    public void reset() {
        myChartComponent.reset();
    }

    @Override
    public GPOptionGroup[] getOptionGroups() {
        return getChartModel().getChartOptionGroups();
    }

    @Override
    public Chart createCopy() {
        return new AbstractChartImplementation(myProject, myUiFacade, getChartModel().createCopy(), myChartComponent);
    }

    @Override
    public Object getAdapter(Class arg0) {
        return null;
    }

    @Override
    public ChartSelection getSelection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus canPaste(ChartSelection selection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void paste(ChartSelection selection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addSelectionListener(ChartSelectionListener listener) {
        mySelectionListeners.add(listener);
    }

    @Override
    public void removeSelectionListener(ChartSelectionListener listener) {
        mySelectionListeners.remove(listener);
    }

    protected void fireSelectionChanged() {
        for (Iterator<ChartSelectionListener> listeners = mySelectionListeners.iterator(); listeners.hasNext();) {
            ChartSelectionListener nextListener = listeners.next();
            nextListener.selectionChanged();
        }
    }

    @Override
    public void addRenderer(ChartRendererBase renderer) {
        myChartModel.addRenderer(renderer);
    }

    @Override
    public void resetRenderers() {
        myChartModel.resetRenderers();
    }

    @Override
    public ChartModel getModel() {
        return myChartModel;
    }

    @Override
    public ChartUIConfiguration getStyle() {
        return myChartModel.getChartUIConfiguration();
    }

    public int getHeaderHeight(JComponent tableContainer, JTable table) {
        JTableHeader tableHeader = table.getTableHeader();
        Point headerLocation = tableHeader.getLocationOnScreen();
        Point treeLocation = tableContainer.getLocationOnScreen();
        return headerLocation.y - treeLocation.y + tableHeader.getHeight() + HEADER_OFFSET;

    }

    protected static class ChartSelectionImpl implements ChartSelection {
        private List<Task> myTasks = new ArrayList<Task>();
        private List<Task> myTasksRO = Collections.unmodifiableList(myTasks);
        private List<HumanResource> myHumanResources = new ArrayList<HumanResource>();
        private List<HumanResource> myHumanResourceRO = Collections.unmodifiableList(myHumanResources);
        private boolean isTransactionRunning;

        @Override
        public boolean isEmpty() {
            return myTasks.isEmpty() && myHumanResources.isEmpty();
        }

        @Override
        public List<Task> getTasks() {
            return myTasksRO;
        }

        @Override
        public List<HumanResource> getHumanResources() {
            return myHumanResourceRO;
        }

        @Override
        public IStatus isDeletable() {
            return Status.OK_STATUS;
        }

        @Override
        public void startCopyClipboardTransaction() {
            if (isTransactionRunning) {
                throw new IllegalStateException("Transaction is already running");
            }
            isTransactionRunning = true;
        }
        @Override
        public void startMoveClipboardTransaction() {
            if (isTransactionRunning) {
                throw new IllegalStateException("Transaction is already running");
            }
            isTransactionRunning = true;
        }
        @Override
        public void cancelClipboardTransaction() {
            isTransactionRunning = false;
        }
        @Override
        public void commitClipboardTransaction() {
            isTransactionRunning = false;
        }

    }
}