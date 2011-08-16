/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Thomas Alexandre, GanttProject Team

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
package net.sourceforge.ganttproject.gui.about;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.options.GeneralOptionPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas Panel to show used library
 */
public class AboutLibraryPanel extends GeneralOptionPanel {

    public AboutLibraryPanel() {
        super(GanttLanguage.getInstance().getText("library"), GanttLanguage
                .getInstance().getText("settingsLibrary"));

        JTable jTableLib = new JTable();
        LibraryFieldTableModel modellib = new LibraryFieldTableModel();

        jTableLib.setModel(modellib);
        modellib.addField(new LibraryInfo("ganttproject-"
                + GanttProject.version + ".jar", "ganttproject.biz",
                "Project Manager."));
        modellib.addField(new LibraryInfo("commons-httpclient.jar",
                "jakarta.apache.org", "WebDAV support."));
        modellib.addField(new LibraryInfo("commons-logging.jar",
                "jakarta.apache.org", "WebDAV support."));
        modellib.addField(new LibraryInfo("jdom-20040226-.jar", "jdom.org",
                "WebDAV support."));
        modellib.addField(new LibraryInfo("webdavlib.jar",
                "jakarta.apache.org", "WebDAV support."));
        modellib.addField(new LibraryInfo("jakarta-slide-webdavlib-2.1b1.jar",
                "webdav.apache.org", "WebDAV support."));
        modellib.addField(new LibraryInfo("helpgui-1.1.jar", "helpgui.sf.net",
                "Help viewer in html."));
        modellib.addField(new LibraryInfo("jgoodies-looks-1.2.2.jar",
                "www.jgoodies.com", "Nice Look'n'Feel."));
        modellib.addField(new LibraryInfo("avalon.jar", "avalon.apache.org",
                "Graphic Export (used for pdf)"));
        modellib.addField(new LibraryInfo("batik.jar", "xml.apache.org",
                "Graphic Export (used for pdf)"));
        modellib.addField(new LibraryInfo("fop.jar", "xml.apache.org",
                "Pdf export library."));
        modellib.addField(new LibraryInfo("fop-font-metrics.jar",
                "xml.apache.org", "Special font use."));
        modellib.addField(new LibraryInfo("junit-3.8.1.jar", "junit.org",
                "Unit tests."));
        modellib.addField(new LibraryInfo("jdnc-modifBen.jar",
                "https://jdnc.dev.java.net/", "Swing components"));
        modellib.addField(new LibraryInfo("mpxj_0.0.25_CVS.jar",
                "http://mpxj.sourceforge.net/",
                "Microsoft Project compatibility"));
        modellib.addField(new LibraryInfo("icons", "eclipse.org",
                "Icons from the Eclipse project (IBM)."));

        JPanel libraryPanel = new JPanel(new BorderLayout());
        libraryPanel.add(new JScrollPane(jTableLib), BorderLayout.CENTER);
        libraryPanel.setPreferredSize(new Dimension(400, 350));
        vb.add(libraryPanel);

        applyComponentOrientation(language.getComponentOrientation());
    }

    @Override
    public boolean applyChanges(boolean askForApply) {
        return false;
    }

    @Override
    public void initialize() {
    }

    /** Store informations for library uses. */
    class LibraryInfo {
        private String libName;

        private String libWeb;

        private String libComment;

        public LibraryInfo(String libName, String libCompany, String libComment) {
            this.libName = libName;
            this.libWeb = libCompany;
            this.libComment = libComment;
        }

        public String getName() {
            return libName;
        }

        public String getWeb() {
            return libWeb;
        }

        public String getComment() {
            return libComment;
        }
    }

    class LibraryFieldTableModel extends AbstractTableModel {
        private GanttLanguage language = GanttLanguage.getInstance();

        final String[] columnNames = { language.getText("name"),
                language.getText("web"), language.getText("notes") };

        final Class<?>[] columnClasses = { String.class, String.class,
                String.class };

        Vector<LibraryInfo> data = new Vector<LibraryInfo>();

        public void addField(LibraryInfo w) {
            data.addElement(w);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return columnClasses[c];
        }

        public Object getValueAt(int row, int col) {
            LibraryInfo info = data.elementAt(row);
            if (col == 0) {
                return info.getName();
            } else if (col == 1) {
                return info.getWeb();
            } else if (col == 2) {
                return info.getComment();
            } else {
                return null;
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 1;
        }
    }
}
