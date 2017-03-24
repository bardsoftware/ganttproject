/*
Copyright (C) 2017 BarD Software s.r.o., Leonid Shatunov

This file is part of GanttProject, an open-source project management tool.

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
package biz.ganttproject.impex.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.List;

/**
 * Methods for google authorization
 *
 * @author Leonid Shatunov (shatuln@gmail.com)
 */
public class GoogleAuth {

  private static final String CLIENT_ID = "299608862260-vnb8d18h7cismqil9fbhf800jlnd3u6d.apps.googleusercontent.com";

  private static final String CLIENT_SECRET = "LUcqnHuf4NCqmEkljaJM0giT";

  private static final String APPLICATION_NAME = "Google Export for GanttProject";

  private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  private static final List<String> SCOPES = ImmutableList.of(CalendarScopes.CALENDAR_READONLY);

  public Credential authorize() throws GeneralSecurityException, IOException {
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, CLIENT_ID, CLIENT_SECRET, SCOPES)
            .build();
    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("ganttuser");
  }

  public Calendar getCalendarService(Credential credential) throws GeneralSecurityException, IOException {
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  public List<CalendarListEntry> getAvaliableCalendarList(Calendar service) throws IOException {
    List<CalendarListEntry> myCalendars = service.calendarList().list().execute().getItems();
    Iterator<CalendarListEntry> iter = myCalendars.iterator();
    while (iter.hasNext()) {
      String role = iter.next().getAccessRole();
      if (role.equals("owner") || role.equals("writer")) {
        System.out.println("yeap");
      } else {
        iter.remove();
      }
    }
    return myCalendars;
  }

  public void someSampleWork(Calendar service) throws IOException {
    // List the next 10 events from the primary calendar.
    DateTime now = new DateTime(System.currentTimeMillis());
    Events events = service.events().list("primary")
        .setMaxResults(10)
        .setTimeMin(now)
        .setOrderBy("startTime")
        .setSingleEvents(true)
        .execute();
    List<Event> items = events.getItems();
    if (items.isEmpty()) {
      System.out.println("No upcoming events found.");
    } else {
      System.out.println("Upcoming events");
      for (Event event : items) {
        DateTime start = event.getStart().getDateTime();
        if (start == null) {
          start = event.getStart().getDate();
        }
        System.out.printf("%s (%s)\n", event.getSummary(), start);
      }
    }
  }
}