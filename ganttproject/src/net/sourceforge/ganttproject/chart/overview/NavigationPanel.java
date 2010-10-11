package net.sourceforge.ganttproject.chart.overview;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.UIFacade;

public class NavigationPanel {
    private final TimelineChart myChart;
    private final IGanttProject myProject;

    public NavigationPanel(IGanttProject project, TimelineChart chart, UIFacade workbenchFacade) {
        myProject = project;
        myChart = chart;
    }

    public Component getComponent() {
        final Box buttonBar = Box.createHorizontalBox();
        //final JPanel buttonBar = new JPanel(new GridLayout(1, 3));
        //buttonBar.setBackground(Color.DARK_GRAY.brighter());
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 20, 0, 0),
                new LineBorder(HighlightOnMouseOver.backgroundColor, 1));
        buttonBar.setBorder(border);
        //buttonBar.add(Box.createHorizontalStrut(10));

        buttonBar.add(new PanelBorder());

        final JLabel projectStart = new JLabel("<html><b>&nbsp;Start&nbsp;</b></html>");
        projectStart.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        buttonBar.add(projectStart);
        projectStart.addMouseListener(new HighlightOnMouseOver(projectStart, buttonBar.getBackground(), new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                myChart.setStartDate(myProject.getTaskManager().getProjectStart());
                myChart.scrollRight();
            }
        }));

        buttonBar.add(new JLabel(" | "));

        final JLabel today = new JLabel("<html><b>&nbsp;Today&nbsp;</b></html>");
        today.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        buttonBar.add(today);
        today.addMouseListener(new HighlightOnMouseOver(today, buttonBar.getBackground(), new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                myChart.setStartDate(new Date());
            }
        }));

        buttonBar.add(new JLabel(" | "));

        final JLabel projectEnd = new JLabel("<html><b>&nbsp;End&nbsp;</b></html>");
        projectEnd.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        buttonBar.add(projectEnd);
        projectEnd.addMouseListener(new HighlightOnMouseOver(projectEnd, buttonBar.getBackground(), new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                final TimelineChart ganttChart = myChart;
                final Date projectEnd = myProject.getTaskManager().getProjectEnd();
                ganttChart.setStartDate(projectEnd);
                while(projectEnd.before(ganttChart.getEndDate())) {
                    ganttChart.scrollRight();
                }
                ganttChart.scrollLeft();
            }
        }));


        buttonBar.add(new PanelBorder());
        return buttonBar;
    }
}