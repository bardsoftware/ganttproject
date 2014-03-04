/*
Copyright 2013 BarD Software s.r.o

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
package net.sourceforge.ganttproject.wizard;

import javax.swing.JComponent;

/**
 * Abstraction of the wizard page. Page lifecycle is the following:
 * -- page is instantiated
 * -- when user reaches page in the wizard, its getComponent method is called, the returned component
 *    is inserted into the wizard UI and page's setActive method is called. Wizard instance is passed to setActive,
 *    and if page realizes that it has a next page, it should instantiate it and call wizard's {@link AbstractWizard.setNextPage()} method
 * -- when user leaves the page, setActive() is called with null argument. It is not necessary to do anything, but page
 *    can save its data at this moment
 *
 * @author dbarashev
 */
public interface WizardPage {
  /** @return the title of the page */
  String getTitle();

  /** @return the Component that makes the page */
  JComponent getComponent();

  /**
   * Indicates that this page is now active (visible, that is) in the wizard or just've been made inactive (user switched
   * to some other page)
   *
   * @param wizard wizard instance when page becomes active or null when it deactivates
   */
  void setActive(AbstractWizard wizard);
}
