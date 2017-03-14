/*
GanttProject is an opensource project management tool. License: GPL3
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
package net.sourceforge.ganttproject.chart;

import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.option.DefaultColorOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.TimeUnitStack;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChartModelResource extends ChartModelBase {

  private final HumanResourceManager myManager;

  private final GPOptionGroup myColorOptions;

  private final ColorOption myResourceNormalLoadOption;

  private final ColorOption myResourceOverloadOption;

  private final ColorOption myResourceUnderloadOption;

  private final ColorOption myDayOffOption;

  private final ResourceChart myResourceChart;

  private static class ResourceLoadOption extends DefaultColorOption implements GP1XOptionConverter {
    private final String myTagName;
    private final String myAttributeName;

    ResourceLoadOption(String id, String tagName, String attributeName) {
      super(id);
      myTagName = tagName;
      myAttributeName = attributeName;
    }

    @Override
    public String getTagName() {
      return myTagName;
    }

    @Override
    public String getAttributeName() {
      return myAttributeName;
    }

    @Override
    public void loadValue(String legacyValue) {
      loadPersistentValue(legacyValue);
    }
  }

  public ChartModelResource(TaskManager taskManager, HumanResourceManager resourceManager, TimeUnitStack timeUnitStack,
      final UIConfiguration projectConfig, ResourceChart resourceChart) {
    super(taskManager, timeUnitStack, projectConfig);
    myResourceChart = resourceChart;
    ResourceLoadRenderer resourceLoadRenderer = new ResourceLoadRenderer(this, resourceChart);
    addRenderer(resourceLoadRenderer);
    myManager = resourceManager;
    {
      myResourceNormalLoadOption = new ResourceLoadOption("resourceChartColors.normalLoad", "colors", "resources") {
        @Override
        public void commit() {
          super.commit();
          projectConfig.setResourceColor(getValue());
        }
      };
      myResourceNormalLoadOption.lock();
      myResourceNormalLoadOption.setValue(new Color(140, 182, 206));
      myResourceNormalLoadOption.commit();
    }
    {
      myResourceOverloadOption = new ResourceLoadOption("resourceChartColors.overLoad", "colors", "resourceOverload") {
        @Override
        public void commit() {
          super.commit();
          projectConfig.setResourceOverloadColor(getValue());
        }
      };
      myResourceOverloadOption.lock();
      myResourceOverloadOption.setValue(new Color(229, 50, 50));
      myResourceOverloadOption.commit();
    }
    {
      myResourceUnderloadOption = new DefaultColorOption("resourceChartColors.underLoad") {
        @Override
        public void commit() {
          super.commit();
          projectConfig.setResourceUnderloadColor(getValue());
        }
      };
      myResourceUnderloadOption.lock();
      myResourceUnderloadOption.setValue(new Color(50, 229, 50));
      myResourceUnderloadOption.commit();
    }
    {
      myDayOffOption = new DefaultColorOption("resourceChartColors.dayOff") {
        @Override
        public void commit() {
          super.commit();
          projectConfig.setDayOffColor(getValue());
        }
      };
      myDayOffOption.lock();
      myDayOffOption.setValue(new Color(0.9f, 1f, 0.17f));
      myDayOffOption.commit();
    }
    myColorOptions = new GPOptionGroup("resourceChartColors", new GPOption[] { myResourceNormalLoadOption,
        myResourceOverloadOption, myResourceUnderloadOption, myDayOffOption });
  }

  // public void paint(Graphics g) {
  // super.paint(g);
  // myResourceLoadRenderer.render();
  // myResourceLoadRenderer.getPrimitiveContainer().paint(getPainter(), g);
  // }

  public HumanResource[] getVisibleResources() {
    return myManager.getResources().toArray(new HumanResource[0]);
  }

  @Override
  public GPOptionGroup[] getChartOptionGroups() {
    List<GPOptionGroup> result = new ArrayList<GPOptionGroup>();
    result.add(myColorOptions);
    return result.toArray(new GPOptionGroup[result.size()]);
  }

  // @Override
  // protected int getRowCount() {
  // return getVisibleResources().length;
  // }

  @Override
  public ChartModelBase createCopy() {
    ChartModelBase result = new ChartModelResource(myTaskManager, myManager, myTimeUnitStack, getProjectConfig(),
        myResourceChart);
    super.setupCopy(result);
    return result;
  }

  @Override
  public void setVisibleTasks(List<Task> visibleTasks) {
    // TODO Auto-generated method stub
  }

  @Override
  public int calculateRowHeight() {
    return Math.max(getChartUIConfiguration().getRowHeight(), getProjectConfig().getAppFontSize().get());
  }
}