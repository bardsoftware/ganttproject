/**
 * 
 */
package net.sourceforge.ganttproject.gui.previousState;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import net.sourceforge.ganttproject.GanttPreviousState;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author nbohn
 * 
 */
public class GanttSaveAsPreviousStateBean extends JPanel {
    private JPanel southPanel;

    private JButton okButton, cancelButton;

    private JTextField nameTextField;

    private JLabel previousStateLabel;

    private JComboBox nameComboBox;

    private List<GanttPreviousState> myPreviousStates;

    private GanttLanguage lang = GanttLanguage.getInstance();

    private static String defaultName = GanttLanguage.getInstance().getText(
            "save");

    private static int count = 1;

    public GanttSaveAsPreviousStateBean(GanttProject project) {
        myPreviousStates = project.getBaselines();
        if (myPreviousStates.size() == 0) {
            defaultName = GanttLanguage.getInstance().getText("save");
            count = 1;
        }
        init();
    }

    public void init() {
        setLayout(new BorderLayout());
        add(constructNamePanel(), BorderLayout.CENTER);
        add(constructSouthPanel(), BorderLayout.SOUTH);
        nameTextField.requestFocusInWindow();
    }

    /** Construct the south panel */

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

    private Box constructNamePanel() {
        Box box = Box.createVerticalBox();
        JPanel previousStatePanel;
        nameComboBox = new JComboBox();
        if (myPreviousStates.size() != 0) {
            previousStatePanel = new JPanel(new FlowLayout(FlowLayout.TRAILING,
                    30, 20));
            previousStateLabel = new JLabel(lang.getText("previousStates"));
            for (int i = 0; i < myPreviousStates.size(); i++)
                nameComboBox.addItem(myPreviousStates
                        .get(i).getName());
            nameComboBox.setName("nameComboBox");
            nameComboBox.setSelectedIndex(-1);
            previousStatePanel.add(previousStateLabel);
            previousStatePanel.add(nameComboBox);
            box.add(previousStatePanel);
        }
        nameTextField = new JTextField(20);
        nameTextField.setText(defaultName + "_" + count);
        nameTextField.selectAll();
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 30,
                20));
        namePanel.add(nameTextField);
        box.add(namePanel);
        box
                .setBorder(new TitledBorder(new EtchedBorder(), lang
                        .getText("name")));
        return box;
    }

    public String getName() {
        return nameTextField.getText();
    }

    public void addActionListener(ActionListener l) {

        okButton.addActionListener(l);

        cancelButton.addActionListener(l);

        nameComboBox.addActionListener(l);
    }

    public JTextField getTextField() {
        return nameTextField;
    }

    public void setDefaultName() {
        if (!nameTextField.getText().equals(defaultName + "_" + count)) {
            defaultName = nameTextField.getText();
            count = 0;
        }
        count++;
    }
}
