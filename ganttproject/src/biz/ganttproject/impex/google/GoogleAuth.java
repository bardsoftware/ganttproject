package biz.ganttproject.impex.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GoogleAuth {

  //Client ID
  private static final String CLIENT_ID = "299608862260-vnb8d18h7cismqil9fbhf800jlnd3u6d.apps.googleusercontent.com";

  //Client Secret
  private static final String CLIENT_SECRET = "LUcqnHuf4NCqmEkljaJM0giT";

  // Application name
  private static final String APPLICATION_NAME =
          "Google Export for GanttProject";

  // Global instance of the JSON factory
  private final JsonFactory JSON_FACTORY =
          JacksonFactory.getDefaultInstance();

  // Global instance of the HTTP transport
  private HttpTransport HTTP_TRANSPORT = new HttpTransport() {
    @Override
    protected LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
      return null;
    }
  };

  private static final List<String> SCOPES =
          Arrays.asList(CalendarScopes.CALENDAR_READONLY);

  public Credential authorize() throws Exception{
      // Build flow and trigger user authorization request.
    HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, CLIENT_ID, CLIENT_SECRET, SCOPES)
            .build();
    return new AuthorizationCodeInstalledApp(
        flow, new LocalServerReceiver()).authorize("ganttuser");
  }

  public Calendar getCalendarService() throws Exception {
    Credential credential = authorize();
    return new Calendar.Builder(
        HTTP_TRANSPORT, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  public void someSampleWork() throws Exception {
    // Build a new authorized API client service.
    Calendar service = getCalendarService();

    // List the next 10 events from the primary calendar.
    DateTime now = new DateTime(System.currentTimeMillis());
    Events events = service.events().list("primary")
        .setMaxResults(10)
        .setTimeMin(now)
        .setOrderBy("startTime")
        .setSingleEvents(true)
        .execute();
    List<Event> items = events.getItems();
    if (items.size() == 0) {
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