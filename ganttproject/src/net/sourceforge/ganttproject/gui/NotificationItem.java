package net.sourceforge.ganttproject.gui;

import javax.swing.event.HyperlinkListener;

public class NotificationItem {
    final String myTitle;
    final String myBody;
    final HyperlinkListener myHyperlinkListener;
    boolean isRead;

    public NotificationItem(String title, String body, HyperlinkListener hyperlinkListener) {
        myTitle = title;
        myBody = body;
        myHyperlinkListener = hyperlinkListener;
    }

    public boolean isRead() {
        return isRead;
    }

    void setRead(boolean b) {
        isRead = b;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NotificationItem) {
            NotificationItem that = (NotificationItem) obj;
            return myTitle.equals(that.myTitle) && myBody.equals(that.myBody);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return myBody.hashCode();
    }
}
