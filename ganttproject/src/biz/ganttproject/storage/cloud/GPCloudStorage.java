// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.storage.StorageDialogBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import net.sourceforge.ganttproject.GPVersion;
import net.sourceforge.ganttproject.document.DocumentStorageUi;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPCloudStorage implements StorageDialogBuilder.Ui {
  private static final String PIN_APPLY_URL = "http://cloud.ganttproject.biz/me/pin-apply";
  private final GPCloudStorageOptions myOptions;
  private final BorderPane myPane;

  public GPCloudStorage(GPCloudStorageOptions options) {
    myOptions = options;
    myPane = new BorderPane();
  }

  private static Label newLabel(String key, String... classes) {
    Label label = new Label(key);
    label.getStyleClass().addAll(classes);
    label.setPrefWidth(400);
    return label;
  }

  private static Pane centered(Node... nodes) {
    VBox centered = new VBox();
    centered.setMaxWidth(Double.MAX_VALUE);
    centered.getStyleClass().add("center");
    centered.getChildren().addAll(nodes);
    return centered;
  }

  private Pane createSetupCloudPane() {
    VBox cloudSetupPane = new VBox();
    cloudSetupPane.setPrefWidth(400);
    cloudSetupPane.getStyleClass().add("pane-service-contents");
    Label title = newLabel("Setup GanttProject Cloud", "title");
    Label titleHelp = newLabel(
        "GanttProject Cloud is a cloud-based service for storing projects and collaborating with your colleagues",
        "title-help");
    cloudSetupPane.getChildren().addAll(title, titleHelp);

    Label signupWarning = newLabel(
        "It seems that this GanttProject is not yet connected to the Cloud.", "alert-warning");

    Label pinSubtitle = newLabel("Already registered?", "subtitle");
    Label pinHelp = newLabel(
        "You need to connect this GanttProject to the Cloud. Sign in to your account on GanttProject Cloud and request a PIN number. Type the PIN into the field below to setup access credentials",
        "help");
    cloudSetupPane.getChildren().addAll(signupWarning, pinSubtitle, pinHelp);
    addPinControls(cloudSetupPane);

    Label signupSubtitle = newLabel("Not yet signed up?", "subtitle");
    Label signupHelp = newLabel(
        "Creating an account on GanttProject Cloud is free and easy. No credit card required. Get up and running instantly.",
        "help");
    cloudSetupPane.getChildren().addAll(signupSubtitle, signupHelp);


    Button signupButton = new Button("Sign Up");
    signupButton.getStyleClass().addAll("btn-signup");
    cloudSetupPane.getChildren().add(centered(signupButton));
    return cloudSetupPane;
  }

  public static class CloudSettingsDto {
    public final String serverUrl;
    public final String username;
    public final String password;

    @JsonCreator
    public CloudSettingsDto(
        @JsonProperty("serverUrl") String serverUrl, @JsonProperty("username") String username, @JsonProperty("password") String password) {
      this.serverUrl = serverUrl;
      this.username = username;
      this.password = password;
    }
  }

  private void addPinControls(Pane pane) {
    TextField pinField = new TextField();
    pinField.setPromptText("Type your PIN");
    HBox pinBox = new HBox();
    pinBox.setMaxWidth(Region.USE_PREF_SIZE);
    Button btnConnect = new Button("Connect");
    btnConnect.addEventHandler(ActionEvent.ACTION, event -> {
      CloudSettingsDto cloudSettings = getCloudSettings(pinField.getText());
      saveCloudServer(cloudSettings);
    });
    pinBox.getChildren().addAll(pinField, btnConnect);

    pane.getChildren().add(centered(pinBox));
  }

  private void saveCloudServer(CloudSettingsDto cloudSettings) {
    myOptions.setCloudServer(cloudSettings);
  }

  private CloudSettingsDto getCloudSettings(String pin) {
    HttpClient httpClient = new DefaultHttpClient();
    try {
      for (int i = 0; i < 3; i++) {
        HttpPost pinApply = new HttpPost(PIN_APPLY_URL);
        pinApply.addHeader("User-Agent", "GanttProject " + GPVersion.CURRENT);
        List<NameValuePair> args = Lists.newArrayList();
        args.add(new BasicNameValuePair("pin", pin));
        pinApply.setEntity(new UrlEncodedFormEntity(args));
        HttpResponse response = httpClient.execute(pinApply);

        switch (response.getStatusLine().getStatusCode()) {
          case HttpStatus.SC_OK:
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getEntity().getContent(), CloudSettingsDto.class);
        }
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      httpClient.getConnectionManager().shutdown();
    }
    return null;
  }

  private Pane createConnectCloudPane() {
    VBox cloudConnectPane = new VBox();
    cloudConnectPane.setPrefWidth(400);
    cloudConnectPane.getStyleClass().add("pane-service-contents");
    Label title = newLabel("Sign in to GanttProject Cloud", "title");
    Label titleHelp = newLabel(
        "You seem to be registered on GanttProject Cloud but you don't store your password on disk. You need to request a new PIN and type it into the field below",
        "title-help");
    cloudConnectPane.getChildren().addAll(title, titleHelp);
    addPinControls(cloudConnectPane);
    return cloudConnectPane;
  }


  @Override
  public String getId() {
    return "cloud";
  }

  @Override
  public Pane createUi(DocumentStorageUi.DocumentReceiver documentReceiver, StorageDialogBuilder.ErrorUi errorUi) {
    if (myPane.getChildren().isEmpty()) {
      WebDavServerDescriptor cloudServer = myOptions.getCloudServer();
      if (cloudServer == null) {
        myPane.setCenter(createSetupCloudPane());
      } else if (cloudServer.getPassword() == null) {
        myPane.setCenter(createConnectCloudPane());
      } else {
        WebdavStorage webdavStorage = new WebdavStorage(cloudServer);
        myPane.setCenter(webdavStorage.createUi(documentReceiver, errorUi));
      }
    }
    return myPane;
  }
}
