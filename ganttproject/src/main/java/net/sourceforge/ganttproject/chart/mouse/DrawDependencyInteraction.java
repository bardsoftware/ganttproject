/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.chart.mouse;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;

import net.sourceforge.ganttproject.ChartComponentBase;
import net.sourceforge.ganttproject.chart.DependencyInteractionRenderer;
import net.sourceforge.ganttproject.chart.item.TaskRegularAreaChartItem;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyCollection;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;

public class DrawDependencyInteraction extends MouseInteractionBase implements MouseInteraction {

  private final Task myTask;

  private Point myStartPoint;

  private DependencyInteractionRenderer myArrow;

  private Task myDependant;

  private MouseEvent myLastMouseEvent = null;

  private final UIFacade myUiFacade;

  private final TaskDependencyCollection myDependencyCollection;

  private final ChartModelFacade myChartModelFacade;

  private final Component myComponent;

  public static interface ChartModelFacade {
    Task findTaskUnderMousePointer(int xpos, int ypos);

    TaskDependency.Hardness getDefaultHardness();
  }

  public DrawDependencyInteraction(MouseEvent initiatingEvent, TaskRegularAreaChartItem taskArea,
      TimelineFacade timelineFacade, ChartModelFacade chartModelFacade, UIFacade uiFacade,
      TaskDependencyCollection dependencyCollection) {
    super(null, timelineFacade);
    myUiFacade = uiFacade;
    myChartModelFacade = chartModelFacade;
    myDependencyCollection = dependencyCollection;
    myStartPoint = initiatingEvent.getPoint();
    myTask = taskArea.getTask();
    myArrow = new DependencyInteractionRenderer(myStartPoint.x, myStartPoint.y, myStartPoint.x, myStartPoint.y);
    myComponent = initiatingEvent.getComponent();
    myComponent.setCursor(ChartComponentBase.HAND_CURSOR);
  }

  @Override
  public void apply(MouseEvent event) {
    myArrow.changePoint2(event.getX(), event.getY());
    myLastMouseEvent = event;
  }

  @Override
  public void finish() {
    myComponent.setCursor(ChartComponentBase.DEFAULT_CURSOR);
    if (myLastMouseEvent != null) {
      myDependant = myChartModelFacade.findTaskUnderMousePointer(myLastMouseEvent.getX(), myLastMouseEvent.getY());
      final Task dependee = myTask;
      if (myDependant != null) {
        if (myDependencyCollection.canCreateDependency(myDependant, dependee)) {
          myUiFacade.getUndoManager().undoableEdit("Draw dependency", new Runnable() {
            @Override
            public void run() {
              try {
                TaskDependency dep = myDependencyCollection.createDependency(myDependant, dependee,
                    new FinishStartConstraintImpl());
                dep.setHardness(myChartModelFacade.getDefaultHardness());

              } catch (TaskDependencyException e) {
                myUiFacade.showErrorDialog(e);
              }
            }
          });
        }
      } else {
        myArrow = new DependencyInteractionRenderer();
        myUiFacade.getActiveChart().reset();
      }
    }
  }

  @Override
  public void paint(Graphics g) {
    myArrow.paint(g);
  }
}