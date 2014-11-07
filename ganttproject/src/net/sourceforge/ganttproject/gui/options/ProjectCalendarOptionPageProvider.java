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
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import biz.ganttproject.core.option.DefaultDateOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.TimeDuration;

import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmException;
import net.sourceforge.ganttproject.task.algorithm.ShiftTaskTreeAlgorithm;

/**
 * Provides project calendar settings page in the settings dialog.
 *
 * @author Dmitry Barashev
 */
public class ProjectCalendarOptionPageProvider extends OptionPageProviderBase {
  private WeekendsSettingsPanel myWeekendsPanel;
  private DefaultDateOption myProjectStartOption;
  private JRadioButton myMoveAllTasks;
  private JRadioButton myMoveStartingTasks;
  private JLabel myMoveDurationLabel;
  private Box myMoveOptionsPanel;
  private JPanel myMoveStrategyPanelWrapper;
  private Date myProjectStart;

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
    final GanttLanguage i18n = GanttLanguage.getInstance();
    final Box result = Box.createVerticalBox();

    myWeekendsPanel = new WeekendsSettingsPanel(getProject(), getUiFacade());
    myWeekendsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    myWeekendsPanel.initialize();
    result.add(myWeekendsPanel);

    result.add(Box.createVerticalStrut(15));

    myProjectStart = getProject().getTaskManager().getProjectStart();
    myProjectStartOption = new DefaultDateOption("project.startDate", myProjectStart) {
      private TimeDuration getMoveDuration() {
        return getProject().getTaskManager().createLength(getProject().getTimeUnitStack().getDefaultTimeUnit(),
            getInitialValue(), getValue());
      }

      @Override
      public void setValue(Date value) {
        super.setValue(value);
        TimeDuration moveDuration = getMoveDuration();
        if (moveDuration.getLength() != 0) {
          updateMoveOptions(moveDuration);
        }
      }

      @Override
      public void commit() {
        super.commit();
        if (!isChanged()) {
          return;
        }
        try {
          moveProject(getMoveDuration());
        } catch (AlgorithmException e) {
          getUiFacade().showErrorDialog(e);
        }
      }
    };

    myMoveOptionsPanel = Box.createVerticalBox();
    myMoveOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    Box dateComponent = Box.createHorizontalBox();
    OptionsPageBuilder builder = new OptionsPageBuilder();
    dateComponent.add(new JLabel(i18n.getText(builder.getI18N().getCanonicalOptionLabelKey(myProjectStartOption))));
    dateComponent.add(Box.createHorizontalStrut(3));
    dateComponent.add(builder.createDateComponent(myProjectStartOption));
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
        final BufferedImage buf = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        super.paint(buf.getGraphics());
        final float[] my_kernel = { 0.0625f, 0.125f, 0.0625f, 0.125f, 0.25f, 0.125f, 0.0625f, 0.125f, 0.0625f };
        final ConvolveOp op = new ConvolveOp(new Kernel(3, 3, my_kernel), ConvolveOp.EDGE_NO_OP, null);
        Image img = op.filter(buf, null);
        g.drawImage(img, 0, 0, null);
      }
    };
    myMoveStrategyPanelWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

    myMoveAllTasks = new JRadioButton(i18n.getText("project.calendar.moveAll.label"));
    myMoveAllTasks.setAlignmentX(Component.LEFT_ALIGNMENT);

    myMoveStartingTasks = new JRadioButton(MessageFormat.format(i18n.getText("project.calendar.moveSome.label"),
        i18n.formatDate(CalendarFactory.createGanttCalendar(myProjectStart))));
    myMoveStartingTasks.setAlignmentX(Component.LEFT_ALIGNMENT);

    ButtonGroup moveGroup = new ButtonGroup();
    moveGroup.add(myMoveAllTasks);
    moveGroup.add(myMoveStartingTasks);
    moveGroup.setSelected(myMoveAllTasks.getModel(), true);

    Box moveStrategyPanel = Box.createVerticalBox();
    myMoveDurationLabel = new JLabel();
    myMoveDurationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    moveStrategyPanel.add(myMoveDurationLabel);
    moveStrategyPanel.add(myMoveAllTasks);
    moveStrategyPanel.add(myMoveStartingTasks);

    myMoveStrategyPanelWrapper.add(moveStrategyPanel, BorderLayout.CENTER);
    myMoveOptionsPanel.add(Box.createVerticalStrut(3));
    myMoveOptionsPanel.add(myMoveStrategyPanelWrapper);

    UIUtil.createTitle(myMoveOptionsPanel, i18n.getText("project.calendar.move.title"));
    result.add(myMoveOptionsPanel);

    updateMoveOptions(getProject().getTaskManager().createLength(0));
    return OptionPageProviderBase.wrapContentComponent(result, getCanonicalPageTitle(), null);
  }

  protected void updateMoveOptions(TimeDuration moveDuration) {
    if (moveDuration.getLength() != 0) {
      String moveLabel = MessageFormat.format(
          GanttLanguage.getInstance().getText("project.calendar.moveDuration.label"), moveDuration.getLength(),
          getProject().getTimeUnitStack().encode(moveDuration.getTimeUnit()));
      myMoveDurationLabel.setText(moveLabel);
      UIUtil.setEnabledTree(myMoveStrategyPanelWrapper, true);
    } else {
      UIUtil.setEnabledTree(myMoveStrategyPanelWrapper, false);
    }
  }

  protected void moveProject(TimeDuration moveDuration) throws AlgorithmException {
    TaskManager taskManager = getProject().getTaskManager();
    ShiftTaskTreeAlgorithm shiftTaskTreeAlgorithm = taskManager.getAlgorithmCollection().getShiftTaskTreeAlgorithm();
    if (myMoveAllTasks.isSelected()) {
      shiftTaskTreeAlgorithm.run(taskManager.getRootTask(), moveDuration, ShiftTaskTreeAlgorithm.DEEP);
    } else if (myMoveStartingTasks.isSelected()) {
      List<Task> moveScope = new ArrayList<Task>();
      TaskContainmentHierarchyFacade taskTree = taskManager.getTaskHierarchy();
      for (Task t : taskManager.getTasks()) {
        if (t.getStart().getTime().equals(myProjectStart) && !taskTree.hasNestedTasks(t)) {
          moveScope.add(t);
        }
      }
      shiftTaskTreeAlgorithm.run(moveScope, moveDuration, ShiftTaskTreeAlgorithm.SHALLOW);
    }
  }

  @Override
  public void commit() {
    myWeekendsPanel.applyChanges(false);
    myProjectStartOption.commit();
  }
}
