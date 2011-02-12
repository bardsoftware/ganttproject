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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.text.MessageFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.calendar.XMLCalendarOpen.MyException;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.model.DefaultDateOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmException;
import net.sourceforge.ganttproject.task.algorithm.ShiftTaskTreeAlgorithm;

public class ProjectCalendarOptionPageProvider extends OptionPageProviderBase {
    private WeekendsSettingsPanel myWeekendsPanel;
    private DefaultDateOption myProjectStartOption;
    private JRadioButton myMoveAllTasks;
    private JRadioButton myMoveStartingTasks;
    private JLabel myMoveDurationLabel;
    private Box myMoveOptionsPanel;
    private JPanel myMoveStrategyPanelWrapper;
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

        final Date projectStart = getProject().getTaskManager().getProjectStart();
        myProjectStartOption = new DefaultDateOption("project.startDate", projectStart) {
            private TaskLength getMoveDuration() {
                return getProject().getTaskManager().createLength(
                    getProject().getTimeUnitStack().getDefaultTimeUnit(), getInitialValue(), getValue());
            }
            @Override
            public void setValue(Date value) {
                super.setValue(value);
                TaskLength moveDuration = getMoveDuration();
                if (moveDuration.getLength() != 0) {
                    updateMoveOptions(moveDuration);
                }
            }

            @Override
            public void commit() {
                super.commit();
                try {
                    moveProject(getMoveDuration());
                } catch (AlgorithmException e) {
                    getUiFacade().showErrorDialog(e);
                }
            }
        };

        myMoveOptionsPanel = Box.createVerticalBox();
        myMoveOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        OptionsPageBuilder builder = new OptionsPageBuilder();
        JComponent dateComponent = builder.createDateComponent(myProjectStartOption);
        dateComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        myMoveOptionsPanel.add(dateComponent);
        myMoveOptionsPanel.add(Box.createVerticalStrut(5));

        myMoveStrategyPanelWrapper = new JPanel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                if (isEnabled()) {
                    super.paint(g);
                    return;
                }
                BufferedImage buf = new BufferedImage(getWidth(),getHeight(), BufferedImage.TYPE_INT_RGB);
                super.paint(buf.getGraphics());
                float[] my_kernel = {
                    0.0625f, 0.125f, 0.0625f, 0.125f, 0.25f, 0.125f,
                    0.0625f, 0.125f, 0.0625f  };
                ConvolveOp op = new ConvolveOp(new Kernel(3,3, my_kernel), ConvolveOp.EDGE_NO_OP, null);
                Image img = op.filter(buf,null);
                g.drawImage(img,0,0,null);
            }
        };
        Box moveStrategyPanel = Box.createVerticalBox();
        myMoveDurationLabel = new JLabel("You will move your project by ");
        myMoveDurationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        myMoveOptionsPanel.add(Box.createVerticalStrut(3));

        myMoveAllTasks = new JRadioButton("All tasks in the project");
        myMoveAllTasks.setAlignmentX(Component.LEFT_ALIGNMENT);

        myMoveStartingTasks = new JRadioButton(
            "Tasks starting on " + GanttLanguage.getInstance().formatDate(new GanttCalendar(projectStart)));
        myMoveStartingTasks.setAlignmentX(Component.LEFT_ALIGNMENT);
        ButtonGroup moveGroup = new ButtonGroup();
        moveGroup.add(myMoveAllTasks);
        moveGroup.add(myMoveStartingTasks);
        moveGroup.setSelected(myMoveAllTasks.getModel(), true);

        moveStrategyPanel.add(myMoveDurationLabel);
        moveStrategyPanel.add(myMoveAllTasks);
        moveStrategyPanel.add(myMoveStartingTasks);

        myMoveStrategyPanelWrapper.add(moveStrategyPanel, BorderLayout.CENTER);
        myMoveOptionsPanel.add(myMoveStrategyPanelWrapper);

        UIUtil.setEnabledTree(myMoveStrategyPanelWrapper, false);
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
            UIUtil.setEnabledTree(myMoveStrategyPanelWrapper, true);
        } else {
            UIUtil.setEnabledTree(myMoveStrategyPanelWrapper, false);
        }
    }

    protected void moveProject(TaskLength moveDuration) throws AlgorithmException {
        if (myMoveAllTasks.isSelected()) {
            ShiftTaskTreeAlgorithm shiftTaskTreeAlgorithm =
                getProject().getTaskManager().getAlgorithmCollection().getShiftTaskTreeAlgorithm();
            shiftTaskTreeAlgorithm.run(
                getProject().getTaskManager().getRootTask(), moveDuration, ShiftTaskTreeAlgorithm.DEEP);
        }
    }

    @Override
    public void commit() {
        myWeekendsPanel.applyChanges(false);
        myProjectStartOption.commit();
    }
}
