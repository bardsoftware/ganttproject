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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.NotificationChannel;
import net.sourceforge.ganttproject.gui.NotificationSlider.AnimationView;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DateOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultDateOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.time.gregorian.GPTimeUnitStack;
import net.sourceforge.ganttproject.util.BrowserControl;


public class RssFeedChecker {

    private final UIFacade myUiFacade;
    private final BooleanOption myCheckRssOption = new DefaultBooleanOption("updateRss.check", true);
    private final DateOption myLastCheckOption = new DefaultDateOption("updateRss.lastCheck", null);
    private final GPOptionGroup myOptionGroup =
        new GPOptionGroup("", new GPOption[] {myCheckRssOption, myLastCheckOption});
    private GPTimeUnitStack myTimeUnitStack;
    private static final String RSS_URL = "http://ganttproject-frontpage.blogspot.com/feeds/posts/default/-/clientrss";
    private final RssParser parser = new RssParser();

    public RssFeedChecker(GPTimeUnitStack timeUnitStack, UIFacade uiFacade) {
        myUiFacade = uiFacade;
        myTimeUnitStack = timeUnitStack;
    }

    public GPOptionGroup getOptions() {
        return myOptionGroup;
    }

    public void run(AnimationView animationHost) {
        Runnable command = null;
        if (!myCheckRssOption.isChecked()) {
            return;
        }
        Date lastCheck = myLastCheckOption.getValue();
        if (lastCheck == null) {
            command = createRssProposalCommand(animationHost);
        } else if (!wasToday(lastCheck)) {
            command = createRssReadCommand(animationHost);
        }
        if (command == null) {
            return;
        }
        new Thread(command).start();
    }

    private Runnable createRssReadCommand(final AnimationView animationHost) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(RSS_URL);
                    RssFeed feed = parser.parse(url.openConnection().getInputStream(), myLastCheckOption.getValue());
                    final RssFeedComponent component = new RssFeedComponent(feed);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            showNotificationPopup(component.getComponent(), component.getActions(), animationHost);
                            myLastCheckOption.setValue(new Date());
                        }
                    });
                } catch (MalformedURLException e) {

                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        };
    }


    private Runnable createRssProposalCommand(final AnimationView animationHost) {
        return new Runnable() {
            @Override
            public void run() {
                final Action learnMore = new GPAction("updateRss.learnMore.label") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        animationHost.close();
                        onLearnMore();
                    }
                };
                final Action ok = new GPAction("updateRss.yes.label") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        animationHost.close();
                        onYes(animationHost);
                    }
                };
                final Action no = new GPAction("updateRss.no.label") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        animationHost.close();
                        onNo();
                    }
                };
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        showNotificationPopup(
                            RssFeedComponent.createHtmlPane(GanttLanguage.getInstance().getText("updateRss.question")),
                            new Action[] {learnMore, ok, no}, animationHost);
                    }

                });
            }
        };
    }

    private boolean wasToday(Date date) {
        return myTimeUnitStack.createDuration(myTimeUnitStack.DAY, date, new Date()).getLength() == 0;
    }

    private void onLearnMore() {
        BrowserControl.displayURL(GanttLanguage.getInstance().getText("updateRss.learnMore.url"));
    }

    private void onYes(AnimationView animationHost) {
        myCheckRssOption.setValue(true);
        new Thread(createRssReadCommand(animationHost)).start();
    }

    private void onNo() {
        myCheckRssOption.setValue(false);
    }

    private void showNotificationPopup(JComponent content, Action[] actions, AnimationView view) {
        myUiFacade.getNotificationManager().showNotification(NotificationChannel.RSS, content, actions, view);
    }
}
