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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sourceforge.ganttproject.gui.options.GeneralOptionPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * The license panel
 *
 * @author athomas
 */
public class AboutLicensePanel extends GeneralOptionPanel {

    public AboutLicensePanel() {
        super(GanttLanguage.getInstance().getText("license"), GanttLanguage
                .getInstance().getText("settingsLicense"));

        // TODO Put license in file and read it
        // TODO Update license to GPL 2 we use
        JTextArea taLicense = new JTextArea();
        StringBuffer text = new StringBuffer();
        text.append("This program is free software; you can redistribute it and/or modify it under the terms of the GNU General ");
        text.append("Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.\n\n");
        text.append("This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied ");
        text.append("warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n\n");
        text.append("You should have received a copy of the GNU General Public License along with this program; if not, write to the Free ");
        text.append("Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.\n\n");
        text.append("Browse the entire GPL license at http://www.gnu.org/copyleft/gpl.html\n");

        taLicense.setText(text.toString());
        taLicense.setEditable(false);
        taLicense.setLineWrap(true);
        taLicense.setWrapStyleWord(true);

        JPanel licensePanel = new JPanel(new BorderLayout());
        licensePanel.add(new JScrollPane(taLicense), BorderLayout.CENTER);
        licensePanel.setPreferredSize(new Dimension(400, 350));
        vb.add(licensePanel);

        applyComponentOrientation(language.getComponentOrientation());
    }

    @Override
    public boolean applyChanges(boolean askForApply) {
        return false;
    }

    @Override
    public void initialize() {
    }
}
