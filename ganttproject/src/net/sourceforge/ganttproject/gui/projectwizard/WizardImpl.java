/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.gui.projectwizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.TopPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;

public abstract class WizardImpl {
    private final static GanttLanguage language = GanttLanguage.getInstance();

    private final ArrayList<WizardPage> myPages = new ArrayList<WizardPage>();

    private int myCurrentPage;

    private final JPanel myPagesContainer;

    private final CardLayout myCardLayout;

    private final AbstractAction myNextAction;

    private final AbstractAction myBackAction;

    private final AbstractAction myOkAction;

    private final AbstractAction myCancelAction;

    private final UIFacade myUIFacade;

    private final String myTitle;

    public WizardImpl(UIFacade uiFacade, String title) {
        // super(frame, title, true);
        myUIFacade = uiFacade;
        myTitle = title;
        myCardLayout = new CardLayout();
        myPagesContainer = new JPanel(myCardLayout);
        myNextAction = new GPAction("next") {
            public void actionPerformed(ActionEvent e) {
                WizardImpl.this.nextPage();
            }
        };
        myBackAction = new GPAction("back") {
            public void actionPerformed(ActionEvent e) {
                WizardImpl.this.backPage();
            }
        };
        myOkAction = new OkAction() {
            public void actionPerformed(ActionEvent e) {
                onOkPressed();
            }
        };
        myCancelAction = new CancelAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancelPressed();
            }
        };
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
            WizardPage nextPage = myPages.get(i);

            JPanel pagePanel = new JPanel(new BorderLayout());
            JComponent titlePanel = TopPanel.create(nextPage.getTitle() + "   (" + language.getText("step") + " "
                    + (i + 1) + " " + language.getText("of") + " " + (myPages.size()) + ")", null);
            pagePanel.add(titlePanel, BorderLayout.NORTH);
            JComponent component = (JComponent) nextPage.getComponent();
            component.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
            pagePanel.add(component);

            myPagesContainer.add(pagePanel, nextPage.getTitle());
        }
        myCardLayout.first(myPagesContainer);
        myPagesContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        adjustButtonState();
        myUIFacade.createDialog(myPagesContainer,
                new Action[] { myBackAction, myNextAction, myOkAction, myCancelAction }, myTitle).show();
    }

    public void adjustButtonState() {
        myBackAction.setEnabled(myCurrentPage > 0);
        myNextAction.setEnabled(myCurrentPage < myPages.size() - 1);
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
        return myPages.get(myCurrentPage);
    }

    public UIFacade getUIFacade() {
        return myUIFacade;
    }
}
