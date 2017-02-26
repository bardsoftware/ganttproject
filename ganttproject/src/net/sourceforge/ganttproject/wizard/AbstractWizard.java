/*
Copyright (C) 2013 BarD Software s.r.o

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
package net.sourceforge.ganttproject.wizard;

import com.google.common.collect.Maps;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIFacade.Centering;
import net.sourceforge.ganttproject.gui.UIFacade.Dialog;
import net.sourceforge.ganttproject.gui.options.TopPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A wizard abstraction capable of managing wizard pages and showing them in the UI
 * according to the user actions.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class AbstractWizard {
  private final ArrayList<WizardPage> myPages = new ArrayList<WizardPage>();

  private final Map<String, JComponent> myTitle2component = Maps.newHashMap();

  private int myCurrentPage;

  private final JPanel myPagesContainer;

  private final CardLayout myCardLayout;

  private final AbstractAction myNextAction;

  private final AbstractAction myBackAction;

  private final AbstractAction myOkAction;

  private final AbstractAction myCancelAction;

  private final UIFacade myUIFacade;

  private final String myTitle;

  private final Dialog myDialog;

  private Runnable myOkRunnable;

  public AbstractWizard(UIFacade uiFacade, String title, WizardPage firstPage) {
    myUIFacade = uiFacade;
    myTitle = title;
    myCardLayout = new CardLayout();
    myPagesContainer = new JPanel(myCardLayout);
    myNextAction = new GPAction("next") {
      @Override
      public void actionPerformed(ActionEvent e) {
        AbstractWizard.this.nextPage();
      }
    };
    myBackAction = new GPAction("back") {
      @Override
      public void actionPerformed(ActionEvent e) {
        AbstractWizard.this.backPage();
      }
    };
    myOkAction = new OkAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onOkPressed();
      }
    };
    myCancelAction = new CancelAction();
    myPagesContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    myDialog = myUIFacade.createDialog(myPagesContainer, new Action[] { myBackAction, myNextAction, myOkAction,
        myCancelAction }, myTitle);
    addPageComponent(firstPage);
    myPages.add(firstPage);
    myDialog.layout();
    myDialog.center(Centering.WINDOW);
    adjustButtonState();
  }

  private void nextPage() {
    assert myCurrentPage + 1 < myPages.size() : "It is a bug: we have no next page while Next button is enabled and has been pressed";
    getCurrentPage().setActive(null);
    WizardPage nextPage = myPages.get(myCurrentPage + 1);
    if (myTitle2component.get(nextPage.getTitle()) == null) {
      addPageComponent(nextPage);
    }
    myCurrentPage++;
    nextPage.setActive(this);
    myCardLayout.show(myPagesContainer, nextPage.getTitle());
    myDialog.center(Centering.WINDOW);
    myDialog.layout();
    adjustButtonState();
  }

  private void addPageComponent(WizardPage page) {
    if (myTitle2component.get(page.getTitle()) == null) {
      JComponent c = wrapePageComponent(page.getTitle(), page.getComponent());
      myPagesContainer.add(c, page.getTitle());
      myTitle2component.put(page.getTitle(), c);
    }
  }

  private JComponent wrapePageComponent(String title, JComponent c) {
    JPanel pagePanel = new JPanel(new BorderLayout());
    JComponent titlePanel = TopPanel.create(title, null);
    pagePanel.add(titlePanel, BorderLayout.NORTH);
    c.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
    pagePanel.add(c, BorderLayout.CENTER);
    return pagePanel;
  }

  private void backPage() {
    if (myCurrentPage > 0) {
      getCurrentPage().setActive(null);
      myCurrentPage--;
      myCardLayout.show(myPagesContainer, getCurrentPage().getTitle());
      getCurrentPage().setActive(this);
    }
    //myDialog.center(Centering.WINDOW);
    adjustButtonState();
  }

  public void show() {
    myCardLayout.first(myPagesContainer);
    getCurrentPage().setActive(this);
    adjustButtonState();
    myDialog.center(Centering.SCREEN);
    myDialog.show();
  }

  private void adjustButtonState() {
    myBackAction.setEnabled(myCurrentPage > 0);
    myNextAction.setEnabled(myCurrentPage < myPages.size() - 1);
    myOkAction.setEnabled(canFinish());
  }

  protected void onOkPressed() {
    myOkRunnable.run();
  }

  protected boolean canFinish() {
    return myOkRunnable != null;
  }

  private boolean isExistingNextPage(WizardPage page) {
    if (page == null) {
      return false;
    }
    int idxPage = myPages.indexOf(page);
    return (idxPage != -1 && myCurrentPage == idxPage - 1);
  }

  /**
   * Active wizard page can call this method to set a next page.
   *
   * @param page next page
   */
  public void setNextPage(WizardPage page) {
    boolean isExisting = isExistingNextPage(page);
    if (!isExisting) {
      List<WizardPage> tail = myPages.subList(myCurrentPage + 1, myPages.size());
      for (WizardPage tailPage : tail) {
        JComponent component = myTitle2component.remove(tailPage.getTitle());
        if (component != null) {
          myPagesContainer.remove(component);
        }
      }
      tail.clear();
      if (page != null) {
        myPages.add(page);
      }
    }
    adjustButtonState();
  }

  /**
   * Wizard pages or specific wizard implementations can call this method to set an
   * action to be called when user clicks OK. This makes OK button enabled.
   *
   * @param action action to be called on OK
   */
  public void setOkAction(Runnable action) {
    myOkRunnable = action;
    adjustButtonState();
  }

  private WizardPage getCurrentPage() {
    return myPages.get(myCurrentPage);
  }

  public UIFacade getUIFacade() {
    return myUIFacade;
  }

  public Dialog getDialog() {
    return myDialog;
  }
}
