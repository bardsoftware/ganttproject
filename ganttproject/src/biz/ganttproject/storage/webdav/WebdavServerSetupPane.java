// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.webdav;

import biz.ganttproject.storage.StorageDialogBuilder;
import biz.ganttproject.storage.cloud.GPCloudStorageOptions;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
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
    PropertySheet propertySheet = new PropertySheet();
    propertySheet.setModeSwitcherVisible(false);
    propertySheet.setSearchBoxVisible(false);
    propertySheet.getItems().add(new BeanProperty(
        myWebdavServer, new PropertyDescriptor("name", WebDavServerDescriptor.class)));
    propertySheet.getItems().add(new BeanProperty(
        myWebdavServer, new PropertyDescriptor("rootUrl", WebDavServerDescriptor.class)));
    propertySheet.getItems().add(new BeanProperty(
        myWebdavServer, new PropertyDescriptor("username", WebDavServerDescriptor.class)));
    propertySheet.getItems().add(new BeanProperty(
        myWebdavServer, new PropertyDescriptor("password", WebDavServerDescriptor.class)));
    propertySheet.getItems().add(new BeanProperty(
        myWebdavServer, new PropertyDescriptor("savePassword", WebDavServerDescriptor.class)) {
      @Override
      public Optional<Class<? extends PropertyEditor<?>>> getPropertyEditorClass() {
        return Optional.of(PasswordPropertyEditor.class);
      }
    });
    BorderPane result = new BorderPane();
    result.setTop(new Label("New WebDAV Server"));
    result.setCenter(propertySheet);

    Button btnDone = new Button("Done");
    btnDone.addEventHandler(ActionEvent.ACTION, event -> onDone());
    result.setBottom(btnDone);
    return result;
  }

  private void onDone() {
    myOptions.addWebdavServer(myWebdavServer);
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
