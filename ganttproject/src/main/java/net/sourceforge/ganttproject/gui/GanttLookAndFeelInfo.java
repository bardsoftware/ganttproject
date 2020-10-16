/*
 * Created on 17.01.2004
 *
 */
package net.sourceforge.ganttproject.gui;

import javax.swing.UIManager.LookAndFeelInfo;

/**
 * @author Michael Haeusler (michael at akatose.de) This class changes the
 *         toString method of the standard LookAndFeelInfo.
 */
public class GanttLookAndFeelInfo extends LookAndFeelInfo {

  protected String toString;

  public GanttLookAndFeelInfo(String name, String className) {
    super(name, className);
    toString = name;
  }

  public GanttLookAndFeelInfo(LookAndFeelInfo info) {
    this(info.getName(), info.getClassName());
  }

  /**
   * returns the name of the LookAndFeel
   */
  @Override
  public String toString() {
    return toString;
  }

}
