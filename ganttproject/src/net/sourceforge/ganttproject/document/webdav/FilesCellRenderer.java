/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

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
package net.sourceforge.ganttproject.document.webdav;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.document.webdav.WebDavResource.WebDavException;

import com.google.common.base.Joiner;

/**
 * Renders WebDAV file names and lock information
 *
 * @author dbarashev (Dmitry Barashev)
 */
class FilesCellRenderer implements ListCellRenderer {
  private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

  @Override
  public Component getListCellRendererComponent(JList list, Object value,
      int index, boolean isSelected, boolean cellHasFocus) {
    WebDavResource webdavResource = (WebDavResource) value;
    JComponent result;
    try {
      JComponent defaultComponent = (JComponent) defaultRenderer.getListCellRendererComponent(list, webdavResource.getName(), index, isSelected, cellHasFocus);
      List<String> lockOwners = webdavResource.getLockOwners();
      if (webdavResource.isCollection()) {
        result = new JLabel(webdavResource.getName(), GPAction.getIcon("16", "folder.png"), SwingConstants.LEADING);
      } else {
        if (lockOwners.isEmpty()) {
          result = defaultComponent;
        } else {
          JLabel name = new JLabel(webdavResource.getName());
          JLabel locks = new JLabel(Joiner.on(',').join(lockOwners), GPAction.getIcon("8", "status-locked.png"), SwingConstants.LEADING);
          locks.setFont(locks.getFont().deriveFont(locks.getFont().getSize()*0.82f));
          locks.setForeground(UIManager.getColor("List.disabledForeground"));
          JPanel box = new JPanel(new GridLayout(2, 1));
          box.add(name);
          box.add(locks);
          if (isSelected) {
            name.setBackground(UIManager.getColor("List.selectionBackground"));
            locks.setBackground(UIManager.getColor("List.selectionBackground"));
          }
          result = box;
        }
        result.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0), result.getBorder()));
      }
      result = decorate(result, defaultComponent, isSelected);
      return result;

    } catch (WebDavException e) {
      return new JLabel("error");
    }
  }

  private JComponent decorate(JComponent result, JComponent defaultComponent, boolean isSelected) {
    if (isSelected) {
      result.setBackground(UIManager.getColor("List.selectionBackground"));
    } else {
      result.setBackground(UIManager.getColor("List.background"));
    }
    result.setBorder(defaultComponent.getBorder());
    return result;
  }
}
