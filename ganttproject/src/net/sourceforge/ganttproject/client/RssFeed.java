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

import java.util.ArrayList;
import java.util.List;

public class RssFeed {
  public static class Item {
    public final String title;
    public final String body;
    public final boolean isUpdate;

    Item(String title, String body, boolean isUpdate) {
      this.title = title;
      this.body = body;
      this.isUpdate = isUpdate;
    }
  }

  private final List<Item> myItems = new ArrayList<Item>();

  void addItem(String title, String body, boolean isUpdate) {
    myItems.add(new Item(title, body, isUpdate));
  }

  public List<Item> getItems() {
    return myItems;
  }
}
