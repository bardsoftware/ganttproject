/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev

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
package net.sourceforge.ganttproject.client;

import biz.ganttproject.app.InternationalizationKt;
import biz.ganttproject.core.option.DateOption;
import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.option.DefaultDateOption;
import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.storage.cloud.HttpClientOk;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.gui.NotificationChannel;
import net.sourceforge.ganttproject.gui.NotificationItem;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.UIFacade;
import org.apache.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Checks GanttProject RSS news feeds once per day
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class RssFeedChecker {
  protected static final int MAX_ATTEMPTS = 10;
  private static final String RSS_HOST = "www.ganttproject.biz";
  private static final String RSS_PATH = "/my/feed";

  private enum CheckOption {
    YES, NO, UNDEFINED;
  }

  private final UIFacade myUiFacade;
  private final DefaultEnumerationOption<CheckOption> myCheckRssOption = new DefaultEnumerationOption<CheckOption>(
      "check", CheckOption.values()) {
    @Override
    protected String objectToString(CheckOption obj) {
      return obj.toString();
    }

    @Override
    protected CheckOption stringToObject(String value) {
      return CheckOption.valueOf(value);
    }
  };
  private final DefaultBooleanOption myBooleanCheckRssOption = new DefaultBooleanOption("rss.checkUpdates") {
    @Override
    public String getPersistentValue() {
      return null;
    }
  };

  private final DateOption myLastCheckOption = new DefaultDateOption("lastCheck", null);
  private final Executor httpExecutor = Executors.newSingleThreadExecutor();

  // This is what we save in the settings file: three-state decision option and the time of the last check.
  private final GPOptionGroup myOptionGroup = new GPOptionGroup("updateRss",
      myCheckRssOption, myLastCheckOption);

  // This is what we show to the user: boolean check/no check option.
  private final GPOptionGroup myUiOptionGroup = new GPOptionGroup("rss", myBooleanCheckRssOption);
  private final RssParser parser = new RssParser();

  public RssFeedChecker(UIFacade uiFacade) {
    myCheckRssOption.setValue(CheckOption.UNDEFINED.toString());
    myUiFacade = uiFacade;
    myBooleanCheckRssOption.setValue(CheckOption.YES.equals(myCheckRssOption.getSelectedValue()));

    // When user changes the option in the UI, we set a certain value of the three-state option.
    myBooleanCheckRssOption.addChangeValueListener(event -> {
      if (event.getTriggerID() != RssFeedChecker.this) {
        if (myBooleanCheckRssOption.isChecked()) {
          myCheckRssOption.setValue(CheckOption.YES.name(), RssFeedChecker.this);
        } else {
          myCheckRssOption.setValue(CheckOption.NO.name(), RssFeedChecker.this);
        }
      }
    });
    // When we read a stored three-state value from the settings file, we set the user-visible boolean value appropriately.
    myCheckRssOption.addChangeValueListener(event -> {
      if (event.getID() != RssFeedChecker.this && CheckOption.UNDEFINED != myCheckRssOption.getSelectedValue()) {
        myBooleanCheckRssOption.setValue(CheckOption.YES == myCheckRssOption.getSelectedValue(), RssFeedChecker.this);
      }
    });
  }

  private NotificationManager getNotificationManager() {
    return myUiFacade.getNotificationManager();
  }

  public GPOptionGroup getOptions() {
    return myOptionGroup;
  }

  public GPOptionGroup getUiOptions() {
    return myUiOptionGroup;
  }

  public void run() {
    CheckOption checkOption = CheckOption.valueOf(myCheckRssOption.getValue());
    if (CheckOption.NO == checkOption) {
      return;
    }
    Date lastCheck = myLastCheckOption.getValue();
    if (lastCheck == null) {
      // It is the first time we run, just mark it. We want to suggest
      // subscribing to updates only to
      // those who runs GP at least twice.
      markLastCheck();
      return;
    }
    if (wasToday(lastCheck)) {
      // It is not the first run of GP but it was last run today and RSS
      // proposal has not been shown yet.
      return;
    }
    // So it is not the first time and even not the first day we start GP.
    // If no decision about subscribing, let's proactively suggest it,
    // otherwise run check RSS.
    if (CheckOption.UNDEFINED == checkOption) {
//      myCheckRssOption.setValue(CheckOption.YES.toString());
      getNotificationManager().addNotifications(Collections.singletonList(getNotificationManager().createNotification(
        NotificationChannel.RSS, "News and Updates", InternationalizationKt.getRootLocalizer().formatText("updateRss.message"), NotificationManager.DEFAULT_HYPERLINK_LISTENER
      )));
    } else {
      readRss();
    }
  }

  private void readRss() {
    var cmd = new Runnable() {
      @Override
      public void run() {
        GPLogger.log("Starting RSS check...");
        var httpClientOk = new HttpClientOk(RSS_HOST, "", "", ()->"");
        try {
          for (int i = 0; i < MAX_ATTEMPTS; i++) {
            var resp = httpClientOk.sendGet(RSS_PATH, Collections.emptyMap());
            if (resp.getCode() == HttpStatus.SC_OK) {
              processResponse(new ByteArrayInputStream(resp.getRawBody()));
              return;
            }
          }
        } catch (Exception e) {
          GPLogger.log(new RuntimeException("Failure reading news and updates: " + e.getMessage(), e));
        } finally {
          GPLogger.log("RSS check finished");
        }
      }

      private void processResponse(InputStream responseStream) {
        RssFeed feed = parser.parse(responseStream, myLastCheckOption.getValue());
        List<NotificationItem> items = new ArrayList<>();
        for (RssFeed.Item item : feed.getItems()) {
            items.add(getNotificationManager().createNotification(
              NotificationChannel.RSS, item.title, item.body, NotificationManager.DEFAULT_HYPERLINK_LISTENER
            ));
        }
        Collections.reverse(items);
        if (!items.isEmpty()) {
          getNotificationManager().addNotifications(items);
        }
        markLastCheck();
      }
    };
    httpExecutor.execute(cmd);
  }

  private static SimpleDateFormat ourDateFormat = new SimpleDateFormat("yyyyMMdd");
  private boolean wasToday(Date date) {
    return ourDateFormat.format(date).equals(ourDateFormat.format(new Date()));
  }

  private void markLastCheck() {
    myLastCheckOption.setValue(new Date());
  }
}
