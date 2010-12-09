/*
 * Created on 02.04.2005
 */
package net.sourceforge.ganttproject.chart;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author bard
 */
public class OptionsDialogAction extends GPAction {
    private UIFacade myUIFacade;

    private GPOptionGroup[] myGroups;

    public OptionsDialogAction(GPOptionGroup[] groups, UIFacade uifacade) {
        super(GanttLanguage.getInstance().getText("chartOptions"));
        myGroups = groups;
        myUIFacade = uifacade;
        this.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource(
                "/icons/chartOptions_16.gif")));
    }

    public void actionPerformed(ActionEvent e) {
        for (int i = 0; i < myGroups.length; i++) {
            myGroups[i].lock();
        }
        final OkAction okAction = new OkAction() {
            public void actionPerformed(ActionEvent e) {
                commit();
            }
        };
        final CancelAction cancelAction = new CancelAction() {
            public void actionPerformed(ActionEvent e) {
                rollback();
            }
        };
        myUIFacade.showDialog(createDialogComponent(), new Action[] { okAction,
                cancelAction });
    }

    private void commit() {
        for (int i = 0; i < myGroups.length; i++) {
            myGroups[i].commit();
        }
    }

    private void rollback() {
        for (int i = 0; i < myGroups.length; i++) {
            myGroups[i].rollback();
        }
    }

    private Component createDialogComponent() {
        OptionsPageBuilder builder = new OptionsPageBuilder();

        JPanel combinedPanel = new JPanel(new BorderLayout());
        JComponent comp = builder.buildPage(myGroups, "ganttChart");
        combinedPanel.add(comp, BorderLayout.CENTER);
        combinedPanel.setBorder(BorderFactory.createEmptyBorder(0,0,3,0));
        return combinedPanel;
    }

	protected String getIconFilePrefix() {
		return null;
	}

	protected String getLocalizedName() {
		return getI18n("chartOptions");
	}
	
	
}
