package net.sourceforge.ganttproject.gui.projectwizard;

import java.awt.Component;

public interface WizardPage {
    String getTitle();

    Component getComponent();

    void setActive(boolean b);
}
