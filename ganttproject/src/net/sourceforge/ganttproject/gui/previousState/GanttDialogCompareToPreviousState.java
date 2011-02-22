/**
 * 
 */
package net.sourceforge.ganttproject.gui.previousState;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;

import net.sourceforge.ganttproject.GanttPreviousStateTask;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.DialogAligner;
import net.sourceforge.ganttproject.gui.GanttDialogInfo;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author nbohn
 * 
 */
public class GanttDialogCompareToPreviousState extends JDialog implements
        ActionListener {
    private GanttLanguage lang = GanttLanguage.getInstance();

    private GanttCompareToPreviousStateBean compareToPreviousBean;

    private GanttProject myProject;

    public GanttDialogCompareToPreviousState(GanttProject project) {
        super(project, GanttLanguage.getInstance().getText("comparePrev"), true);
        myProject = project;
        compareToPreviousBean = new GanttCompareToPreviousStateBean(project);
        compareToPreviousBean.addActionListener(this);
        Container cp = getContentPane();
        cp.add(compareToPreviousBean, BorderLayout.CENTER);
        this.pack();
        setResizable(false);
        DialogAligner.center(this, getParent());
        applyComponentOrientation(lang.getComponentOrientation());
    }

    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() instanceof JButton) {
            JButton button = (JButton) evt.getSource();
            if (button.getName().equals("ok")) {
                if (compareToPreviousBean.getSelected() != 0) {
                    try {
                        ArrayList<GanttPreviousStateTask> tasks = myProject
                                .getBaselines()
                                .get(compareToPreviousBean.getSelected() - 1)
                                .load();
                        myProject.getArea().setPreviousStateTasks(tasks);
                        myProject.getArea().repaint();
                    } catch (Exception e) {
                    	myProject.getUIFacade().showErrorDialog(e);
                    }
                } else
                    myProject.getArea().setPreviousStateTasks(null);
                this.setVisible(false);
                dispose();

            } else if (button.getName().equals("cancel")) {
                this.setVisible(false);
                dispose();
            } else if (button.getName().equals("remove")) {
                GanttDialogInfo gdi = new GanttDialogInfo(myProject,
                        GanttDialogInfo.WARNING, GanttDialogInfo.YES_NO_OPTION,
                        lang.getText("msg25"), lang.getText("warning"));
                gdi.setVisible(true);
                if (gdi.res == GanttDialogInfo.YES) {
                    compareToPreviousBean.removeItem();

                    // this.setVisible(false);
                    // dispose();
                }
            }
        } else if (evt.getSource() instanceof JComboBox) {
            JComboBox comboBox = (JComboBox) evt.getSource();
            if (comboBox.getName().equals("nameComboBox")) {
                if (compareToPreviousBean.getSelected() == 0)
                    compareToPreviousBean.setEnabled(false);
                else
                    compareToPreviousBean.setEnabled(true);
            }
        }

    }

}
