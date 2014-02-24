/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject;

public abstract class GPVersion {
  public static String V2_0_1 = "2.0.1";
  public static String V2_0_2 = "2.0.2";
  public static String V2_0_3 = "2.0.3";
  public static String V2_0_4 = "2.0.4";
  public static String V2_0_5 = "2.0.5";
  public static String V2_0_6 = "2.0.6";
  public static String V2_0_7 = "2.0.7";
  public static String V2_0_8 = "2.0.8";
  public static String V2_0_9 = "2.0.9";
  public static String V2_0_10 = "2.0.10";
  public static String V2_0_X = V2_0_10;
  public static String PRAHA = "2.5.5 Praha (build 1256)";
  public static String BRNO = "2.6 Brno (build 1473)";
  public static String BRNO_2_6_1 = "2.6.1 Brno (build 1499)";
  public static String BRNO_2_6_2 = "2.6.2 Brno (build 1544)";
  public static String BRNO_2_6_3 = "2.6.3 Brno (build 1610)";
  public static String BRNO_2_6_4 = "2.6.4 Brno (build 1622)";
  public static String BRNO_2_6_5 = "2.6.5 Brno (build 1633)";
  public static String CURRENT = BRNO_2_6_5;

  public static String getCurrentVersionNumber() {
    return CURRENT.split("\\s")[0];
  }

  public static String getCurrentBuildNumber() {
    int posBuild = CURRENT.indexOf("(build ");
    if (posBuild == -1) {
      return null;
    }
    int posClosingBrace = CURRENT.indexOf(')', posBuild);
    return CURRENT.substring(posBuild + "(build ".length(), posClosingBrace);
  }
}
