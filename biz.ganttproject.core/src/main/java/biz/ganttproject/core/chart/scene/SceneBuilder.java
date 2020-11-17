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
 * This class is responsible for building a scene, which is some particular part of a chart.
 * For instance, chart timeline is a scene. The process of building involves creating 
 * shapes on a canvas instance. When all scene builders finish their work, the 
 * rendering engine takes their canvases and renders their contents on the target
 * device (e.g. on Graphics instance). 
 *  
 * @author dbarashev (Dmitry Barashev)
 */
public interface SceneBuilder {
  /**
   * Resets this builder and sets scene height, which is a height of a user-visible viewport 
   * of a chart
   * 
   * @param sceneHeight
   */
  void reset(int sceneHeight);
  
  /**
   * Builds a scene 
   */
  void build();
  
  /**
   * @return canvas instance
   */
  Canvas getCanvas();
}
