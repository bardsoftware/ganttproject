// Copyright (C) 2021 BarD Software
package biz.ganttproject.core.model.task;

/**
 * @author dbarashev@bardsoftware.com
 */
public
enum ConstraintType {
  startstart, finishstart, finishfinish, startfinish;
  private static final String[] PERSISTENT_VALUES = new String[]{
      "SS", "FS", "FF", "SF"
  };

  public String getPersistentValue() {
    return String.valueOf(ordinal() + 1);
  }

  public String getReadablePersistentValue() {
    return PERSISTENT_VALUES[ordinal()];
  }

  public static ConstraintType fromPersistentValue(String dependencyTypeAsString) {
    return ConstraintType.values()[Integer.parseInt(dependencyTypeAsString) - 1];
  }

  public static ConstraintType fromReadablePersistentValue(String str) {
    for (int i = 0; i < PERSISTENT_VALUES.length; i++) {
      if (PERSISTENT_VALUES[i].equals(str)) {
        return ConstraintType.values()[i];
      }
    }
    throw new IllegalArgumentException("Can't find constraint by persistent value=" + str);
  }
}
