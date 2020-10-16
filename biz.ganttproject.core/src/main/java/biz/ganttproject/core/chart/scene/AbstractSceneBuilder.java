/*
Copyright 2012 GanttProject Team

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
package biz.ganttproject.core.chart.scene;

import biz.ganttproject.core.chart.canvas.Canvas;

/**
 * Simple abstract scene builder with some boilerplate code.
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public abstract class AbstractSceneBuilder implements SceneBuilder {
  private final Canvas myCanvas;
  private int myHeight;

  protected AbstractSceneBuilder() {
    myCanvas = new Canvas();
  }

  protected AbstractSceneBuilder(Canvas canvas) {
    myCanvas = canvas;
  }
  
  public void setHeight(int height) {
    myHeight = height;
  }

  protected int getHeight() {
    return myHeight;
  }

  public Canvas getCanvas() {
    return myCanvas;
  }

  @Override
  public void reset(int sceneHeight) {
    getCanvas().clear();
    setHeight(sceneHeight);
  }
}
