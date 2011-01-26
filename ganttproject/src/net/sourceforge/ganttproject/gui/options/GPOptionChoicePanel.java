/*
 * Created on 18.05.2005
 */
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;

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

    public Component getComponent(Action[] choiceChangeActions,
            GPOptionGroup[] choiceOptions, int selectedGroupIndex) {
        JComponent[] choiceComponents = new JComponent[choiceOptions.length];
        for (int i = 0; i < choiceChangeActions.length; i++) {
            GPOptionGroup nextOptions = choiceOptions[i];
            JComponent nextOptionComponent = nextOptions == null ? new JPanel()
                    : myOptionPageBuilder
                            .buildPlanePage(new GPOptionGroup[] { nextOptions });
            choiceComponents[i] = nextOptionComponent;
        }
        return getComponent(choiceChangeActions, choiceComponents, selectedGroupIndex);
    }

    public JComponent getComponent(Action[] choiceChangeActions, JComponent[] choiceComponents, int selectedGroupIndex) {
        myButtons = new AbstractButton[choiceChangeActions.length];
        myOptionComponents = new JComponent[choiceChangeActions.length];
        Box result = Box.createVerticalBox();
        myExporterToggles = new ButtonGroup();
        for (int i = 0; i < choiceChangeActions.length; i++) {
            final int selectedIndex = i;
            final Action nextRealAction = choiceChangeActions[i];
            Action nextWrapperAction = new AbstractAction(String
                    .valueOf(nextRealAction.getValue(Action.NAME))) {
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
            nextOptionComponent.setBorder(BorderFactory.createEmptyBorder(
                    0, 30, 20, 0));
            nextExporterPanel.add(nextOptionComponent, BorderLayout.CENTER);
            setEnabledTree(nextOptionComponent, false);
            result.add(nextExporterPanel);
//            if (i == 0) {
//                nextButton.setSelected(true);
//            }
        }
        setSelected(selectedGroupIndex);
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
        newSelected.setText("<html><body><b><u>" + mySavedSelectedText
                + "</u></b></body></html>");
        mySelectedIndex = selectedIndex;
        newSelected.setSelected(true);
        setEnabledTree(myOptionComponents[mySelectedIndex], true);
    }

    private void setEnabledTree(JComponent root, boolean isEnabled) {
    	UIUtil.setEnabledTree(root, isEnabled);
    }

}