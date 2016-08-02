// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.core.OperationStatus;
import biz.ganttproject.storage.StorageDialogBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import net.sourceforge.ganttproject.GPVersion;
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

import static biz.ganttproject.storage.cloud.GPCloudStorage.centered;
import static biz.ganttproject.storage.cloud.GPCloudStorage.newLabel;

/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudLoginPane {
  private static final String PIN_APPLY_URL = "http://cloud.ganttproject.biz/me/pin-apply";

  private final GPCloudStorageOptions myOptions;
  private final StorageDialogBuilder.ErrorUi myErrorUi;

  GPCloudLoginPane(GPCloudStorageOptions cloudStorageOptions, StorageDialogBuilder.ErrorUi errorUi) {
    myOptions = cloudStorageOptions;
    myErrorUi = errorUi;
  }

  Pane createPane() {
    VBox result = new VBox();
    result.setPrefWidth(400);
    result.getStyleClass().add("pane-service-contents");
    Label pinSubtitle = newLabel("Use PIN to get access credentials", "subtitle");
    Label pinHelp = newLabel(
        "If you don't know your access credentials, sign in to your account on GanttProject Cloud and request a PIN number. Type the PIN into the field below.",
        "help");
    result.getChildren().addAll(pinSubtitle, pinHelp);
    addPinControls(result);
    return result;
  }

  private void addPinControls(Pane pane) {
    TextField pinField = new TextField();
    pinField.setPromptText("Type your PIN");
    HBox pinBox = new HBox();
    pinBox.setMaxWidth(Region.USE_PREF_SIZE);
    Button btnConnect = new Button("Connect");

    Service<OperationStatus<CloudSettingsDto, OperationStatus.DefaultCode>> pinApplyService = new Service<OperationStatus<CloudSettingsDto, OperationStatus.DefaultCode>>() {
      @Override
      protected Task<OperationStatus<CloudSettingsDto, OperationStatus.DefaultCode>> createTask() {
        return new Task<OperationStatus<CloudSettingsDto, OperationStatus.DefaultCode>>() {
          @Override
          protected OperationStatus<CloudSettingsDto, OperationStatus.DefaultCode> call() throws Exception {
            return getCloudSettings(pinField.getText());
          }
        };
      }
    };
    btnConnect.addEventHandler(ActionEvent.ACTION, event -> {
      myErrorUi.showBusyIndicator(true);
      pinApplyService.setOnSucceeded(e -> {
        Worker<OperationStatus<CloudSettingsDto, OperationStatus.DefaultCode>> worker = e.getSource();
        OperationStatus<CloudSettingsDto, OperationStatus.DefaultCode> result = worker.getValue();
        if (result.isOk()) {
          saveCloudServer(result.get());
          //replaceUi();
        } else {
          myErrorUi.error(result.getMessage());
        }
        myErrorUi.showBusyIndicator(false);
      });
      pinApplyService.start();
    });
    pinBox.getChildren().addAll(pinField, btnConnect);

    pane.getChildren().add(centered(pinBox));
  }

  private OperationStatus<CloudSettingsDto, OperationStatus.DefaultCode> getCloudSettings(String pin) {
    HttpClient httpClient = new DefaultHttpClient();
    HttpPost pinApply = new HttpPost(PIN_APPLY_URL);
    pinApply.addHeader("User-Agent", "GanttProject " + GPVersion.CURRENT);
    List<NameValuePair> args = Lists.newArrayList();
    args.add(new BasicNameValuePair("pin", pin));
    try {
      for (int i = 0; i < 3; i++) {
        pinApply.setEntity(new UrlEncodedFormEntity(args));
        HttpResponse response = httpClient.execute(pinApply);

        switch (response.getStatusLine().getStatusCode()) {
          case HttpStatus.SC_OK:
            ObjectMapper mapper = new ObjectMapper();
            return OperationStatus.success(mapper.readValue(response.getEntity().getContent(), CloudSettingsDto.class));
          default:
            return OperationStatus.failure("PIN was rejected: %s", response.getStatusLine().getReasonPhrase());
        }
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      pinApply.releaseConnection();
      httpClient.getConnectionManager().shutdown();
    }
    return OperationStatus.failure("PIN failed");
  }

  private void saveCloudServer(CloudSettingsDto cloudSettings) {
    myOptions.setCloudServer(cloudSettings);
  }


}
