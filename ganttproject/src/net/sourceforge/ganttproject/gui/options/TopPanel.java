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
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * A panel to show the top.gif image and write a title and a comment.
 *
 * @author athomas
 */
public class TopPanel extends JPanel {

    public TopPanel(String title, String comment) {
        setLayout(new BorderLayout());

        final JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.ORANGE);
        topPanel.setForeground(Color.BLACK);
        topPanel.setBorder(BorderFactory.createLineBorder(Color.ORANGE.darker()));

        final JLabel labelTitle = new JLabel(title);
        labelTitle.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        labelTitle.setFont(labelTitle.getFont().deriveFont(Font.BOLD));
        topPanel.add(labelTitle, BorderLayout.WEST);

        if (comment != null && !comment.isEmpty()) {
            JTextArea textArea = new JTextArea(comment);
            textArea.setEditable(false);
            textArea.setFont(textArea.getFont().deriveFont(Font.PLAIN, textArea.getFont().getSize()-2));
            textArea.setDragEnabled(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setMargin(new Insets(0, 5, 2, 5));
            textArea.setBackground(Color.ORANGE);
            topPanel.add(textArea, BorderLayout.SOUTH);
        }
        add(topPanel, BorderLayout.CENTER);
        applyComponentOrientation(GanttLanguage.getInstance().getComponentOrientation());
    }
}
