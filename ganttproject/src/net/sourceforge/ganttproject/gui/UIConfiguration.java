/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject Team

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
package net.sourceforge.ganttproject.gui;

import biz.ganttproject.core.chart.render.AlphaRenderingOption;
import biz.ganttproject.core.option.BooleanOption;
import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.option.FontOption;
import biz.ganttproject.core.option.IntegerOption;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;

import javax.swing.*;
import java.awt.*;

/**
 * @author bard
 */
public class UIConfiguration {
//  private final Font myChartMainFont;

  /** default resource color */
  private Color myResColor;

  /** overload resource color */
  private Color myResOverColor;

  /** underload resource color */
  private Color myResUnderColor;

  private Color myEarlierPreviousTaskColor;

  private Color myLaterPreviousTaskColor;

  private Color myPreviousTaskColor;

  /** Color used for weekend indications */
  private Color myWeekEndColor;

  /** Color used for days off (and holidays) */
  private Color myDayOffColor;

  private boolean isRedlineOn;

  private boolean isCriticalPathOn;

  private final AlphaRenderingOption myWeekendAlphaRenderingOption;
  private final RedlineOption myRedlineOption = new RedlineOption();
  private BooleanOption myProjectDatesOption = new DefaultBooleanOption("showProjectDates");

  private FontOption myChartFontOption;
  private Supplier<Integer> myAppFontSize;
  private IntegerOption myDpiOption;

  public UIConfiguration(Color taskColor, boolean isRedlineOn) {
//    myChartMainFont = chartMainFont == null ? Fonts.DEFAULT_CHART_FONT : chartMainFont;
    this.isRedlineOn = isRedlineOn;
    myResColor = new Color(140, 182, 206);
    myResOverColor = new Color(229, 50, 50);
    myResUnderColor = new Color(50, 229, 50);
    myEarlierPreviousTaskColor = new Color(50, 229, 50);
    myLaterPreviousTaskColor = new Color(229, 50, 50);
    myPreviousTaskColor = Color.LIGHT_GRAY;
    myWeekEndColor = Color.GRAY;
    myDayOffColor = new Color(0.9f, 1f, 0.17f);
    myWeekendAlphaRenderingOption = new AlphaRenderingOption();
    myAppFontSize = new Supplier<Integer>() {
      @Override
      public Integer get() {
        Font tableFont = (Font) UIManager.get("Table.font");
        return tableFont.getSize() + 8;
      }
    };
  }

//  public Font getChartMainFont() {
//    return myChartMainFont;
//  }
//
  public Color getResourceColor() {
    return myResColor;
  }

  public void setResourceColor(Color myResColor) {
    this.myResColor = myResColor;
  }

  public Color getResourceOverloadColor() {
    return myResOverColor;
  }

  public void setResourceOverloadColor(Color myResOverColor) {
    this.myResOverColor = myResOverColor;
  }

  public Color getResourceUnderloadColor() {
    return myResUnderColor;
  }

  public void setResourceUnderloadColor(Color myResUnderColor) {
    this.myResUnderColor = myResUnderColor;
  }

  public Color getEarlierPreviousTaskColor() {
    return myEarlierPreviousTaskColor;
  }

  public void setEarlierPreviousTaskColor(Color earlierTaskColor) {
    this.myEarlierPreviousTaskColor = earlierTaskColor;
  }

  public Color getLaterPreviousTaskColor() {
    return myLaterPreviousTaskColor;
  }

  public void setLaterPreviousTaskColor(Color laterTaskColor) {
    this.myLaterPreviousTaskColor = laterTaskColor;
  }

  public Color getPreviousTaskColor() {
    return myPreviousTaskColor;
  }

  public void setPreviousTaskColor(Color previousTaskColor) {
    this.myPreviousTaskColor = previousTaskColor;
  }

  public Color getWeekEndColor() {
    return myWeekEndColor;
  }

  public Color getDayOffColor() {
    return myDayOffColor;
  }

  public void setWeekEndColor(Color myWeekEndColor) {
    this.myWeekEndColor = myWeekEndColor;
  }

  public void setDayOffColor(Color dayOffColor) {
    this.myDayOffColor = dayOffColor;
  }

  public boolean isRedlineOn() {
    return isRedlineOn;
  }

  public void setRedlineOn(boolean redlineOn) {
    isRedlineOn = redlineOn;
  }

  public boolean isCriticalPathOn() {
    return isCriticalPathOn;
  }

  public void setCriticalPathOn(boolean isOn) {
    this.isCriticalPathOn = isOn;
  }

  public AlphaRenderingOption getWeekendAlphaRenderingOption() {
    return myWeekendAlphaRenderingOption;
  }

  class RedlineOption extends DefaultBooleanOption implements GP1XOptionConverter {
    RedlineOption() {
      super("showTodayLine");
    }

    @Override
    public String getTagName() {
      return "redline";
    }

    @Override
    public String getAttributeName() {
      return "value";
    }

    @Override
    public void loadValue(String legacyValue) {
      lock();
      loadPersistentValue(legacyValue);
      commit();
    }

    @Override
    public void commit() {
      super.commit();
      setRedlineOn(isChecked());
    }
  };

  public BooleanOption getRedlineOption() {
    return myRedlineOption;
  }

  public BooleanOption getProjectBoundariesOption() {
    return myProjectDatesOption;
  }

  public void setChartFontOption(FontOption chartFontOption) {
    myChartFontOption = chartFontOption;
  }

  public FontOption getChartFontOption() {
    return myChartFontOption;
  }

  public Supplier<Integer> getAppFontSize() {
    return myAppFontSize;
  }

  public IntegerOption getDpiOption() {
    return myDpiOption;
  }

  public void setDpiOption(IntegerOption dpiOption) {
    myDpiOption = Preconditions.checkNotNull(dpiOption);
  }
}
