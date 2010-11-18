/***************************************************************************
 GanttDialogCalendar.java  -  description
 -------------------
 begin                : dec 2004
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

package net.sourceforge.ganttproject.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Dialog allow you to select a date
 */
public class GanttDialogCalendar extends JDialog implements ActionListener {

    /** Language */
    private GanttLanguage language = GanttLanguage.getInstance();

    private JList listCalendar;

    private GanttPanelDate panelDate;

    private JButton bNew, bDelete, bImport, bSelectDays;

    private JRadioButton bWork, bNonWork;

    /** Constructor */
    public GanttDialogCalendar(JFrame parent) {
        super(parent, GanttProject.correctLabel(GanttLanguage.getInstance()
                .getText("editCalendars")), true);
        setResizable(false);

        JToolBar top = new JToolBar();
        top.setFloatable(false);
        bNew = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/new_16.gif")));
        bDelete = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/delete_16.gif")));
        bImport = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/open_16.gif")));
        bSelectDays = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/calendar_16.gif")));

        top.add(bNew);
        top.add(bDelete);
        top.add(bImport);
        top.addSeparator();
        top.add(bSelectDays);

        bNew.addActionListener(this);
        bDelete.addActionListener(this);
        bImport.addActionListener(this);
        bSelectDays.addActionListener(this);

        bNew.setToolTipText(GanttProject.getToolTip(GanttProject
                .correctLabel(language.getText("newCalendar"))));
        bDelete.setToolTipText(GanttProject.getToolTip(GanttProject
                .correctLabel(language.getText("deleteCalendar"))));
        bImport.setToolTipText(GanttProject.getToolTip(GanttProject
                .correctLabel(language.getText("importCalendar"))));
        bSelectDays.setToolTipText(GanttProject.getToolTip(GanttProject
                .correctLabel(language.getText("selectDays"))));

        String[] list = new String[20];
        list[0] = "Default";
        list[1] = "Night Calendar";
        list[2] = "Default";
        list[3] = "Night Calendar";
        list[4] = "Default";
        list[5] = "Night Calendar";
        list[6] = "Default";
        list[7] = "Night Calendar";
        list[8] = "Default";
        list[9] = "Night Calendar";
        list[10] = "Default";
        list[11] = "Night Calendar";
        list[12] = "Default";
        list[13] = "Night Calendar";
        list[14] = "Default";
        list[15] = "Night Calendar";
        list[16] = "Default";
        list[17] = "Night Calendar";
        list[18] = "Default";
        list[19] = "Night Calendar";

        listCalendar = new JList(list);
        panelDate = new GanttPanelDate(new GanttCalendar());

        JPanel panel1 = new JPanel();
        //JPanel panel2 = new JPanel();
        //JPanel panel3 = new JPanel();

        Box vb1 = Box.createVerticalBox();

        /*
         * bWork = new JRadioButton("Working Day", new
         * ImageIcon(getClass().getResource("/icons/working_day_16.gif")),
         * true); bNonWork = new JRadioButton("Working Day", new
         * ImageIcon(getClass().getResource("/icons/non_working_day_16.gif")),
         * false);
         */

        bWork = new JRadioButton("Working Day", true);
        bNonWork = new JRadioButton("Non-Working Day", false);

        /*
         * panel3.add(bWork, BorderLayout.WEST); panel3.add(bNonWork,
         * BorderLayout.EAST);
         */

        vb1.add(panelDate);
        vb1.add(bWork);
        vb1.add(bNonWork);

        // panel2.add(panelDate,BorderLayout.NORTH);
        // panel2.add(panel3,BorderLayout.SOUTH);

        panel1.add(new JScrollPane(listCalendar), BorderLayout.WEST);
        panel1.add(vb1, BorderLayout.EAST);

        JPanel p = new JPanel();
        JButton ok = new JButton(language.getText("ok"));
        getRootPane().setDefaultButton(ok);
        p.add(ok);
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                setVisible(false);
            }
        });
        JButton cancel = new JButton(language.getText("cancel"));
        p.add(cancel);
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                setVisible(false);
            }
        });
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(panel1, "Center");
        getContentPane().add(p, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        DialogAligner.center(this, getParent());
        applyComponentOrientation(language.getComponentOrientation());

    }

    /** Action listener when click the button */
    public void actionPerformed(ActionEvent e) {

    }

}
