/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev

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
package net.sourceforge.ganttproject.client;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.NotificationSlider.AnimationView;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.DateOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultDateOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.BrowserControl;


public class RssFeedChecker {

    private final UIFacade myUiFacade;
    private final DateOption myLastCheckOption = new DefaultDateOption("updateRss.lastCheck", null);
    private final GPOptionGroup myOptionGroup = new GPOptionGroup("", new GPOption[] {myLastCheckOption});

    public RssFeedChecker(UIFacade uiFacade) {
        myUiFacade = uiFacade;
    }

    public GPOptionGroup getOptions() {
        return myOptionGroup;
    }

    public void run(final AnimationView animationHost) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Action learnMore = new GPAction("updateRss.learnMore.label") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        onLearnMore();
                    }
                };
                final Action ok = new GPAction("updateRss.yes.label") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        onYes();
                    }
                };
                final Action no = new GPAction("updateRss.no.label") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        onNo();
                    }
                };
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        showNotificationPopup(new JLabel(
                            GanttLanguage.getInstance().getText("updateRss.question")),
                            new Action[] {learnMore, ok, no}, animationHost);
                    }

                });
            }
        }).start();
    }

    private void onLearnMore() {
        BrowserControl.displayURL(GanttLanguage.getInstance().getText("updateRss.learnMore.url"));
    }

    private void onYes() {

    }

    private void onNo() {

    }

    private void showNotificationPopup(JComponent content, Action[] actions, AnimationView view) {
        myUiFacade.showNotificationPopup(content, actions, view);
    }
}
