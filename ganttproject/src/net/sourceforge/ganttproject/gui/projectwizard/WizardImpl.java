package net.sourceforge.ganttproject.gui.projectwizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.TopPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;

public abstract class WizardImpl {
    public class NextAction extends AbstractAction {
        NextAction() {
            super(GanttLanguage.getInstance().getText("next"));
        }

        public void actionPerformed(ActionEvent e) {
            WizardImpl.this.nextPage();
        }

    }

    public class BackAction extends AbstractAction {
        BackAction() {
            super(GanttLanguage.getInstance().getText("back"));
        }

        public void actionPerformed(ActionEvent e) {
            WizardImpl.this.backPage();
        }

    }

    private final ArrayList myPages = new ArrayList();

    private int myCurrentPage;

    private JPanel myPagesContainer;

    private CardLayout myCardLayout;

    private NextAction myNextAction;

    private BackAction myBackAction;

    private OkAction myOkAction;

    private UIFacade myUIFacade;

    private String myTitle;

    private CancelAction myCancelAction;

    public WizardImpl(UIFacade uiFacade, String title) {
        // super(frame, title, true);
        myUIFacade = uiFacade;
        myTitle = title;
        myCardLayout = new CardLayout();
        myPagesContainer = new JPanel(myCardLayout);
        myNextAction = new NextAction();
        myBackAction = new BackAction();
    }

    public void nextPage() {
        if (myCurrentPage < myPages.size() - 1) {
            getCurrentPage().setActive(false);
            myCurrentPage++;
            getCurrentPage().setActive(true);
            myCardLayout.next(myPagesContainer);
        }
        adjustButtonState();
    }

    public void backPage() {
        if (myCurrentPage > 0) {
            getCurrentPage().setActive(false);
            myCurrentPage--;
            getCurrentPage().setActive(true);
            myCardLayout.previous(myPagesContainer);
        }
        adjustButtonState();
    }

    public void show() {
        for (int i = 0; i < myPages.size(); i++) {
            WizardPage nextPage = (WizardPage) myPages.get(i);
            //
            JPanel pagePanel = new JPanel(new BorderLayout());
            TopPanel titlePanel = new TopPanel(nextPage.getTitle() + "   ("
                    + GanttLanguage.getInstance().getText("step") + " "
                    + (i + 1) + " " + GanttLanguage.getInstance().getText("of")
                    + " " + (myPages.size()) + ")", null);
            pagePanel.add(titlePanel, BorderLayout.NORTH);
            pagePanel.add(nextPage.getComponent());
            //
            myPagesContainer.add(pagePanel, nextPage.getTitle());
        }
        myCardLayout.first(myPagesContainer);
        myPagesContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        myOkAction = new OkAction() {
            public void actionPerformed(ActionEvent e) {
                onOkPressed();
            }
        };
        myCancelAction = new CancelAction() {
            public void actionPerformed(ActionEvent e) {
                onCancelPressed();
            }
        };
        adjustButtonState();
        myUIFacade.showDialog(myPagesContainer, new Action[] { myBackAction,
                myNextAction, myOkAction, myCancelAction }, myTitle);
    }

    public void adjustButtonState() {
        myBackAction.setEnabled(true);
        myNextAction.setEnabled(true);
        if (myCurrentPage == 0) {
            myBackAction.setEnabled(false);
        }
        if (myCurrentPage == myPages.size() - 1) {
            myNextAction.setEnabled(false);
        }
        myOkAction.setEnabled(canFinish());
    }

    protected boolean canFinish() {
        return true;
    }

    protected void addPage(WizardPage page) {
        myPages.add(page);
    }

    protected void onOkPressed() {
        getCurrentPage().setActive(false);
    }

    private void onCancelPressed() {
        getCurrentPage().setActive(false);
    }

    private WizardPage getCurrentPage() {
        return (WizardPage) myPages.get(myCurrentPage);
    }

    public UIFacade getUIFacade() {
        return myUIFacade;
    }

}
