/***************************************************************************
 GanttPanelDate.java  -  description
 -------------------
 begin                : dec 2002
 copyright            : (C) 2002 by Thomas Alexandre
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.time.gregorian.GregorianCalendar;

/**
 * Dialog allow you to select a date
 */
public class GanttPanelDate extends JPanel {
    private GanttLanguage language = GanttLanguage.getInstance();

    /** Graphic area for display the year */
    private GanttDialogDateDay ddd;

    /** Save the date */
    private GanttCalendar save;

    // String [] month;

    /** buttons to handles the change of date EV 20031027 * */
    private JButton jbPrevMonth, jbNextMonth;

    private JFormattedTextField jtDate;

    private JButton jbPrevYear, jbNextYear;

    /** Constructor */
    public GanttPanelDate(GanttCalendar date) {
        // super(parent, GanttLanguage.getInstance().getText("chooseDate"),
        // true);

        // setResizable(false);
        this.save = date.Clone();
        // month = date.getDayMonthLanguage();
        Box vb1 = Box.createVerticalBox();
        Box hb1 = Box.createHorizontalBox();

        jbPrevYear = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/prevyear.gif")));
        jbPrevYear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                rollDate(Calendar.YEAR, -1);
            }
        });
        jbPrevMonth = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/prevmonth.gif")));
        jbPrevMonth.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                rollDate(Calendar.MONTH, -1);
            }
        });
        jbNextMonth = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/nextmonth.gif")));
        jbNextMonth.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                rollDate(Calendar.MONTH, +1);
            }
        });
        jbNextYear = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/nextyear.gif")));
        jbNextYear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                rollDate(Calendar.YEAR, +1);
            }
        });

        jbPrevYear.setToolTipText(GanttProject.getToolTip(language
                .getText("prevYear")));
        jbPrevMonth.setToolTipText(GanttProject.getToolTip(language
                .getText("prevMonth")));
        jbNextMonth.setToolTipText(GanttProject.getToolTip(language
                .getText("nextMonth")));
        jbNextYear.setToolTipText(GanttProject.getToolTip(language
                .getText("nextYear")));

        ddd = new GanttDialogDateDay(date, language);
        jtDate = new JFormattedTextField(new SimpleDateFormat("MMM yyyy"));
        jtDate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                GregorianCalendar tmp = new GregorianCalendar();
                tmp.setTime((Date) jtDate.getValue());
                changeDate(tmp);
            }
        });
        jtDate.setValue(ddd.date.getTime());
        jtDate.setEditable(false);
        hb1.add(jbPrevYear);
        hb1.add(jbPrevMonth);
        hb1.add(jtDate);
        hb1.add(jbNextMonth);
        hb1.add(jbNextYear);

        vb1.add(hb1);
        vb1.add(ddd);

        add(vb1, "Center");
    }

    /**
     * Signified that cancel button has been clicked, cancel the selectio and
     * restur the old date
     */
    public void cancel() {
        ddd.date = save;
        changeDate(save);
    }

    /** Fill the content of the central label * */
    private void changeDate(GregorianCalendar newDate) {
        boolean differentMonth = (ddd.date.get(Calendar.MONTH) != newDate
                .get(Calendar.MONTH))
                || (ddd.date.get(Calendar.YEAR) != newDate.get(Calendar.YEAR));
        ddd.date.setTime(newDate.getTime());
        if (differentMonth) {
            jtDate.setValue(ddd.date.getTime());
            ddd.repaint();
        }
    }

    /** This function rolls the date * */
    private void rollDate(int field, int amount) {
        ddd.date.add(field, amount);
        jtDate.setValue(ddd.date.getTime());
        ddd.repaint();
    }

    /** Return The selected date. */
    public GanttCalendar getDate() {
        return ddd.date;
    }

    /**
     * Class use for display the day
     */
    public class GanttDialogDateDay extends JPanel {
        public GanttCalendar date; // The selected date

        GanttLanguage language; // The language uses

        /** Constructor */
        public GanttDialogDateDay(GanttCalendar date, GanttLanguage language) {
            this.date = date;
            this.language = language;
            MouseListener ml = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    clickFunction(e.getX(), e.getY());
                }
            };
            this.addMouseListener(ml);
        }

        /** The default size. */
        public Dimension getPreferredSize() {
            return new Dimension(230, 105); // 210
        }

        /** When the user click on the widget */
        public void clickFunction(int x, int y) {
            // Has he click on the panel???
            if (x < getWidth() / 7 * 7 && y > 15 && y < 7 * 15) {
                int X = x / (getWidth() / 7);
                int Y = (y - 15) / 15;

                // Recup the first monday
                GanttCalendar tmpdate = date.Clone();
                tmpdate.setDay(1);
                String d = tmpdate.getdayWeek();
                while (!d.equals(language.getDay(1))) {
                    tmpdate.go(Calendar.DATE, -1);
                    d = tmpdate.getdayWeek();
                }
                // Search the exact day
                for (int i = 0; i < Y * 7 + X; i++)
                    tmpdate.go(Calendar.DATE, 1);

                // Check the validity of the month
                // if(tmpdate.getMonth() == date.getMonth())
                // date = tmpdate;
                changeDate(tmpdate);
            }
            repaint();

        }

        /** draw the panel */
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            int sizex = getWidth();
            int sizey = getHeight();
            // Display the legend at top
            g.setColor(Color.white);
            g.fillRect(0, 0, sizex, sizey);
            // two colors uses
            Color gris = new Color((float) 0.827, (float) 0.827, (float) 0.827);
            Color bleu = new Color((float) 0.29, (float) 0.349, (float) 0.643);
            g.setColor(bleu);
            g.fillRect(0, 0, sizex, 15);

            String[] dayWeek = date.getDayWeekLanguage();
            g.setColor(Color.white);
            for (int i = 0; i < dayWeek.length; i++) {
                String dw = dayWeek[(i + 1) % 7];
                g.drawString(
                        dw.substring(0, dw.length() < 3 ? dw.length() : 3), i
                                * sizex / 7 + 3, 12);
            }
            GanttCalendar tmpdate = date.Clone();
            TimeZone timeZone = tmpdate.getTimeZone();
            tmpdate.setDay(1);
            String d = tmpdate.getdayWeek();
            while (!d.equals(language.getDay(1))) {
                tmpdate.go(Calendar.DATE, -1);
                d = tmpdate.getdayWeek();
            }
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 7; j++) {
                    if (tmpdate.getMonth() != date.getMonth())
                        g.setColor(gris);
                    else {
                        if (tmpdate.getDay() == date.getDay()) {
                            g.setColor(bleu);
                            g.fillRect(j * sizex / 7 - 1, 15 + i * 15,
                                    sizex / 7, 15);
                            g.setColor(Color.white);
                        } else
                            g.setColor(Color.black);
                    }
                    g.drawString("" + tmpdate.getDate(), j * sizex / 7 + 4,
                            30 + i * 15 - 3);
                    tmpdate.go(Calendar.DATE, 1);
                    if (timeZone != null
                            && timeZone.inDaylightTime(tmpdate.getTime())) {
                        tmpdate.add(Calendar.MILLISECOND, timeZone
                                .getDSTSavings());
                        timeZone = null;
                    }

                }
            }
        }
    }
}
