/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.gui.options;

import java.awt.Component;

import javax.swing.JComponent;

import biz.ganttproject.core.option.GPOptionGroup;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;
import net.sourceforge.ganttproject.language.GanttLanguage;

public abstract class OptionPageProviderBase implements OptionPageProvider {
  private String myPageID;
  private IGanttProject myProject;
  private UIFacade myUiFacade;

  protected OptionPageProviderBase(String pageID) {
    myPageID = pageID;
  }

  @Override
  public String getPageID() {
    return myPageID;
  }

  @Override
  public boolean hasCustomComponent() {
    return false;
  }

  @Override
  public Component buildPageComponent() {
    return null;
  }

  @Override
  public void init(IGanttProject project, UIFacade uiFacade) {
    myProject = project;
    myUiFacade = uiFacade;
  }

  @Override
  public void commit() {
    for (GPOptionGroup optionGroup : getOptionGroups()) {
      optionGroup.commit();
    }
  }

  @Override
  public abstract GPOptionGroup[] getOptionGroups();

  protected IGanttProject getProject() {
    return myProject;
  }

  protected UIFacade getUiFacade() {
    return myUiFacade;
  }

  @Override
  public String toString() {
    return GanttLanguage.getInstance().getText(
        new OptionsPageBuilder.I18N().getCanonicalOptionPageLabelKey(getPageID()));
  }

  protected static JComponent wrapContentComponent(JComponent contentComponent, String title, String description) {
    return UIUtil.createTopAndCenter(TopPanel.create(title, description), contentComponent);
  }

  protected String getCanonicalPageTitle() {
    return GanttLanguage.getInstance().getText(
        new OptionsPageBuilder.I18N().getCanonicalOptionPageTitleKey(getPageID()));
  }
}
