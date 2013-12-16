package net.sourceforge.ganttproject.wizard;

import javax.swing.JComponent;

public interface WizardPage {
  /** @return the title of the page */
  String getTitle();

  /** @return the Component that makes the page */
  JComponent getComponent();

  void setActive(AbstractWizard wizard);
}
