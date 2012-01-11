/*
 LICENSE:

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 3 of the License, or
 (at your option) any later version.

 Copyright (C) 2004, GanttProject Development Team
 */
package net.sourceforge.ganttproject.gui;

import java.awt.Color;
import java.awt.Font;

import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class UIConfiguration {
    private final Font myMenuFont;

    private final Font myChartMainFont;

    private Color myTaskColor;
    private Color myProjectLevelTaskColor;

    private Color myResColor; // default resource color

    private Color myResOverColor; // overload resoure color

    private Color myResUnderColor; // underload resoure color

    private Color myEarlierPreviousTaskColor;

    private Color myLaterPreviousTaskColor;

    private Color myPreviousTaskColor;

    private Color myWeekEndColor; // week end color

    private Color myDayOffColor;

    private boolean isRedlineOn;

    private boolean isCriticalPathOn;

    private final AlphaRenderingOption myWeekendAlphaRenderingOption;
    private final RedlineOption myRedlineOption = new RedlineOption();
    private BooleanOption myProjectDatesOption = new DefaultBooleanOption(
    "showProjectDates");
    public UIConfiguration(Font menuFont, Font chartMainFont, Color taskColor,
            boolean isRedlineOn) {
        myMenuFont = menuFont == null ? Fonts.DEFAULT_MENU_FONT
                : menuFont;
        myChartMainFont = chartMainFont == null ? Fonts.DEFAULT_CHART_FONT : chartMainFont;
        this.isRedlineOn = isRedlineOn;
        setTaskColor(taskColor);
        myResColor = new Color(140, 182, 206);
        myResOverColor = new Color(229, 50, 50);
        myResUnderColor = new Color(50, 229, 50);
        myEarlierPreviousTaskColor = new Color(50, 229, 50);
        myLaterPreviousTaskColor = new Color(229, 50, 50);
        myPreviousTaskColor = Color.LIGHT_GRAY;
        myWeekEndColor = Color.GRAY;
        myDayOffColor = new Color(0.9f, 1f, 0.17f);
        myWeekendAlphaRenderingOption = new AlphaRenderingOption(this);
    }

    public Font getMenuFont() {
        return myMenuFont;
    }

    public Font getChartMainFont() {
        return myChartMainFont;
    }

    public Color getTaskColor() {
        return myProjectLevelTaskColor==null ? myTaskColor : myProjectLevelTaskColor;
    }

    public void setTaskColor(Color myTaskColor) {
        this.myTaskColor = myTaskColor;
    }

    public void setProjectLevelTaskColor(Color color) {
        myProjectLevelTaskColor = color;
    }
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

    public static class AlphaRenderingOption extends DefaultEnumerationOption<Object>  {
        static final String[] VALUES = new String[] {"chart.weekend_alpha_rendering.100", "chart.weekend_alpha_rendering.75", "chart.weekend_alpha_rendering.50", "chart.weekend_alpha_rendering.25", "chart.weekend_alpha_rendering.0"};
        final float[] FLOATS = new float[] {1f, 0.75f, 0.5f, 0.25f, 0f};
        private int myIndex=0;
        AlphaRenderingOption(UIConfiguration config) {
            super("chart.weekend_alpha_rendering", VALUES);
//            for (int i=0; i<VALUES.length; i++) {
//                if (FLOATS[i]==config.getWeekendAlphaValue()) {
//                    myIndex = i;
//                    break;
//                }
//            }
        }
        @Override
        public void commit() {
            super.commit();
            String value = getValue();
            for (int i=0; i<VALUES.length; i++) {
                if (VALUES[i].equals(value)) {
                    myIndex = i;
                    break;
                }
            }
        }
        public float getValueAsFloat() {
            return FLOATS[myIndex];
        }

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
}
