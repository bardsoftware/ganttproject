package net.sourceforge.ganttproject.chart.overview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.TaskLength;

public class NavigationPanel {
    private final TimelineChart myChart;
    private final IGanttProject myProject;

    public NavigationPanel(IGanttProject project, TimelineChart chart, UIFacade workbenchFacade) {
        myProject = project;
        myChart = chart;
    }

    public Component getComponent() {
        final JToolBar buttonBar = new JToolBar();
        buttonBar.setFloatable(false);
        buttonBar.setBackground(new Color(0.93f, 0.93f, 0.93f));
        buttonBar.setBorderPainted(false);

        final JButton projectStart = new JButton("<html><b>&nbsp;Start&nbsp;</b></html>");
        projectStart.setMargin(new Insets(0, 0, 0, 0));
        projectStart.setBorderPainted(false);
        buttonBar.add(projectStart);
        projectStart.addMouseListener(new HighlightOnMouseOver(projectStart, new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                myChart.setStartDate(myProject.getTaskManager().getProjectStart());
                myChart.scrollBy(createTimeInterval(-1));
            }
        }));

        buttonBar.add(new JLabel(" | "));

        final JButton today = new JButton("<html><b>&nbsp;Today&nbsp;</b></html>");
        today.setMargin(new Insets(0, 0, 0, 0));
        today.setBorderPainted(false);
        buttonBar.add(today);
        today.addMouseListener(new HighlightOnMouseOver(today, new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                myChart.setStartDate(new Date());
            }
        }));

        buttonBar.add(new JLabel(" | "));

        final JButton projectEnd = new JButton("<html><b>&nbsp;End&nbsp;</b></html>");
        projectEnd.setMargin(new Insets(0, 0, 0, 0));
        projectEnd.setBorderPainted(false);
        buttonBar.add(projectEnd);
        projectEnd.addMouseListener(new HighlightOnMouseOver(projectEnd, new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                final TimelineChart ganttChart = myChart;
                final Date projectEnd = myProject.getTaskManager().getProjectEnd();
                ganttChart.setStartDate(projectEnd);
                while(projectEnd.before(ganttChart.getEndDate())) {
                    ganttChart.scrollBy(createTimeInterval(-1));
                }
                ganttChart.scrollBy(createTimeInterval(1));
            }
        }));

        return buttonBar;
    }

	protected TaskLength createTimeInterval(int i) {
		return myProject.getTaskManager().createLength(i);
	}
}