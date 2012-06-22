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
import javax.swing.UIManager;

import org.apache.webdav.lib.WebdavResource;

import com.google.common.base.Joiner;

/**
 * Renders WebDAV file names and lock information
 *
 * @author dbarashev (Dmitry Barashev)
 */
class FilesCellRenderer implements ListCellRenderer<WebdavResource> {
  private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

  @Override
  public Component getListCellRendererComponent(JList<? extends WebdavResource> list, WebdavResource value,
      int index, boolean isSelected, boolean cellHasFocus) {
    JComponent result;
    JComponent defaultComponent = (JComponent) defaultRenderer.getListCellRendererComponent(list, value.getName(), index, isSelected, cellHasFocus);
    List<String> lockOwners = WebDavStorageImpl.getLockOwners(value);
    if (lockOwners.isEmpty()) {
      result = defaultComponent;
    } else {
      JLabel name = new JLabel(value.getName());
      JLabel locks = new JLabel("locked: " + Joiner.on(',').join(lockOwners));
      locks.setFont(locks.getFont().deriveFont(locks.getFont().getSize()*0.82f));
      locks.setForeground(UIManager.getColor("List.disabledForeground"));
      JPanel box = new JPanel(new GridLayout(2, 1));
      box.add(name);
      box.add(locks);
      if (isSelected) {
        box.setBackground(UIManager.getColor("List.selectionBackground"));
        name.setBackground(UIManager.getColor("List.selectionBackground"));
        locks.setBackground(UIManager.getColor("List.selectionBackground"));
      } else {
        box.setBackground(UIManager.getColor("List.background"));
      }
      box.setBorder(defaultComponent.getBorder());
      result = box;
    }
    result.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0), result.getBorder()));
    return result;
  }
}
