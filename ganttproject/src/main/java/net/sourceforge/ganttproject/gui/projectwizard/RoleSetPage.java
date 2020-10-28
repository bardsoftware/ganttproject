/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Alexandre Thomas, Dmitry Barashev

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
package net.sourceforge.ganttproject.gui.projectwizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractListModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleSet;

public class RoleSetPage implements WizardPage {
  private final I18N myI18N;

  private RoleSetListModel myListModel;

  RoleSetPage(RoleSet[] roleSets, I18N i18n) {
    myI18N = i18n;
    myListModel = new RoleSetListModel(roleSets, i18n);
  }

  @Override
  public String getTitle() {
    return myI18N.getProjectDomainPageTitle();
  }

  @Override
  public Component getComponent() {
    Box domainBox = new Box(BoxLayout.PAGE_AXIS);
    JLabel label = new JLabel(GanttLanguage.getInstance().getText("chooseRoleSets"));

    final JList roleSetsList = new JList(myListModel);
    roleSetsList.setCellRenderer(myListModel.getCellRenderer());
    roleSetsList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int index = roleSetsList.locationToIndex(e.getPoint());
        myListModel.toggle(index);
      }
    });
    roleSetsList.setAlignmentX(0);
    label.setLabelFor(roleSetsList);
    label.setAlignmentX(0);

    domainBox.add(label);
    domainBox.add(Box.createVerticalStrut(5));
    domainBox.add(roleSetsList);

    JPanel result = new JPanel(new BorderLayout());
    result.add(domainBox, BorderLayout.CENTER);
    // result.setBorder(LineBorder.createBlackLineBorder());
    return result;
  }

  private static class RoleSetListModel extends AbstractListModel implements ListCellRenderer {
    private final RoleSet[] myRoleSets;

    private final I18N myI18n;

    RoleSetListModel(RoleSet[] roleSets, I18N i18n) {
      myRoleSets = roleSets;
      myI18n = i18n;
    }

    public void toggle(int index) {
      if (!isTheOnlyEnabled(myRoleSets[index])) {
        myRoleSets[index].setEnabled(!myRoleSets[index].isEnabled());
        fireContentsChanged(this, index, index);
      }
    }

    @Override
    public int getSize() {
      return myRoleSets.length;
    }

    @Override
    public Object getElementAt(int index) {
      return myRoleSets[index];
    }

    ListCellRenderer getCellRenderer() {
      return this;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
        boolean cellHasFocus) {
      final RoleSet roleSet = (RoleSet) value;
      final JCheckBox result = new JCheckBox(myI18n.getRoleSetDisplayName(roleSet), roleSet.isEnabled());
      if (isTheOnlyEnabled(roleSet)) {
        result.setEnabled(false);
      }
      result.setToolTipText(createTooltipText(roleSet));
      return result;
    }

    private boolean isTheOnlyEnabled(RoleSet roleSet) {
      boolean result = true;
      for (int i = 0; i < myRoleSets.length; i++) {
        if (myRoleSets[i] != roleSet && myRoleSets[i].isEnabled()) {
          result = false;
          break;
        }
      }
      return result;
    }

    private String createTooltipText(RoleSet roleSet) {
      StringBuffer result = new StringBuffer();
      result.append(myI18n.getRolesetTooltipHeader(roleSet.getName()));
      Role[] roles = roleSet.getRoles();
      for (int i = 0; i < roles.length; i++) {
        Role nextRole = roles[i];
        result.append(myI18n.formatRoleForTooltip(nextRole));
      }

      result.append(myI18n.getRolesetTooltipFooter());
      return result.toString();
    }
  }

  @Override
  public void setActive(boolean active) {
  }
}
