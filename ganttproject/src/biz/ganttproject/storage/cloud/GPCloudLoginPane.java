// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.core.OperationStatus;
import biz.ganttproject.storage.StorageDialogBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import net.sourceforge.ganttproject.GPVersion;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static biz.ganttproject.storage.cloud.GPCloudStorage.newHyperlink;
import static biz.ganttproject.storage.cloud.GPCloudStorage.newLabel;

/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudLoginPane implements GPCloudStorage.PageUi {
  private static final String PIN_APPLY_URL = "http://cloud.ganttproject.biz/me/pin-apply";
  private static final URI PIN_REQUEST_URL = URI.create("http://cloud.ganttproject.biz/");

  private final GPCloudStorageOptions myOptions;
  private final StorageDialogBuilder.DialogUi myDialogUi;
  private final Consumer<Pane> myUpdateUi;
  private final WebdavStorage myWebdavStorage;
  private LoginForm myLoginForm;
  private Button mySigninButton;

  GPCloudLoginPane(GPCloudStorageOptions cloudStorageOptions, StorageDialogBuilder.DialogUi dialogUi,
                   Consumer<Pane> updateUi, WebdavStorage webdavStorage) {
    myOptions = cloudStorageOptions;
    myDialogUi = dialogUi;
    myUpdateUi = updateUi;
    myWebdavStorage = webdavStorage;
  }

  public CompletableFuture<Pane> createPane() {
    GridPane result = new GridPane();
    int row = 0;
    result.setHgap(10);
    result.setPrefWidth(400);
    result.getStyleClass().add("pane-service-contents");
    Label title = newLabel("Sign in to GanttProject Cloud", "title");
    result.add(title, 0, row++, 2, 1);

    Label pinSubtitle = newLabel("With PIN", "subtitle");
    result.add(pinSubtitle, 0, row++, 2, 1);
    result.add(newHyperlink(e -> SwingUtilities.invokeLater(() -> {
      try {
        Desktop.getDesktop().browse(PIN_REQUEST_URL);
      } catch (IOException ex) {
        myDialogUi.error(ex);
      }
    }), "First time accessing GanttProject Cloud? [Request a PIN]", "help"), 0, row++, 2, 1);
    row += addPinControls(result, row, myLoginForm);

    Label loginSubtitle = newLabel("With email and password", "subtitle");
    result.add(loginSubtitle, 0, row++, 2, 1);

    myLoginForm = new LoginForm();
    row += myLoginForm.createPane(result, row);

    HBox loginBox = new HBox();
    loginBox.setAlignment(Pos.CENTER);
    mySigninButton = new Button("Sign In");
    loginBox.getChildren().add(mySigninButton);
    result.add(loginBox, 0, row++, 2, 1);

    //result.getChildren().addAll(title, loginSubtitle, myLoginForm.createPane());
    myOptions.getCloudServer().ifPresent(server -> {
      myLoginForm.myEmail.setValue(server.getUsername());
      String password = Strings.nullToEmpty(server.getPassword());
      myLoginForm.myPassword.setValue(password);
      if (password.isEmpty()) {
        myLoginForm.myForgetPassword.setValue(true);
      } else {
        myLoginForm.mySavePassword.setValue(true);
      }
    });
    myLoginForm.myEmail.addListener((observable, oldValue, newValue) -> {
      updateLoginButton();
    });
    myLoginForm.myPassword.addListener((observable, oldValue, newValue) -> {
      updateLoginButton();
    });
    myLoginForm.myForgetPassword.setValue(true);

    return CompletableFuture.completedFuture(result);
  }

  private void updateLoginButton() {
    saveCloudServer(new CloudSettingsDto(GPCloudStorageOptions.CANONICAL_GANTTPROJECT_CLOUD_URL, myLoginForm.myEmail.getValue(), myLoginForm.myPassword.getValue()));
    mySigninButton.setDisable(Strings.isNullOrEmpty(myLoginForm.myEmail.getValue()) || Strings.isNullOrEmpty(myLoginForm.myPassword.getValue()));
  }

  private int addPinControls(GridPane grid, int startRow, LoginForm loginForm) {
    TextField pinField = new TextField();
    pinField.setPromptText("Type PIN");
    HBox pinBox = new HBox();
    pinBox.getStyleClass().add("control-row");
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
      myDialogUi.showBusyIndicator(true);
      pinApplyService.setOnSucceeded(e -> {
        Worker<OperationStatus<CloudSettingsDto, OperationStatus.DefaultCode>> worker = e.getSource();
        OperationStatus<CloudSettingsDto, OperationStatus.DefaultCode> result = worker.getValue();
        if (result.isOk()) {
          CloudSettingsDto dto = result.get();
          saveCloudServer(dto);
          myLoginForm.myEmail.setValue(dto.username);
          myLoginForm.myPassword.setValue(dto.password);
          myLoginForm.myForgetPassword.setValue(true);
          nextPage();
        } else {
          myDialogUi.error(result.getMessage());
        }
        myDialogUi.showBusyIndicator(false);
      });
      pinApplyService.start();
//      CloudSettingsDto dto = new CloudSettingsDto("http://webdav.ganttproject.biz", "dbarashev@bardsoftware.com", "foobar");
//      saveCloudServer(dto);
//      loginForm.myEmail.setValue(dto.username);
//      loginForm.myPassword.setValue(dto.password);
//      loginForm.myForgetPassword.setValue(true);
    });
    pinBox.getChildren().addAll(pinField, btnConnect);

    grid.add(pinBox, 1, startRow);
    return 1;
  }

  private void nextPage() {
    myOptions.getCloudServer().ifPresent(server -> {
      myWebdavStorage.setServer(server);
      myUpdateUi.accept(myWebdavStorage.createUi());
    });

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


  static class LoginForm {
    StringProperty myEmail;
    StringProperty myPassword;
    BooleanProperty mySavePassword;
    BooleanProperty myForgetPassword;

    int createPane(GridPane grid, int startRow) {
      grid.add(newLabel("Email", "control-label"), 0, 0 + startRow);
      TextField email = new TextField();
      myEmail = email.textProperty();
      grid.add(email, 1, 0 + startRow);

      grid.add(newLabel("Password", "control-label"), 0, 1 + startRow);
      PasswordField password = new PasswordField();
      myPassword = password.textProperty();
      grid.add(password, 1, 1 + startRow);

      //grid.add(newLabel("Keep password", "control-label"), 0, 2 + startRow);
      ToggleGroup btnGroup = new ToggleGroup();
      RadioButton dontSave = new RadioButton("Don't save password anywhere");
      dontSave.setToggleGroup(btnGroup);
      myForgetPassword = dontSave.selectedProperty();

      RadioButton savePassword = new RadioButton("Save password in the options file");
      savePassword.setToggleGroup(btnGroup);
      mySavePassword = savePassword.selectedProperty();

      grid.add(dontSave, 1, 2 + startRow);
      grid.add(savePassword, 1, 3 + startRow);
      return 4;
    }
  }
}
