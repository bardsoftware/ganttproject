/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2005-2011 GanttProject Team

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
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SpringLayout;

import biz.ganttproject.core.option.GPOptionGroup;

import net.sourceforge.ganttproject.gui.UIUtil;

/**
 * @author bard
 */
public class GPOptionChoicePanel {

  private ButtonGroup myExporterToggles;

  private AbstractButton[] myButtons;

  private JComponent[] myOptionComponents;

  private int mySelectedIndex;

  private String mySavedSelectedText;

  private OptionsPageBuilder myOptionPageBuilder = new OptionsPageBuilder();

  public JComponent getComponent(Action[] choiceChangeActions, GPOptionGroup[] choiceOptions, int selectedGroupIndex) {
    JComponent[] choiceComponents = new JComponent[choiceOptions.length];
    for (int i = 0; i < choiceChangeActions.length; i++) {
      GPOptionGroup nextOptions = choiceOptions[i];
      JComponent nextOptionComponent = nextOptions == null ? new JPanel()
          : myOptionPageBuilder.buildPlanePage(new GPOptionGroup[] { nextOptions });
      choiceComponents[i] = nextOptionComponent;
    }
    return getComponent(choiceChangeActions, choiceComponents, selectedGroupIndex);
  }

  public JComponent getComponent(Action[] choiceChangeActions, JComponent[] choiceComponents, int selectedGroupIndex) {
    myButtons = new AbstractButton[choiceChangeActions.length];
    myOptionComponents = new JComponent[choiceChangeActions.length];
    // Box result = Box.createVerticalBox();
    JPanel panelContainer = new JPanel(new SpringLayout());
    myExporterToggles = new ButtonGroup();
    for (int i = 0; i < choiceChangeActions.length; i++) {
      final int selectedIndex = i;
      final Action nextRealAction = choiceChangeActions[i];
      Action nextWrapperAction = new AbstractAction(String.valueOf(nextRealAction.getValue(Action.NAME))) {
        @Override
        public void actionPerformed(ActionEvent e) {
          nextRealAction.actionPerformed(e);
          updateSelectionUI(selectedIndex);
        }
      };
      JRadioButton nextButton = new JRadioButton(nextWrapperAction);
      nextButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
      JPanel nextExporterPanel = new JPanel(new BorderLayout());
      nextExporterPanel.add(nextButton, BorderLayout.NORTH);
      myButtons[i] = nextButton;
      myExporterToggles.add(nextButton);
      JComponent nextOptionComponent = choiceComponents[i];
      myOptionComponents[i] = nextOptionComponent;
      nextOptionComponent.setBorder(BorderFactory.createEmptyBorder(0, 30, 20, 0));
      nextExporterPanel.add(nextOptionComponent, BorderLayout.CENTER);
      setEnabledTree(nextOptionComponent, false);
      panelContainer.add(nextExporterPanel);
      // if (i == 0) {
      // nextButton.setSelected(true);
      // }
    }
    SpringUtilities.makeCompactGrid(panelContainer, myOptionComponents.length, 1, 0, 0, 5, 5);
    setSelected(selectedGroupIndex);

    JPanel result = new JPanel(new BorderLayout());
    result.add(panelContainer, BorderLayout.NORTH);
    return result;

  }

  private void updateSelectionUI(int selectedIndex) {
    AbstractButton prevSelected = myButtons[mySelectedIndex];
    prevSelected.setText(mySavedSelectedText);
    setEnabledTree(myOptionComponents[mySelectedIndex], false);
    setSelected(selectedIndex);
  }

  private void setSelected(int selectedIndex) {
    AbstractButton newSelected = myButtons[selectedIndex];
    mySavedSelectedText = newSelected.getText();
    newSelected.setText("<html><body><b><u>" + mySavedSelectedText + "</u></b></body></html>");
    mySelectedIndex = selectedIndex;
    newSelected.setSelected(true);
    setEnabledTree(myOptionComponents[mySelectedIndex], true);
  }

  private void setEnabledTree(JComponent root, boolean isEnabled) {
    UIUtil.setEnabledTree(root, isEnabled);
  }

}