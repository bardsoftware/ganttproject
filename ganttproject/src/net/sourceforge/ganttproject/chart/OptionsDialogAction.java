/*
 * Created on 02.04.2005
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;

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
            @Override
            public void actionPerformed(ActionEvent e) {
                rollback();
            }
        };
        myUIFacade.createDialog(createDialogComponent(), new Action[] { okAction, cancelAction }, "").show();
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
        JComponent comp = builder.buildPage(myGroups, "ganttChart");
        comp.setBorder(new EmptyBorder(5, 5, 5, 5));
        return comp;
    }

    @Override
    protected String getIconFilePrefix() {
        return null;
    }

    @Override
    protected String getLocalizedName() {
        return getI18n("chartOptions");
    }


}
