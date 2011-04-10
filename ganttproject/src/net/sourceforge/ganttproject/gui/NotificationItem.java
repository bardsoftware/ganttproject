package net.sourceforge.ganttproject.gui;

import javax.swing.event.HyperlinkListener;

public class NotificationItem {
    final String myTitle;
    final String myBody;
    final HyperlinkListener myHyperlinkListener;

    public NotificationItem(String title, String body, HyperlinkListener hyperlinkListener) {
        myTitle = title;
        myBody = body;
        myHyperlinkListener = hyperlinkListener;
    }
}
