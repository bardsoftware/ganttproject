package biz.ganttproject.impex.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.util.DateTime;

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

  // Directory to store user credentials for this application
  private static final java.io.File DATA_STORE_DIR = new java.io.File(
          "data/resources/credentials");

  // Global instance of the {@link FileDataStoreFactory}
  private static FileDataStoreFactory DATA_STORE_FACTORY;

  // Global instance of the JSON factory
  private static final JsonFactory JSON_FACTORY =
          JacksonFactory.getDefaultInstance();

  // Global instance of the HTTP transport
  private static HttpTransport HTTP_TRANSPORT;

  private static final List<String> SCOPES =
          Arrays.asList(CalendarScopes.CALENDAR_READONLY);

  static {
    try {
      HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public static Credential authorize() {
    try {
      // Build flow and trigger user authorization request.
      GoogleAuthorizationCodeFlow flow =
              new GoogleAuthorizationCodeFlow.Builder(
                      HTTP_TRANSPORT, JSON_FACTORY, CLIENT_ID, CLIENT_SECRET, SCOPES)
                      .setDataStoreFactory(DATA_STORE_FACTORY)
                      .setAccessType("offline")
                      .build();
      Credential credential = new AuthorizationCodeInstalledApp(
              flow, new LocalServerReceiver()).authorize("ganttuser");
      System.out.println(
              "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
      return credential;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static com.google.api.services.calendar.Calendar
  getCalendarService() throws IOException {
    Credential credential = authorize();
    return new com.google.api.services.calendar.Calendar.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
  }

  public void someSampleWork() throws IOException {
    // Build a new authorized API client service.
    // Note: Do not confuse this class with the
    //   com.google.api.services.calendar.model.Calendar class.
    com.google.api.services.calendar.Calendar service =
            getCalendarService();

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