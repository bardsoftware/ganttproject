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
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Box;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

/** Abstract class for the Options panels */
public abstract class GeneralOptionPanel extends JPanel {

  protected static final GanttLanguage language = GanttLanguage.getInstance();

  /** General vertical box. */
  protected final Box vb = Box.createVerticalBox();

  private final String myTitle;

  private final String myComment;

  private final UIFacade myUiFacade;

  public GeneralOptionPanel(UIFacade uiFacade, String title, String comment) {
    super();
    myUiFacade = uiFacade;
    setLayout(new BorderLayout());
    add(vb, BorderLayout.CENTER);
    myTitle = title;
    myComment = comment;
  }

  public Component getComponent() {
    return this;
  }

  /**
   * This method checks if options panel has value that got changed changed. And
   * ask the user to commit changes if askForApply is true.
   *
   * @returns true when there were changes which needed to be committed
   */
  public abstract boolean applyChanges(boolean askForApply);

  /** Initialize the component. */
  public abstract void initialize();

  /** This method asks the user for saving the changes. */
  public boolean askForApplyChanges() {
    return (UIFacade.Choice.YES == getUIFacade().showConfirmationDialog(language.getText("msg20"),
        language.getText("question")));
  }

  public String getTitle() {
    return myTitle;
  }

  public String getComment() {
    return myComment;
  }

  protected UIFacade getUIFacade() {
    return myUiFacade;
  }

  public void rollback() {
    initialize();
  }
}
