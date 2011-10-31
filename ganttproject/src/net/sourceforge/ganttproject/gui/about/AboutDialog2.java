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
package net.sourceforge.ganttproject.gui.about;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.gui.AbstractPagesDialog;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.TopPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class AboutDialog2 extends AbstractPagesDialog {

    public AboutDialog2(UIFacade uiFacade) {
        super("about", uiFacade, createPages());
    }

    private static List<ListItem> createPages() {
        List<ListItem> result = new ArrayList<AbstractPagesDialog.ListItem>();
        result.add(createHtmlPage("authors"));
        result.add(createHtmlPage("license"));
        result.add(createHtmlPage("library"));
        return result;
    }

    private static ListItem createHtmlPage(String key) {
        JPanel result = new JPanel(new BorderLayout());
        JPanel authors = new JPanel(new BorderLayout());
        authors.add(new JLabel(i18n("about." + key)), BorderLayout.NORTH);
        result.add(TopPanel.create(i18n(key), null), BorderLayout.NORTH);
        result.add(authors, BorderLayout.CENTER);
        result.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return new ListItem(false, key, i18n(key), result);
    }

    private static String i18n(String key) {
        return GanttLanguage.getInstance().getText(key);
    }

    @Override
    protected void onOk() {
    }
}
