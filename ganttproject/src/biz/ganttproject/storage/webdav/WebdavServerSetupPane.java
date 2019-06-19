// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.webdav;

import biz.ganttproject.FXUtil;
import biz.ganttproject.storage.StorageDialogBuilder;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import net.sourceforge.ganttproject.language.GanttLanguage;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.BeanProperty;
import org.controlsfx.property.editor.AbstractPropertyEditor;
import org.controlsfx.property.editor.PropertyEditor;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavServerSetupPane implements StorageDialogBuilder.Ui {
  private final WebDavServerDescriptor myWebdavServer;
  private final Consumer<WebDavServerDescriptor> myValueConsumer;
  private final boolean myHasDelete;

  public WebdavServerSetupPane(WebDavServerDescriptor webdavServer, Consumer<WebDavServerDescriptor> valueConsumer, boolean hasDelete) {
    myWebdavServer = webdavServer.clone();
    myValueConsumer = valueConsumer;
    myHasDelete = hasDelete;
  }

  @Override
  public String getId() {
    return "webdav-setup";
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public String getCategory() {
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
        myWebdavServer, new WebDavPropertyDescriptor("password", "option.webdav.server.password.label")) {
      @Override
      public Optional<Class<? extends PropertyEditor<?>>> getPropertyEditorClass() {
        return Optional.of(PasswordPropertyEditor.class);
      }
    });
    propertySheet.getItems().add(new BeanProperty(
        myWebdavServer, new WebDavPropertyDescriptor("savePassword", "option.webdav.server.savePassword.label.trailing")));
    Button btnDone = new Button("Done");
    btnDone.getStyleClass().add("btn-done");
    FXUtil.INSTANCE.createBreathingButton(btnDone);
    btnDone.addEventHandler(ActionEvent.ACTION, event -> onDone());
    HBox bottomBox = new HBox();
    bottomBox.getStyleClass().add("button-bar");


    Pane spacerPane = new Pane();
    HBox.setHgrow(spacerPane, Priority.ALWAYS);
    bottomBox.getChildren().addAll(btnDone, spacerPane);
    if (myHasDelete) {
      Button btnDelete = new Button("Delete");
      btnDelete.addEventHandler(ActionEvent.ACTION, event -> myValueConsumer.accept(null));
      bottomBox.getChildren().add(btnDelete);
    }
    VBox.setVgrow(propertySheet, Priority.SOMETIMES);

    centerBox.getChildren().addAll(propertySheet, bottomBox);

    BorderPane result = new BorderPane();
    result.getStyleClass().addAll("pane-service-contents", "webdav-server-setup");

    HBox titleBox = new HBox();
    titleBox.getStyleClass().add("title");
    Label title = myHasDelete ? new Label("Edit WebDAV Server") : new Label("New WebDAV Server");
    titleBox.getChildren().add(title);
    result.setTop(titleBox);
    result.setCenter(centerBox);

    return result;
  }

  @Override
  public Optional<Pane> createSettingsUi() {
    return Optional.empty();
  }

  private void onDone() {
    myValueConsumer.accept(myWebdavServer);
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

    protected StringProperty getObservableValue() {
      return this.getEditor().textProperty();
    }

    public void setValue(String value) {
      this.getEditor().setText(value);
    }
  }

}
