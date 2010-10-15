/***************************************************************************
 AboutJavaInfosPanel.java 
 ------------------------------------------
 begin                : 29 juin 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package net.sourceforge.ganttproject.gui.about;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.security.AccessControlException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.options.GeneralOptionPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas About the java informations panel.
 */
public class AboutJavaInfosPanel extends GeneralOptionPanel {

    /** Constructor. */
    public AboutJavaInfosPanel(GanttProject parent) {
        super(GanttLanguage.getInstance().getText("jinfos"), GanttLanguage
                .getInstance().getText("settingsJavaInfos"), parent);

        JTable jTableProperties = new JTable();
        AboutFieldTableModel modelproperties = new AboutFieldTableModel();
        jTableProperties.setModel(modelproperties);

        try {
            Enumeration props = System.getProperties().propertyNames();
            SortedSet<String> s = new TreeSet<String>();
            while (props.hasMoreElements()) {
                s.add((String) props.nextElement());
            }
            Iterator<String> i = s.iterator();
            while (i.hasNext()) {
                String prop = i.next();
                modelproperties.addField(new SystemInfo(prop, System
                        .getProperty(prop)));
            }
        } catch (AccessControlException e) {
            // This can happen when running in a sandbox (Java WebStart)
            System.err.println(e + ": " + e.getMessage());
        }

        JPanel infosPanel = new JPanel(new BorderLayout());
        infosPanel.add(new JScrollPane(jTableProperties), BorderLayout.CENTER);
        infosPanel.setPreferredSize(new Dimension(400, 350));
        vb.add(infosPanel);

        applyComponentOrientation(language.getComponentOrientation());
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.ganttproject.gui.options.GeneralOptionPanel#applyChanges(boolean)
     */
    public boolean applyChanges(boolean askForApply) {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.ganttproject.gui.options.GeneralOptionPanel#initialize()
     */
    public void initialize() {
    }

    class SystemInfo {
        private String name;

        private String value;

        public SystemInfo(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    class AboutFieldTableModel extends AbstractTableModel {
        private GanttLanguage language = GanttLanguage.getInstance();

        final String[] columnNames = { language.getText("name"),
                language.getText("value") };

        final Class[] columnClasses = { String.class, String.class };

        Vector<SystemInfo> data = new Vector<SystemInfo>();

        public void addField(SystemInfo w) {
            data.addElement(w);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Class getColumnClass(int c) {
            return columnClasses[c];
        }

        public Object getValueAt(int row, int col) {
            SystemInfo info = data.elementAt(row);
            if (col == 0)
                return info.getName();
            else if (col == 1)
                return info.getValue();
            else
                return null;
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }
}
