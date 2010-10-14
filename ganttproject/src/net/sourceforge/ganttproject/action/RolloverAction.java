package net.sourceforge.ganttproject.action;

import javax.swing.Icon;

public interface RolloverAction {
    Icon getIconOnMouseOver();

    void setIconSize(String iconSize);

    void isIconVisible(boolean isNull);
}
