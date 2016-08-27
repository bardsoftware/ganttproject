// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.webdav;

import biz.ganttproject.storage.StorageDialogBuilder;
import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.*;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import net.sourceforge.ganttproject.language.GanttLanguage;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.BeanProperty;
import org.controlsfx.property.editor.AbstractPropertyEditor;
import org.controlsfx.property.editor.PropertyEditor;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.Optional;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavServerSetupPane implements StorageDialogBuilder.Ui {
  private final WebDavServerDescriptor myWebdavServer;
  private final GPCloudStorageOptions myOptions;

  public WebdavServerSetupPane(GPCloudStorageOptions options, WebDavServerDescriptor webdavServer) {
    myOptions = options;
    myWebdavServer = webdavServer;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public String getId() {
    return null;
  }

  @Override
  public Pane createUi() {
    try {
      return doCreateUi();
    } catch (IntrospectionException e) {
      e.printStackTrace();
      return new Pane();
    }
  }

  private Pane doCreateUi() throws IntrospectionException {
    VBox centerBox = new VBox();
    PropertySheet propertySheet = new PropertySheet();
    propertySheet.getStyleClass().addAll("property-sheet");
    propertySheet.setModeSwitcherVisible(false);
    propertySheet.setSearchBoxVisible(false);
    propertySheet.getItems().add(new BeanProperty(
        myWebdavServer, new WebDavPropertyDescriptor("name", "webdav.serverName")));
    propertySheet.getItems().add(new BeanProperty(
        myWebdavServer, new WebDavPropertyDescriptor("rootUrl", "option.webdav.server.url.label")));
    propertySheet.getItems().add(new BeanProperty(
        myWebdavServer, new WebDavPropertyDescriptor("username", "option.webdav.server.username.label")));
    propertySheet.getItems().add(new BeanProperty(
        myWebdavServer, new WebDavPropertyDescriptor("password", "option.webdav.server.password.label")){
      @Override
      public Optional<Class<? extends PropertyEditor<?>>> getPropertyEditorClass() {
        return Optional.of(PasswordPropertyEditor.class);
      }
    });
    propertySheet.getItems().add(new BeanProperty(
        myWebdavServer, new WebDavPropertyDescriptor("savePassword", "option.webdav.server.savePassword.label.trailing")) );
    Button btnDone = new Button("Done");
    btnDone.addEventHandler(ActionEvent.ACTION, event -> onDone());
    HBox bottomBox = new HBox();
    bottomBox.getStyleClass().add("center");
    bottomBox.getChildren().addAll(btnDone);
    Pane spacer = new Pane();
    VBox.setVgrow(propertySheet, Priority.SOMETIMES);

    centerBox.getChildren().addAll(propertySheet, bottomBox);

    BorderPane result = new BorderPane();
    result.getStyleClass().addAll("pane-service-contents", "webdav-server-setup");

    HBox titleBox = new HBox();
    titleBox.getStyleClass().add("title");
    titleBox.getChildren().add(new Label("New WebDAV Server"));
    result.setTop(titleBox);
    result.setCenter(centerBox);

    return result;
  }

  private void onDone() {
    myOptions.addWebdavServer(myWebdavServer);
  }

  private static class WebDavPropertyDescriptor extends PropertyDescriptor {

    public WebDavPropertyDescriptor(String propertyName, String i18nKey) throws IntrospectionException {
      super(propertyName, WebDavServerDescriptor.class);
      setDisplayName(GanttLanguage.getInstance().getText(i18nKey));
    }
  }
  public static class PasswordPropertyEditor extends AbstractPropertyEditor<String, PasswordField> {

    public PasswordPropertyEditor(PropertySheet.Item property) {
      super(property, new PasswordField());
    }

    @Override
    protected ObservableValue<String> getObservableValue() {
      return (ObservableValue<String>) getProperty().getObservableValue().get();
    }

    @Override
    public void setValue(String s) {
      getProperty().setValue(s);
    }
  }
}
