/*
 * Created on 17.01.2004
 *
 */
package net.sourceforge.ganttproject.gui;

import java.util.HashMap;
import java.util.Map;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.jgoodies.looks.LookUtils;

/**
 * @author Michael Haeusler (michael at akatose.de) This singleton class stores
 *         info about the installed LookAndFeels.
 */
public class GanttLookAndFeels {

  protected Map<String, GanttLookAndFeelInfo> infoByClass;

  protected Map<String, GanttLookAndFeelInfo> infoByName;

  protected static GanttLookAndFeels singleton;

  static {
    UIManager.put("ClassLoader", LookUtils.class.getClassLoader());
    UIManager.installLookAndFeel("Plastic", "com.jgoodies.looks.plastic.PlasticLookAndFeel");
  }

  protected GanttLookAndFeels() {
    infoByClass = new HashMap<String, GanttLookAndFeelInfo>();
    infoByName = new HashMap<String, GanttLookAndFeelInfo>();
    LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
    for (int i = 0; i < lookAndFeels.length; i++) {
      GanttLookAndFeelInfo info = new GanttLookAndFeelInfo(lookAndFeels[i]);
      addLookAndFeel(info);
    }
  }

  protected void addLookAndFeel(GanttLookAndFeelInfo info) {
    if (info.getName().startsWith("Kunststoff") && System.getProperty("os.name").startsWith("Mac")) {
      System.err.println("LookAndFeel not added (Kunststoff is ignored on MacOS).");
    } else {
      if (!infoByClass.containsKey(info.getClassName())) {
        infoByClass.put(info.getClassName(), info);
        infoByName.put(info.getName(), info);
      } else {
        System.err.println("LookAndFeel " + info + "(" + info.getClassName() + ") already installed.");
      }
    }
  }

  public GanttLookAndFeelInfo getInfoByClass(String className) {
    return infoByClass.get(className);
  }

  public GanttLookAndFeelInfo getInfoByName(String name) {
    return infoByName.get(name);
  }

  public GanttLookAndFeelInfo getDefaultInfo() {
    GanttLookAndFeelInfo info = getInfoByClass(UIManager.getSystemLookAndFeelClassName());
    if (null == info)
      info = getInfoByClass(UIManager.getCrossPlatformLookAndFeelClassName());
    return info;
  }

  public GanttLookAndFeelInfo[] getInstalledLookAndFeels() {
    GanttLookAndFeelInfo[] lookAndFeels = new GanttLookAndFeelInfo[0];
    return infoByClass.values().toArray(lookAndFeels);
  }

  public static GanttLookAndFeels getGanttLookAndFeels() {
    if (singleton == null) {
      singleton = new GanttLookAndFeels();
    }
    return singleton;
  }
}
