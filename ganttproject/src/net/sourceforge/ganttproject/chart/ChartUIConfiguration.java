/*
GanttProject is an opensource project management tool. License: GPL3
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
package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.awt.Font;

import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.UIConfiguration;

/**
 * @author bard
 */
public class ChartUIConfiguration {

  private final Font mySpanningRowTextFont;

  private final Color mySpanningHeaderBackgroundColor;

  private final Color myHeaderBorderColor;

  private final Color myHorizontalGutterColor1 = new Color(0.807f, 0.807f, 0.807f);

  private final Color myHorizontalGutterColor2 = Color.white;

  private final Color myBottomUnitGridColor;

  private final Font myBottomUnitFont;

  private final Color myWorkingTimeBackgroundColor;

  private final Color myHolidayTimeBackgroundColor;

  private final Color myPublicHolidayTimeBackgroundColor;

  private int myRowHeight;

  private UIConfiguration myProjectConfig;

  private int myHeaderHeight = 44;

  private int myYOffset = 0;

  private final int myMargin = 4;

  ChartUIConfiguration(UIConfiguration projectConfig) {
    mySpanningRowTextFont = Fonts.TOP_UNIT_FONT;
    myBottomUnitFont = projectConfig.getChartMainFont();
    mySpanningHeaderBackgroundColor = new Color(0.93f, 0.93f, 0.93f);
    myHeaderBorderColor = new Color(0.482f, 0.482f, 0.482f);
    myWorkingTimeBackgroundColor = Color.WHITE;
    myHolidayTimeBackgroundColor = new Color(0.9f, 0.9f, 0.9f);
    myPublicHolidayTimeBackgroundColor = new Color(240, 220, 240);
    // myHeaderBorderColor = new Color(0f, 1f, 0f);
    myBottomUnitGridColor = new Color(0.482f, 0.482f, 0.482f);
    myProjectConfig = projectConfig;
  }

  Font getSpanningHeaderFont() {
    return mySpanningRowTextFont;
  }

  Font getBottomUnitFont() {
    return myBottomUnitFont;
  }

  public int getHeaderHeight() {
    return myHeaderHeight;
  }

  public void setHeaderHeight(int headerHeight) {
    myHeaderHeight = headerHeight;
  }

  public int getSpanningHeaderHeight() {
    return myHeaderHeight / 2;
  }

  public Color getSpanningHeaderBackgroundColor() {
    return mySpanningHeaderBackgroundColor;
  }

  public Color getHeaderBorderColor() {
    return myHeaderBorderColor;
  }

  public Color getHorizontalGutterColor1() {
    return myHorizontalGutterColor1;
  }

  public Color getHorizontalGutterColor2() {
    return myHorizontalGutterColor2;
  }

  public Color getBottomUnitGridColor() {
    return myBottomUnitGridColor;
  }

  public Color getWorkingTimeBackgroundColor() {
    return myWorkingTimeBackgroundColor;
  }

  public Color getHolidayTimeBackgroundColor() {
    return myHolidayTimeBackgroundColor;
  }

  public Color getPublicHolidayTimeBackgroundColor() {
    return myPublicHolidayTimeBackgroundColor;
  }

  public int getRowHeight() {
    return myRowHeight;
  }

  public void setRowHeight(int rowHeight) {
    myRowHeight = rowHeight;
  }

  public Color getDefaultTaskColor() {
    return myProjectConfig.getTaskColor();
  }

  public Color getWeekEndColor() {
    return myProjectConfig.getWeekEndColor();
  }

  public boolean isRedlineOn() {
    return myProjectConfig.isRedlineOn();
  }

  public Font getChartFont() {
    return myProjectConfig.getChartMainFont();
  }

  public Color getResourceNormalLoadColor() {
    return myProjectConfig.getResourceColor();
  }

  public Color getResourceOverloadColor() {
    return myProjectConfig.getResourceOverloadColor();
  }

  public Color getResourceUnderLoadColor() {
    return myProjectConfig.getResourceUnderloadColor();
  }

  public Color getPreviousTaskColor() {
    return myProjectConfig.getPreviousTaskColor();
  }

  public Color getEarlierPreviousTaskColor() {
    return myProjectConfig.getEarlierPreviousTaskColor();
  }

  public Color getLaterPreviousTaskColor() {
    return myProjectConfig.getLaterPreviousTaskColor();
  }

  public boolean isCriticalPathOn() {
    return myProjectConfig.isCriticalPathOn();
  }

  public Color getDayOffColor() {
    return myProjectConfig.getDayOffColor();
  }

  public int getYOffSet() {
    return myYOffset;
  }

  public void setYOffSet(int offset) {
    myYOffset = offset;
  }

  public int getMargin() {
    return myMargin;
  }

  public float getWeekendAlphaValue() {
    return myProjectConfig.getWeekendAlphaRenderingOption().getValueAsFloat();
  }

  public ChartUIConfiguration createCopy() {
    ChartUIConfiguration copy = new ChartUIConfiguration(myProjectConfig);
    copy.setHeaderHeight(getHeaderHeight());
    copy.setRowHeight(getRowHeight());
    copy.setYOffSet(getYOffSet());
    return copy;
  }
}
