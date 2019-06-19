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
package net.sourceforge.ganttproject.gui;

import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.language.GanttLanguage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;

public abstract class AbstractPagesDialog {
  private final UIFacade myUIFacade;
  private final List<ListItem> myItems;
  private final HashSet<String> myPageIds = new HashSet<>();
  private final String myTitleKey;
  private JList<ListItem> myPagesList;

  protected AbstractPagesDialog(String titleKey, UIFacade uifacade, List<ListItem> pages) {
    myTitleKey = titleKey;
    myUIFacade = uifacade;
    myItems = pages;
    for (ListItem li : myItems) {
      myPageIds.add(li.id);
    }
  }

  protected boolean isPageVisible(String pageId) {
    return myPageIds.contains(pageId);
  }

  public void show() {
    show(null);
  }

  public void show(String pageID) {
    OkAction okAction = new OkAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onOk();
      }
    };
    myUIFacade.createDialog(getComponent(), new Action[]{okAction, CancelAction.EMPTY},
        GanttLanguage.getInstance().getCorrectedLabel(myTitleKey)).show();
    if (pageID != null) {
      for (int i = 0; i < myItems.size(); i++) {
        if (pageID.equals(myItems.get(i).id)) {
          myPagesList.setSelectedIndex(i);
          break;
        }
      }
    }
  }

  protected static class ListItem {
    final boolean isGroupHeader;
    final String name;
    final Container component;
    final String id;

    public ListItem(boolean isGroupHeader, String id, String name, Container component) {
      this.isGroupHeader = isGroupHeader;
      this.id = id;
      this.name = name;
      this.component = component;
    }
  }

  protected abstract void onOk();

  private Component getComponent() {
    final JPanel contentPanel = new JPanel(new CardLayout());
    // Add panels to CardLayout
    for (ListItem li : myItems) {
      if (li.component != null) {
        contentPanel.add(li.component, li.id);
      }
    }
    myPagesList = new JList<ListItem>(new AbstractListModel<ListItem>() {
      @Override
      public ListItem getElementAt(int idx) {
        return myItems.get(idx);
      }

      @Override
      public int getSize() {
        return myItems.size();
      }
    });
    myPagesList.setVisibleRowCount(myItems.size());
    myPagesList.setCellRenderer(new DefaultListCellRenderer() {
      private final JButton EMPTY_BUTTON = new JButton();

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int idx, boolean isSelected,
                                                    boolean cellHasFocus) {
        ListItem listItem = (ListItem) value;
        Component defaultResult = super.getListCellRendererComponent(list, listItem.name, idx, isSelected, false);
        Font font = defaultResult.getFont();

        JPanel wrapper = new JPanel(new BorderLayout());
        if (listItem.isGroupHeader) {
          defaultResult.setFont(font.deriveFont(Font.BOLD, font.getSize() + 2.0f));
          defaultResult.setBackground(EMPTY_BUTTON.getBackground());
          defaultResult.setForeground(EMPTY_BUTTON.getForeground());
          wrapper.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(1, 0, 0, 0, EMPTY_BUTTON.getBackground().darker()),
              BorderFactory.createEmptyBorder(2, 3, 2, 5)));
        } else {
          defaultResult.setFont(font.deriveFont(font.getSize() + 2.0f));
          wrapper.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        }
        wrapper.setBackground(defaultResult.getBackground());
        wrapper.add(defaultResult, BorderLayout.CENTER);
        if (listItem.isGroupHeader) {
          JPanel headerWrapper = new JPanel(new BorderLayout());
          headerWrapper.setBackground(list.getBackground());
          if (idx > 0) {
            headerWrapper.add(new JLabel(" "), BorderLayout.NORTH);
          }
          headerWrapper.add(wrapper, BorderLayout.CENTER);
          return headerWrapper;
        }
        return wrapper;
      }
    });
    myPagesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myPagesList.addListSelectionListener(e -> {
      ListItem listItem = myPagesList.getSelectedValue();
      if (listItem.isGroupHeader) {
        // Assumes that the list does not end with a GroupHeader!
        myPagesList.setSelectedIndex(myPagesList.getSelectedIndex() + 1);
      } else {
        final CardLayout cardLayout = (CardLayout) contentPanel.getLayout();
        cardLayout.show(contentPanel, listItem.id);
      }
    });
    myPagesList.setBorder(BorderFactory.createEtchedBorder());

    SwingUtilities.invokeLater(() -> myPagesList.setSelectedIndex(0));

    JPanel rootPanel = new JPanel(new BorderLayout());
    rootPanel.add(myPagesList, BorderLayout.WEST);
    JPanel contentPanelWrapper = new JPanel(new BorderLayout());
    contentPanelWrapper.add(contentPanel, BorderLayout.CENTER);
    rootPanel.add(contentPanelWrapper, BorderLayout.CENTER);
    rootPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    return rootPanel;
  }

}
