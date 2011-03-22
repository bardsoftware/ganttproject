package net.sourceforge.ganttproject.client;

import java.util.ArrayList;
import java.util.List;

class RssFeed {
    static class Item {
        final String title;
        final String body;

        Item(String title, String body) {
            this.title = title;
            this.body = body;
        }
    }

    private final List<Item> myItems = new ArrayList<Item>();

    void addItem(String title, String body) {
        myItems.add(new Item(title, body));
    }

    List<Item> getItems() {
        return myItems;
    }
}
