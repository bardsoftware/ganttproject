// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.webdav;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import net.sourceforge.ganttproject.language.GanttLanguage;

import java.util.function.Consumer;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavPasswordPane {
  private final GanttLanguage i18n = GanttLanguage.getInstance();
  private final WebDavServerDescriptor myServer;
  private final Consumer<WebDavServerDescriptor> myServerConsumer;

  WebdavPasswordPane(WebDavServerDescriptor server, Consumer<WebDavServerDescriptor> serverConsumer) {
    myServer = server;
    myServerConsumer = serverConsumer;
  }

  Pane createUi() {
    HBox hbox = new HBox();

    HBox topPane = new HBox();
    topPane.getStyleClass().add("title");
    Label title = new Label(i18n.formatText("webdav.ui.title.password", myServer.name));
    topPane.getChildren().add(title);

    PasswordField passwordField = new PasswordField();
    Button btnDone = new Button("Sign in");
    btnDone.addEventHandler(ActionEvent.ACTION, event -> onDone(passwordField.getText()));
    HBox.setHgrow(passwordField, Priority.ALWAYS);
    hbox.getChildren().addAll(passwordField, btnDone);

    VBox result = new VBox();
    result.getStyleClass().add("webdav-server-password");
    result.getChildren().addAll(topPane, hbox);

    return result;
  }

  private void onDone(String password) {
    myServer.setPassword(password);
    myServerConsumer.accept(myServer);
  }
}
