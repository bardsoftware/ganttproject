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
package net.sourceforge.ganttproject.gui.about;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.GPVersion;
import net.sourceforge.ganttproject.gui.AbstractPagesDialog;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.TopPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class AboutDialog2 extends AbstractPagesDialog {

    private static final Color HTML_BACKGROUND = new JPanel().getBackground();

    public AboutDialog2(UIFacade uiFacade) {
        super("about", uiFacade, createPages());
    }

    private static List<ListItem> createPages() {
        List<ListItem> result = new ArrayList<AbstractPagesDialog.ListItem>();
        result.add(createSummaryPage());
        result.add(createHtmlPage("authors"));
        result.add(createHtmlPage("license"));
        result.add(createHtmlPage("library"));
        return result;
    }

    private static ListItem createSummaryPage() {
        JPanel result = new JPanel(new BorderLayout());
        JEditorPane html = createHtml(GanttLanguage.getInstance().formatText("about.summary", GPVersion.PRAHA));
        html.setAlignmentX(0.5f);
        JPanel htmlWrapper = new JPanel(new BorderLayout());
        htmlWrapper.add(html, BorderLayout.NORTH);
        result.add(htmlWrapper, BorderLayout.NORTH);

        JLabel icon = new JLabel(new ImageIcon(AboutDialog2.class.getResource("/icons/ganttproject.png")));
        icon.setAlignmentX(0.5f);
        JPanel iconWrapper = new JPanel(new BorderLayout());
        iconWrapper.add(icon, BorderLayout.NORTH);
        result.add(iconWrapper, BorderLayout.CENTER);

        result.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
        return new ListItem(false, "summary", i18n("summary"), result);
    }

    private static ListItem createHtmlPage(String key) {
        JPanel result = new JPanel(new BorderLayout());
        JComponent topPanel = TopPanel.create(i18n(key), null);
        topPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        result.add(topPanel, BorderLayout.NORTH);

        JPanel planePageWrapper = new JPanel(new BorderLayout());
        planePageWrapper.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        JComponent planePage = createHtml(i18n("about." + key));
        planePage.setAlignmentX(Component.LEFT_ALIGNMENT);
        planePageWrapper.add(planePage, BorderLayout.NORTH);
        result.add(planePageWrapper, BorderLayout.CENTER);

        return new ListItem(false, key, i18n(key), result);
    }


    private static JEditorPane createHtml(String html) {
        JEditorPane htmlPane = new JEditorPane("text/html", html);
        htmlPane.setEditable(false);
        htmlPane.setBackground(HTML_BACKGROUND);
        htmlPane.addHyperlinkListener(NotificationManager.DEFAULT_HYPERLINK_LISTENER);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        htmlPane.setSize(new Dimension(screenSize.width/3, Integer.MAX_VALUE));
        return htmlPane;
    }
    private static String i18n(String key) {
        return GanttLanguage.getInstance().getText(key);
    }

    @Override
    protected void onOk() {
    }
}
