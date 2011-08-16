/***************************************************************************
 ColorConvertion.java
 ------------------------------------------
 begin                : 6 juil. 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas@ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package net.sourceforge.ganttproject.util;

import java.awt.Color;
import java.util.regex.Pattern;

import net.sourceforge.ganttproject.GanttGraphicArea;

/**
 * @author athomas Convert the color format from and to differents versions
 */
public class ColorConvertion {

    /** @return the color as hexadecimal version like #RRGGBB */
    public static String getColor(Color color) {
        String res = "#";

        if (color.getRed() <= 15)
            res += "0";
        res += Integer.toHexString(color.getRed());
        if (color.getGreen() <= 15)
            res += "0";
        res += Integer.toHexString(color.getGreen());
        if (color.getBlue() <= 15)
            res += "0";
        res += Integer.toHexString(color.getBlue());

        return res;
    }

    /** parse a string as hew and return the corresponding color. */
    public static Color determineColor(String hexString) {
        if (!Pattern.matches("#[0-9abcdefABCDEF]{6}+", hexString)) {
            return GanttGraphicArea.taskDefaultColor;
        }
        int r, g, b;
        r = Integer.valueOf(hexString.substring(1, 3), 16).intValue();
        g = Integer.valueOf(hexString.substring(3, 5), 16).intValue();
        b = Integer.valueOf(hexString.substring(5, 7), 16).intValue();
        return new Color(r, g, b);
    }
}
