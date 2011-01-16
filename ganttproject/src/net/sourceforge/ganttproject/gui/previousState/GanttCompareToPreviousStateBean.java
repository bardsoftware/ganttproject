/**
 * 
 */
package net.sourceforge.ganttproject.gui.previousState;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import net.sourceforge.ganttproject.GanttPreviousState;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author nbohn
 * 
 */
public class GanttCompareToPreviousStateBean extends JPanel {
    private GanttLanguage lang = GanttLanguage.getInstance();

    private List<GanttPreviousState> myPreviousStates;

    private JPanel southPanel;

    private JButton okButton, cancelButton, removeButton;

    private JComboBox nameComboBox;

    public GanttCompareToPreviousStateBean(GanttProject project) {
        myPreviousStates = project.getBaselines();
        init();
    }

    public void init() {
        setLayout(new BorderLayout());
        add(constructGeneralPanel(), BorderLayout.NORTH);
        add(constructSouthPanel(), BorderLayout.SOUTH);
    }

    private JPanel constructSouthPanel() {
        okButton = new JButton(lang.getText("ok"));

        okButton.setName("ok");

        if (getRootPane() != null)
            getRootPane().setDefaultButton(okButton); // set ok the defuault
        // button when press
        // "enter" --> check
        // because
        // getRootPane()==null
        // !!!

        cancelButton = new JButton(lang.getText("cancel"));

        cancelButton.setName("cancel");

        southPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 10));

        southPanel.add(okButton);

        southPanel.add(cancelButton);

        return southPanel;
    }

    private JPanel constructGeneralPanel() {
        JPanel generalPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING,
                40, 20));
        nameComboBox = new JComboBox();
        nameComboBox.setName("nameComboBox");
        nameComboBox.addItem(lang.getText("none"));
        for (int i = 0; i < myPreviousStates.size(); i++)
            nameComboBox.addItem(myPreviousStates.get(i)
                    .getName());
        nameComboBox.setSelectedIndex(myPreviousStates.size());
        nameComboBox.setName("nameComboBox");
        removeButton = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/delete_16.gif")));
        removeButton.setToolTipText(GanttProject.getToolTip(lang
                .getText("delete")));
        removeButton.setName("remove");
        generalPanel.add(nameComboBox);
        generalPanel.add(removeButton);
        generalPanel.setBorder(new TitledBorder(new EtchedBorder(), lang
                .getText("previousStates")));
        return generalPanel;
    }

    public void addActionListener(ActionListener l) {

        okButton.addActionListener(l);

        cancelButton.addActionListener(l);

        removeButton.addActionListener(l);

        nameComboBox.addActionListener(l);

    }

    public int getSelected() {
        return nameComboBox.getSelectedIndex();
    }

    public void removeItem() {
        int index = nameComboBox.getSelectedIndex();
        nameComboBox.removeItemAt(index);
        myPreviousStates.get(index - 1).remove();
        myPreviousStates.remove(index - 1);
    }

    public void setEnabled(boolean b) {
        removeButton.setEnabled(b);
    }
}
