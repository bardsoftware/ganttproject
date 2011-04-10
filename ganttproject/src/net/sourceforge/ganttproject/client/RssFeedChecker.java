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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Date;

import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import net.sourceforge.ganttproject.GPVersion;
import net.sourceforge.ganttproject.gui.NotificationChannel;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DateOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultDateOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.time.gregorian.GPTimeUnitStack;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;


public class RssFeedChecker {

    private final UIFacade myUiFacade;
    private final BooleanOption myCheckRssOption = new DefaultBooleanOption("check", true);
    private final DateOption myLastCheckOption = new DefaultDateOption("lastCheck", null);
    private final GPOptionGroup myOptionGroup =
        new GPOptionGroup("updateRss", new GPOption[] {myCheckRssOption, myLastCheckOption});
    private GPTimeUnitStack myTimeUnitStack;
    private static final String RSS_URL = "http://www.ganttproject.biz/my/feed";
    private final RssParser parser = new RssParser();

    public RssFeedChecker(GPTimeUnitStack timeUnitStack, UIFacade uiFacade) {
        myUiFacade = uiFacade;
        myTimeUnitStack = timeUnitStack;
    }

    public GPOptionGroup getOptions() {
        return myOptionGroup;
    }

    public void run() {
        Runnable command = null;
        if (!myCheckRssOption.isChecked()) {
            return;
        }
        Date lastCheck = myLastCheckOption.getValue();
        if (lastCheck == null) {
            command = createRssProposalCommand();
        } else if (!wasToday(lastCheck)) {
            command = createRssReadCommand();
        }
        if (command == null) {
            return;
        }
        new Thread(command).start();
    }

    private Runnable createRssReadCommand() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    HttpClient httpClient = new HttpClient();
                    String url = RSS_URL;
                    while (true) {
                        GetMethod getRssUrl = new GetMethod(url);
                        getRssUrl.setFollowRedirects(true);
                        getRssUrl.setRequestHeader("User-Agent", "GanttProject " + GPVersion.PRAHA);
                        int result = httpClient.executeMethod(getRssUrl);

                        if (result == HttpStatus.SC_MOVED_TEMPORARILY) {
                            url = getRssUrl.getResponseHeader("Location").getValue();
                            continue;
                        }
                        if (result == HttpStatus.SC_OK) {
                            processResponse(getRssUrl.getResponseBodyAsStream());
                        }
                        return;
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            private void processResponse(InputStream responseStream) {
                RssFeed feed = parser.parse(responseStream, myLastCheckOption.getValue());
                for (RssFeed.Item item : feed.getItems()) {
                    myUiFacade.getNotificationManager().addNotification(
                        NotificationChannel.RSS, item.title, item.body);
                }

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        myLastCheckOption.setValue(new Date());
                    }
                });

            }
        };
    }

    private Runnable createRssProposalCommand() {
        return new Runnable() {
            @Override
            public void run() {
                myUiFacade.getNotificationManager().addNotification(
                    NotificationChannel.RSS, "", GanttLanguage.getInstance().getText("updateRss.question"),
                    new HyperlinkListener() {
                        @Override
                        public void hyperlinkUpdate(HyperlinkEvent e) {
                            if (e.getEventType() != EventType.ACTIVATED) {
                                return;
                            }
                            if ("yes".equals(e.getURL().getHost())) {
                                onYes();
                            } else if ("no".equals(e.getURL().getHost())) {
                                onNo();
                            } else {
                                NotificationManager.DEFAULT_HYPERLINK_LISTENER.hyperlinkUpdate(e);
                            }
                        }
                    });
            }
        };
    }

    private boolean wasToday(Date date) {
        return myTimeUnitStack.createDuration(
            myTimeUnitStack.DAY, date, myTimeUnitStack.DAY.adjustLeft(new Date())).getLength() == 0;
    }

    private void onYes() {
        myCheckRssOption.setValue(true);
        new Thread(createRssReadCommand()).start();
    }

    private void onNo() {
        myCheckRssOption.setValue(false);
    }
}
