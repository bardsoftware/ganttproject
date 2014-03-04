/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class NotificationComponent implements NotificationChannel.Listener {
  public static interface AnimationView {
    boolean isReady();

    boolean isVisible();

    void setComponent(JComponent component, JComponent owner, Runnable onHide);

    void close();
  }

  private final JPanel myComponent;
  private final Action[] myActions;
  int myPosition;
  private Action myBackwardAction;
  private Action myForwardAction;
  private final Set<NotificationItem> myNotifications = new HashSet<NotificationItem>();
  private final NotificationChannel myChannel;
  private Action myClearAction;
  private final AnimationView mySlider;

  NotificationComponent(NotificationChannel channel, AnimationView slider) {
    mySlider = slider;
    myComponent = new JPanel(new CardLayout());
    List<Action> actions = new ArrayList<Action>();
    myBackwardAction = createBackwardAction();
    myForwardAction = createForwardAction();
    myClearAction = createClearAction();
    actions.add(myBackwardAction);
    actions.add(myForwardAction);
    actions.add(myClearAction);
    myActions = actions.toArray(new Action[0]);
    myChannel = channel;
    myChannel.addListener(this);
  }

  void processItems() {
    if (myChannel.getItems().isEmpty() && myChannel.getDefaultNotification() != null) {
      addNotification(myChannel.getDefaultNotification(), myChannel);
    }
    for (NotificationItem notification : myChannel.getItems()) {
      addNotification(notification, myChannel);
    }
    if (!myNotifications.isEmpty()) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          updateEnabled();
        }
      });
    }
  }

  void addNotification(NotificationItem item, NotificationChannel channel) {
    if (!myNotifications.contains(item)) {
      addNotification(item.myTitle, item.myBody, item.myHyperlinkListener, channel);
      myNotifications.add(item);
    }
  }

  void addNotification(String title, String body, HyperlinkListener hyperlinkListener, NotificationChannel channel) {
    JComponent htmlPane = createHtmlPane(GanttLanguage.getInstance().formatText("error.channel.text", title, body),
        hyperlinkListener);
    UIUtil.setBackgroundTree(htmlPane, channel.getColor());
    htmlPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(channel.getColor().darker()),
        BorderFactory.createEmptyBorder(3, 3, 3, 3)));
    myComponent.add(htmlPane, String.valueOf(myComponent.getComponentCount()));
  }

  private Action createBackwardAction() {
    return new GPAction("updateRss.backwardItem") {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        if (myPosition > 0) {
          ((CardLayout) myComponent.getLayout()).show(myComponent, String.valueOf(--myPosition));
          updateEnabled();
        }
      }
    };

  }

  private Action createClearAction() {
    return new GPAction("updateRss.clear") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        myChannel.clear();
        mySlider.close();
      }
    };
  }

  private Action createForwardAction() {
    return new GPAction("updateRss.forwardItem") {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        if (myPosition < myComponent.getComponentCount() - 1) {
          ((CardLayout) myComponent.getLayout()).show(myComponent, String.valueOf(++myPosition));
          updateEnabled();
        }
      }
    };
  }

  private void updateEnabled() {
    assert myBackwardAction != null && myForwardAction != null;
    myBackwardAction.setEnabled(myPosition > 0);
    myForwardAction.setEnabled(myPosition < myComponent.getComponentCount() - 1);
    if (!myChannel.getItems().isEmpty()) {
      myChannel.setRead(myPosition);
    }
  }

  JComponent getComponent() {
    Action[] actions = getActions();
    JPanel buttonPanel = new JPanel(new GridLayout(1, actions.length, 2, 0));
    for (final Action a : actions) {
      JButton button = new TestGanttRolloverButton(a);
      buttonPanel.add(button);
    }
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
    JPanel result = new JPanel(new BorderLayout());

    result.add(myComponent, BorderLayout.CENTER);
    result.add(buttonPanel, BorderLayout.NORTH);
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    return result;
  }

  Action[] getActions() {
    return myActions;
  }

  static JScrollPane createHtmlPane(String html, HyperlinkListener hyperlinkListener) {
    JEditorPane htmlPane = UIUtil.createHtmlPane(html, hyperlinkListener);
    htmlPane.setBackground(Color.YELLOW);
    htmlPane.setBorder(BorderFactory.createEmptyBorder());
    Dimension htmlSize = htmlPane.getPreferredSize();

    final JScrollPane scrollPane = new JScrollPane(htmlPane);
    scrollPane.setAutoscrolls(false);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setPreferredSize(new Dimension(Math.min(400, htmlSize.width + 50), Math.min(300, htmlSize.height + 50)));
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        scrollPane.getVerticalScrollBar().setValue(0);
      }
    });
    return scrollPane;
  }

  @Override
  public void notificationAdded() {
    processItems();
  }

  @Override
  public void notificationRead(NotificationItem item) {
    // Do nothing
  }

  @Override
  public void channelCleared() {
    myNotifications.clear();
    myComponent.removeAll();
    processItems();
  }
}
