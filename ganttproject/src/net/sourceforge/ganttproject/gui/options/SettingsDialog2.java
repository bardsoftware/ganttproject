/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.plugins.PluginManager;

public class SettingsDialog2 {
    private final IGanttProject myProject;
    private final UIFacade myUIFacade;
    private List<ListItem> myItems;
    private final OptionPageProvider[] myProviders;
    private final List<GPOptionGroup> myOptionGroups = new ArrayList<GPOptionGroup>();

    public SettingsDialog2(IGanttProject project, UIFacade uifacade) {
        myProject = project;
        myUIFacade = uifacade;
        myProviders = (OptionPageProvider[]) PluginManager.getExtensions(
            "net.sourceforge.ganttproject.OptionPageProvider", OptionPageProvider.class);
        for (OptionPageProvider p : myProviders) {
            p.init(project, uifacade);
            for (GPOptionGroup optionGroup : p.getOptionGroups()) {
                myOptionGroups.add(optionGroup);
            }
        }
    }
    
    public void show() {
        OkAction okAction = new OkAction() {
            public void actionPerformed(ActionEvent e) {
                for (GPOptionGroup optionGroup : myOptionGroups) {
                    optionGroup.commit();
                }
            }
        };
        CancelAction cancelAction = new CancelAction() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            }
        };
        myUIFacade.showDialog(
            getComponent(), new Action[] {okAction, cancelAction}, GanttLanguage.getInstance().getText("settings"));
    }

    static class ListItem {
        final boolean isGroupHeader;
        final String name;
        final Container component;
        
        ListItem(boolean isGroupHeader, String name, Container component) {
            this.isGroupHeader = isGroupHeader;
            this.name = name;
            this.component = component;
        }
    }

    private Component getComponent() {
        final JPanel contentPanel = new JPanel(new CardLayout());
        
        myItems = getListItems(myProviders);
        for (ListItem li : myItems) {
            contentPanel.add(li.component, li.name);
        }
        final JList pagesList = new JList(new AbstractListModel() {
            @Override
            public Object getElementAt(int idx) {
                return myItems.get(idx);
            }
            @Override
            public int getSize() {
                return myItems.size();
            }
        });
        pagesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList list, Object value, int idx, boolean isSelected, boolean cellHasFocus) {
                ListItem listItem = (ListItem) value;
                Component defaultResult = super.getListCellRendererComponent(
                    list, listItem.name, idx, isSelected, false);
                Font font = defaultResult.getFont(); 
                
                JPanel wrapper = new JPanel(new BorderLayout());
                if (listItem.isGroupHeader) {
                    defaultResult.setFont(font.deriveFont(Font.BOLD, font.getSize()+2.0f));
                    defaultResult.setBackground(Color.ORANGE);
                    defaultResult.setForeground(Color.BLACK);
                    wrapper.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0,0,0, Color.ORANGE.darker()),
                        BorderFactory.createEmptyBorder(2, 3, 2, 5)));
                } else {
                    defaultResult.setFont(font.deriveFont(font.getSize()+2.0f));
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
                } else {
                    return wrapper;
                }
            }
        });
        pagesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pagesList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                CardLayout cardLayout = (CardLayout) contentPanel.getLayout();
                ListItem listItem = (ListItem) pagesList.getSelectedValue();
                cardLayout.show(contentPanel, listItem.name);
            }
        });
        pagesList.setBorder(BorderFactory.createEtchedBorder());
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                pagesList.setSelectedIndex(0);
            }
        });

        final JPanel pagesPanel = new JPanel(new BorderLayout());
        pagesPanel.add(pagesList, BorderLayout.CENTER);
        
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(pagesPanel, BorderLayout.WEST);
        rootPanel.add(contentPanel, BorderLayout.CENTER);    
        rootPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        return rootPanel;
    }
    
    private List<ListItem> getListItems(OptionPageProvider[] providers) {
        Map<String, OptionPageProvider> pageId_provider = new HashMap<String, OptionPageProvider>();
        for (OptionPageProvider p : providers) {
            pageId_provider.put(p.getPageID(), p);
        }
        List<ListItem> items = new ArrayList<ListItem>();
        String[] listConfig = GanttLanguage.getInstance().getText("settings.pages").split(",");
        for (String s : listConfig) {
            ListItem li;
            if (s.startsWith("group.")) {
                String groupNameKey = s.substring("group.".length());
                li = new ListItem(
                    true, 
                    GanttLanguage.getInstance().correctLabel(GanttLanguage.getInstance().getText(groupNameKey)), 
                    new JPanel());
            } else {
                OptionPageProvider p = pageId_provider.get(s);
                assert p != null : "OptionPageProvider with pageID=" + s + " not found";
                li = new ListItem(false, p.toString(), 
                    (Container)new OptionPageProviderPanel(p, myProject, myUIFacade).getComponent());
            }
            items.add(li);
        }
        return items;
    }
}