/*
Copyright 2003-2012 GanttProject Team

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
package biz.ganttproject.core.chart.render;

import biz.ganttproject.core.option.DefaultEnumerationOption;

/**
 * Option which controls opacity of task bars over weekends.
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class AlphaRenderingOption extends DefaultEnumerationOption<Object> {
  private static final String[] VALUES = new String[] { "chart.weekend_alpha_rendering.100",
      "chart.weekend_alpha_rendering.75", "chart.weekend_alpha_rendering.50", "chart.weekend_alpha_rendering.25",
      "chart.weekend_alpha_rendering.0" };
  private static final float[] FLOATS = new float[] { 1f, 0.75f, 0.5f, 0.25f, 0f };

  private int myIndex = 0;

  public AlphaRenderingOption() {
    super("chart.weekend_alpha_rendering", VALUES);
  }

  @Override
  public void commit() {
    super.commit();
    updateIndex();
  }

  @Override
  public void loadPersistentValue(String value) {
    super.loadPersistentValue(value);
    updateIndex();
  }

  public float getValueAsFloat() {
    return FLOATS[myIndex];
  }
  
  private void updateIndex() {
    String value = getValue();
    for (int i = 0; i < VALUES.length; i++) {
      if (VALUES[i].equals(value)) {
        myIndex = i;
        break;
      }
    }    
  }
}