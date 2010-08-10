/***************************************************************************
 TopPanel.java 
 ------------------------------------------
 begin                : 24 juin 2004
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
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas A panel to show the top.gif image and write a title and a
 *         comment.
 */
public class TopPanel extends JPanel {

    /** Constructor. */
    public TopPanel(String title, String comment) {
        setLayout(new BorderLayout());
        Box vb = Box.createVerticalBox();
        add(vb, BorderLayout.CENTER);

        final int iWidth = 420;

        // Paint a gray border around the panel
        JPanel topPanel = new JPanel(new BorderLayout()) {
            public void paint(Graphics g) {
                super.paint(g);
                g.setColor(new Color(0.67f, 0.66f, 0.6f));
                g.drawRect(0, 0, getWidth() - 1, 24);
            }
        };
        topPanel.setBackground(Color.white);

        JLabel labelTitle = new JLabel(" " + title);
        labelTitle.setFont(new Font(this.getFont().getFontName(), Font.BOLD,
                this.getFont().getSize()));
        topPanel.add(labelTitle, BorderLayout.WEST);
        topPanel.add(new myIconPanel(), BorderLayout.EAST);
        topPanel.setPreferredSize(new Dimension(iWidth, 26));

        vb.add(topPanel);
        JTextArea textArea = new JTextArea(comment);
        textArea.setEditable(false);
        textArea.setDragEnabled(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(3, 5, 3, 5));
        textArea.setBackground(getBackground());
        vb.add(textArea);

        // vb.setBorder(LineBorder.createBlackLineBorder());
        applyComponentOrientation(GanttLanguage.getInstance()
                .getComponentOrientation());
    }

    /** Little to print the little icon inside. */
    private class myIconPanel extends JPanel {
        Icon icon;

        public myIconPanel() {
            icon = new ImageIcon(getClass().getResource("/icons/top.gif"));
            applyComponentOrientation(GanttLanguage.getInstance()
                    .getComponentOrientation());
        }

        public void paintComponent(Graphics g) {
            icon.paintIcon(this, g, 0, 3);
        }

        /** The prefered size of this panel */
        public Dimension getPreferredSize() {
            return new Dimension(140, 20);
        }
    }
}
