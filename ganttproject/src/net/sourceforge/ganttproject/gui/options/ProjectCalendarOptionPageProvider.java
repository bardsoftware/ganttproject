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

import java.awt.Component;
import java.text.MessageFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import net.sourceforge.ganttproject.calendar.XMLCalendarOpen.MyException;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.model.DefaultDateOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.task.TaskLength;

public class ProjectCalendarOptionPageProvider extends OptionPageProviderBase {
    private WeekendsSettingsPanel myWeekendsPanel;
    private DefaultDateOption myProjectStartOption;
    private JRadioButton myMoveAllTasks;
    private JRadioButton myMoveStartingTasks;
    private JLabel myMoveDurationLabel;
    private Box myMoveOptionsPanel;
    public ProjectCalendarOptionPageProvider() {
        super("project.calendar");
    }
    @Override
    public GPOptionGroup[] getOptionGroups() {
        return new GPOptionGroup[0];
    }
    @Override
    public boolean hasCustomComponent() {
        return true;
    }
    @Override
    public Component buildPageComponent() {
        Box result = Box.createVerticalBox();

        myWeekendsPanel = new WeekendsSettingsPanel(getProject());
        myWeekendsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        myWeekendsPanel.initialize();
        result.add(myWeekendsPanel);

        result.add(Box.createVerticalStrut(15));

        myProjectStartOption = new DefaultDateOption(
                "project.startDate", getProject().getTaskManager().getProjectStart()) {
            @Override
            public void setValue(Date value) {
                super.setValue(value);
                TaskLength moveDuration = getProject().getTaskManager().createLength(
                    getProject().getTimeUnitStack().getDefaultTimeUnit(), getInitialValue(), value);
                if (moveDuration.getLength() != 0) {
                    updateMoveOptions(moveDuration);
                }
            }

            @Override
            public void commit() {
                // TODO Auto-generated method stub
                super.commit();
            }
        };

        myMoveOptionsPanel = Box.createVerticalBox();
        myMoveOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        OptionsPageBuilder builder = new OptionsPageBuilder();
        JComponent dateComponent = builder.createDateComponent(myProjectStartOption);
        dateComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        myMoveOptionsPanel.add(dateComponent);
        myMoveOptionsPanel.add(Box.createVerticalStrut(5));

        myMoveDurationLabel = new JLabel("You will move your project by ");
        myMoveDurationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        myMoveOptionsPanel.add(Box.createVerticalStrut(3));

        myMoveAllTasks = new JRadioButton("All tasks in the project");
        myMoveAllTasks.setAlignmentX(Component.LEFT_ALIGNMENT);
        myMoveOptionsPanel.add(Box.createVerticalStrut(2));

        myMoveStartingTasks = new JRadioButton("Just these N tasks");
        myMoveStartingTasks.setAlignmentX(Component.LEFT_ALIGNMENT);
        ButtonGroup moveGroup = new ButtonGroup();
        moveGroup.add(myMoveAllTasks);
        moveGroup.add(myMoveStartingTasks);

        myMoveOptionsPanel.add(myMoveDurationLabel);
        myMoveOptionsPanel.add(myMoveAllTasks);
        myMoveOptionsPanel.add(myMoveStartingTasks);

        UIUtil.createTitle(myMoveOptionsPanel, "Move project");
        result.add(myMoveOptionsPanel);

        return OptionPageProviderBase.wrapContentComponent(
            result, myWeekendsPanel.getTitle(), myWeekendsPanel.getComment());
    }

    protected void updateMoveOptions(TaskLength moveDuration) {
        if (moveDuration.getLength() != 0) {
            String moveLabel = MessageFormat.format("You will move by {0} {1}",
                moveDuration.getLength(),
                getProject().getTimeUnitStack().encode(moveDuration.getTimeUnit()));
            myMoveDurationLabel.setText(moveLabel);
            UIUtil.setEnabledTree(myMoveOptionsPanel, true);
        } else {
            UIUtil.setEnabledTree(myMoveOptionsPanel, false);
        }
    }

    @Override
    public void commit() {
        myWeekendsPanel.applyChanges(false);
    }
}
