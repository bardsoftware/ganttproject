/***************************************************************************
 AboutAuthorPanel.java 
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
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.sourceforge.ganttproject.gui.options.GeneralOptionPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas Form that show informations about authors
 */
public class AboutAuthorPanel extends GeneralOptionPanel {

    /** Constructor. */
    public AboutAuthorPanel() {
        super(GanttLanguage.getInstance().getText("authors"), GanttLanguage
                .getInstance().getText("settingsAuthors"));

        JTable jTableAuthors = new JTable();
        AuthorsFieldTableModel modelauthors = new AuthorsFieldTableModel();

        jTableAuthors.setModel(modelauthors);
        modelauthors.addField(new AuthorsInfo("THOMAS Alexandre",
                "alexthomas@ganttproject.org"));
        modelauthors.addField(new AuthorsInfo("BARASHEV Dmitry",
                "dbarashev@ganttproject.org"));
        modelauthors.addField(new AuthorsInfo("AZURE Joe",
                "jazure@fishalaska.com"));
        modelauthors.addField(new AuthorsInfo("BARANNE Benoit",
                "bbaranne@users.sourceforge.net"));
        modelauthors.addField(new AuthorsInfo("BOHN Nicolas",
                "nicolasbohn@users.sourceforge.net"));
        modelauthors.addField(new AuthorsInfo("LIBS Jean-Marc",
                "jyhem@users.sourceforge.net"));
        modelauthors.addField(new AuthorsInfo("", ""));
        modelauthors.addField(new AuthorsInfo("BARMEIER Matthias",
                "matthias.barmeier@sourcepark.de"));
       modelauthors.addField(new AuthorsInfo("MURAS Joanna",
       			"zabucha@users.sourceforge.net"));
        modelauthors.addField(new AuthorsInfo("LU Cui", "cuix1@yahoo.com"));
        modelauthors.addField(new AuthorsInfo("SCHULZ Maik",
                "post@maikschulz.de"));
        modelauthors.addField(new AuthorsInfo("HAEUSLER Michael",
                "michael@akatose.de"));
        modelauthors.addField(new AuthorsInfo("ANDRESEN Roger",
                "Roger@Andresen.com"));
        modelauthors.addField(new AuthorsInfo("", ""));
        modelauthors.addField(new AuthorsInfo("LIPINSKI Pawel",
                "pawel.lipinski@javart.com.pl"));
        modelauthors.addField(new AuthorsInfo("AUDRU C�dric",
                "cedricaudru@yahoo.fr"));
        modelauthors.addField(new AuthorsInfo("A S Hodel", "hodelas@ieee.org"));
        modelauthors.addField(new AuthorsInfo("BALAZS Major",
                "BMajor@Graphisoft.hu"));
        modelauthors.addField(new AuthorsInfo("BARZILAI Igor",
        	"igor.barzilai@free.fr"));
        modelauthors.addField(new AuthorsInfo("BREZINA Marek",
                "marek.brezina@seznam.cz"));
        modelauthors.addField(new AuthorsInfo("BROKER Rick",
                "rick_broker@yahoo.com"));
        modelauthors.addField(new AuthorsInfo("CASTILHO Danilo",
                "dncastilho@yahoo.com.br"));
        modelauthors.addField(new AuthorsInfo("CHRISTENSEN Carsten",
                "coc-kultur@aalborg.dk"));
        modelauthors.addField(new AuthorsInfo("CROUNSE Brian",
                "brian@crounse.name"));
        modelauthors.addField(new AuthorsInfo("FERRAZ Nelson",
                "nferraz@phperl.com"));
        modelauthors.addField(new AuthorsInfo("GLOEGL Michael",
                "gloegl@fmi.uni-passau.de"));
        modelauthors.addField(new AuthorsInfo("GIANTSIDIS Nikos",
        	"nickgiant@yahoo.com")); //greek translation        
        modelauthors.addField(new AuthorsInfo("HERRMANN G.",
                "herr@nike.eonet.ne.jp"));
        modelauthors.addField(new AuthorsInfo("HURSEY John",
                "joshh@cs.earlham.edu"));
        modelauthors.addField(new AuthorsInfo("ILES Jon",
                "jon.iles@tapsterrock.com"));
        modelauthors.addField(new AuthorsInfo("Jiwon Kim",
                "yesdi@users.sourceforge.net")); // Korean language
        modelauthors
                .addField(new AuthorsInfo("KARLGREN Jussi", "jussi@sics.se"));
        modelauthors.addField(new AuthorsInfo("KITSIK Ahti",
                "ahti.kitsik@gmail.com"));
        modelauthors.addField(new AuthorsInfo("LIN Kirin",
                "kirinlin@users.sourceforge.net"));
        modelauthors.addField(new AuthorsInfo("L'KENFACK Etienne",
                "etiennelk@hotmail.com"));
        modelauthors.addField(new AuthorsInfo("MADSEN Jan",
                "JM-kultur@aalborg.dk"));
        modelauthors.addField(new AuthorsInfo("MARCH Stephen",
                "steve@enerds.ca"));
        modelauthors.addField(new AuthorsInfo("MIYATA Yasuhiro",
                "yasuhiro.miyata@ulsystems.co.jp"));
        modelauthors.addField(new AuthorsInfo("NATHANAEL Uwe",
                "Uwe.Nathanael@t-online.de"));
        modelauthors
                .addField(new AuthorsInfo("OGNESS John", "john@ogness.net"));
        modelauthors.addField(new AuthorsInfo("PAOLETTI Tomaso",
                "tom@ipaoletti.net"));
        modelauthors.addField(new AuthorsInfo("PLUSCHKE Andreas",
                "homepage.plueschke@gmx.de"));
        modelauthors.addField(new AuthorsInfo("RACINOWSKI Przemyslaw",
                "p.racinowski@wp.pl"));
        modelauthors.addField(new AuthorsInfo("REY Juan", "juanrey@inicia.es"));
        modelauthors.addField(new AuthorsInfo("SAHIN Cengiz",
                "cengiz@sahinc.de"));
        modelauthors.addField(new AuthorsInfo("SENIGAGLIESI Paolo",
                "senigagliesi_inf@hotmail.com"));
        modelauthors.addField(new AuthorsInfo("SHABTAI Yoav",
                "yoavs@pmp-medical.com"));
        modelauthors.addField(new AuthorsInfo("STAVRIDES Paul",
                "pstav@adelie.net"));
        modelauthors.addField(new AuthorsInfo("VAN DER WIEL Andre",
                "a.vd.wiel@chello.nl"));
        modelauthors.addField(new AuthorsInfo("VOCI Elio",
                "elio.voci@gawab.com"));
        modelauthors.addField(new AuthorsInfo("ZAVOLZHSKY Alexandr",
                "zavolzhsky@mail.ru"));
        modelauthors.addField(new AuthorsInfo("Zheko Zhekov",
                "zhekov@electrostart.com")); // bulgarian language

        JPanel authorsPanel = new JPanel(new BorderLayout());
        authorsPanel.add(new JScrollPane(jTableAuthors), BorderLayout.CENTER);
        authorsPanel.setPreferredSize(new Dimension(400, 350));
        vb.add(authorsPanel);

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
        // nothing to change
    }

    class AuthorsInfo {
        private String name;

        private String mail;

        public AuthorsInfo(String name, String mail) {
            this.name = name;
            this.mail = mail;
        }

        public String getName() {
            return name;
        }

        public String getMail() {
            return mail;
        }
    }

    class AuthorsFieldTableModel extends AbstractTableModel {
        private GanttLanguage language = GanttLanguage.getInstance();

        final String[] columnNames = { language.getText("name"),
                language.getText("colMail") };

        final Class[] columnClasses = { String.class, String.class };

        Vector<AuthorsInfo> data = new Vector<AuthorsInfo>();

        public void addField(AuthorsInfo w) {
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
            AuthorsInfo info = data.elementAt(row);
            if (col == 0)
                return info.getName();
            else if (col == 1)
                return info.getMail();
            else
                return null;
        }

        public boolean isCellEditable(int row, int col) {
            return col == 1;
        }
    }
}
