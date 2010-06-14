package net.sourceforge.ganttproject.gui.taskproperties;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.StartFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.StartStartConstraintImpl;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class TaskDependenciesPanel extends CommonPanel {
    protected GanttLanguage language = GanttLanguage.getInstance(); // language

    // the panel
    // will
    // display

    private GridBagConstraints gbc = new GridBagConstraints();

    private JPanel predecessorsPanel;

    private JScrollPane predecessorsScrollPane; // second row, a table

    private JTable predecessorsTable;

    private final TaskManager myTaskManager;

    private DependencyTableModel myTableModel;

    public TaskDependenciesPanel(Task task) {
        super(task);
        myTaskManager = task.getManager();

    }

    public JPanel getComponent() {
        if (predecessorsPanel == null) {
            constructPredecessorsPanel();
        }
        return predecessorsPanel;
    }

    public DependencyTableModel getTableModel() {
        return myTableModel;
    }

    protected void constructPredecessorsPanel() {

        predecessorsPanel = new JPanel(new GridBagLayout());

        myTableModel = new DependencyTableModel(getTask());

        predecessorsTable = new JTable(myTableModel);

        predecessorsTable.setPreferredScrollableViewportSize(new Dimension(500,
                130));

        setUpPredecessorComboColumn(predecessorsTable.getColumnModel()
                .getColumn(1), predecessorsTable); // set column editor

        setUpTypeComboColumn(predecessorsTable.getColumnModel().getColumn(2)); // set
        // column
        // editor
        setUpHardnessColumnEditor(predecessorsTable.getColumnModel().getColumn(4));
        predecessorsTable.setRowHeight(23); // set row height

        predecessorsTable.getColumnModel().getColumn(0).setPreferredWidth(10); // set
        // column
        // size

        predecessorsTable.getColumnModel().getColumn(1).setPreferredWidth(200);

        predecessorsTable.getColumnModel().getColumn(2).setPreferredWidth(60);

        predecessorsTable.getColumnModel().getColumn(3).setPreferredWidth(40);

        predecessorsScrollPane = new JScrollPane(predecessorsTable);

        JPanel secondPredecessorsPanel = new JPanel();
        secondPredecessorsPanel.setBorder(new TitledBorder(new EtchedBorder(),
                language.getText("predecessors")));
        secondPredecessorsPanel.add(predecessorsScrollPane);

        JButton bremove = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/delete_16.gif")));
        bremove.setToolTipText(GanttProject.getToolTip(language
                .getText("removeRelationShip")));
        bremove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int[] selectedRow = predecessorsTable.getSelectedRows();
                for (int i = 0; i < selectedRow.length; ++i) {
                    predecessorsTable.getModel().setValueAt(null,
                            selectedRow[i], 1);
                }
            }
        });

        secondPredecessorsPanel.add(bremove);

        gbc.anchor = GridBagConstraints.WEST;

        gbc.insets.right = 15;

        gbc.insets.left = 10;

        gbc.insets.top = 10;

        gbc.weighty = 0;

        addUsingGBL(predecessorsPanel, setupCommonFields(true), gbc, 0, 0, 1, 1);

        gbc.gridx = 0;

        gbc.gridy = 1;

        gbc.gridwidth = 8;

        gbc.gridheight = 1;

        gbc.weighty = 1;

        predecessorsPanel.add(secondPredecessorsPanel, gbc);
    }

    protected void setUpPredecessorComboColumn(TableColumn predecessorColumn,
            final JTable predecessorTable) {
        // Set up the editor for the sport cells.
        final JComboBox comboBox = new JComboBox();
        Task[] possiblePredecessors = myTaskManager.getAlgorithmCollection()
                .getFindPossibleDependeesAlgorithm().run(getTask());
        for (int i = 0; i < possiblePredecessors.length; i++) {
            Task next = possiblePredecessors[i];
            comboBox.addItem(new DependencyTableModel.TaskComboItem(next));

        }

        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (predecessorTable.getEditingRow() != -1) {
                    DependencyTableModel.TaskComboItem selectedItem = (DependencyTableModel.TaskComboItem) comboBox
                            .getSelectedItem();
                    if (selectedItem != null) {
                        predecessorTable.setValueAt(selectedItem,
                                predecessorTable.getEditingRow(), 0);
                        predecessorTable.setValueAt(CONSTRAINTS[0],
                                predecessorTable.getEditingRow(), 2);
                        // predecessorTable.setValueAt(0+"",
                        // predecessorTable.getEditingRow(), 3);
                    }
                }
            }
        });
        comboBox.setEditable(false);
        predecessorColumn.setCellEditor(new DefaultCellEditor(comboBox));
    }

    private void setUpTypeComboColumn(TableColumn typeColumn) {
        // Set up the editor for the sport cells.
        DefaultComboBoxModel model = new DefaultComboBoxModel(CONSTRAINTS);
        JComboBox comboBox = new JComboBox(model);
        comboBox.setSelectedIndex(0);
        comboBox.setEditable(false);
        typeColumn.setCellEditor(new DefaultCellEditor(comboBox));
    }

    private void setUpHardnessColumnEditor(TableColumn hardnessColumn) {
        DefaultComboBoxModel model = new DefaultComboBoxModel(HARDNESS);
        JComboBox comboBox = new JComboBox(model);
        comboBox.setSelectedIndex(0);
        comboBox.setEditable(false);
        hardnessColumn.setCellEditor(new DefaultCellEditor(comboBox));    	
    }
    public JTable getTable() {
        return predecessorsTable;
    }

    private static TaskDependencyConstraint[] CONSTRAINTS = new TaskDependencyConstraint[] {
            new FinishStartConstraintImpl(), new FinishFinishConstraintImpl(),
            new StartFinishConstraintImpl(), new StartStartConstraintImpl() };

    private static TaskDependency.Hardness[] HARDNESS = new TaskDependency.Hardness[] {
    	TaskDependency.Hardness.STRONG, TaskDependency.Hardness.RUBBER
    };
}
