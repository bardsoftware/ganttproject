// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.webdav;

import biz.ganttproject.storage.StorageDialogBuilder;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import net.sourceforge.ganttproject.document.webdav.WebDavResource;

import java.util.Optional;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavResourceListView {
  private final StorageDialogBuilder.DialogUi myDialogUi;

  static class ListViewItem {
    final BooleanProperty isSelected = new SimpleBooleanProperty();
    final ObjectProperty<WebDavResource> resource;

    ListViewItem(WebDavResource resource) {
      this.resource = new SimpleObjectProperty<>(resource);
    }

    public static Callback<ListViewItem, Observable[]> asObservables() {
      return item -> new Observable[] {item.isSelected, item.resource};
    }
  }

  private final ListView<ListViewItem> myListView;

  WebdavResourceListView(StorageDialogBuilder.DialogUi dialogUi, Runnable onDeleteResource, Runnable onToggleLockResource, BooleanProperty isLockingSupported) {
    myDialogUi = dialogUi;
    myListView = new ListView<>();
    myListView.setCellFactory(param -> new ListCell<ListViewItem>() {
      @Override
      protected void updateItem(ListViewItem item, boolean empty) {
        try {
          doUpdateItem(item, empty);
        } catch (WebDavResource.WebDavException e) {
          myDialogUi.error(e);
        }
      }
      private void doUpdateItem(ListViewItem item, boolean empty) throws WebDavResource.WebDavException {
        if (item == null) {
          setText("");
          setGraphic(null);
          return;
        }
        super.updateItem(item, empty);
        if (empty) {
          setText("");
          setGraphic(null);
          return;
        }
        HBox hbox = new HBox();
        hbox.getStyleClass().add("webdav-list-cell");
        boolean isLocked = item.resource.getValue().isLocked();
        boolean isLockable = item.resource.getValue().isLockSupported(true);
        if (isLockable && !isLockingSupported.getValue()) {
          isLockingSupported.setValue(true);
        }

        FontAwesomeIconView icon = isLocked
            ?  new FontAwesomeIconView(FontAwesomeIcon.LOCK)
            :  new FontAwesomeIconView(FontAwesomeIcon.FOLDER);
        if (!item.resource.getValue().isCollection()) {
          icon.getStyleClass().add("hide");
        } else {
          icon.getStyleClass().add("icon");
        }
        Label label = new Label(item.resource.getValue().getName(), icon);
        hbox.getChildren().add(label);
        if (item.isSelected.getValue() && !item.resource.getValue().isCollection()) {
          HBox btnBox = new HBox();
          btnBox.getStyleClass().add("webdav-list-cell-button-pane");
          Button btnDelete = new Button("", new FontAwesomeIconView(FontAwesomeIcon.TRASH));
          btnDelete.addEventHandler(ActionEvent.ACTION, event -> onDeleteResource.run());

          Button btnLock = null;
          if (isLocked) {
            btnLock = new Button("", new FontAwesomeIconView(FontAwesomeIcon.UNLOCK));
          } else if (isLockable) {
            btnLock = new Button("", new FontAwesomeIconView(FontAwesomeIcon.LOCK));
          }
          if (btnLock != null) {
            btnLock.addEventHandler(ActionEvent.ACTION, event -> onToggleLockResource.run());
            btnBox.getChildren().add(btnLock);
          }
          btnBox.getChildren().add(btnDelete);
          HBox.setHgrow(btnBox, Priority.ALWAYS);
          hbox.getChildren().add(btnBox);
        } else {
          Button placeholder = new Button("");
          placeholder.getStyleClass().add("hide");
          hbox.getChildren().add(placeholder);
        }
        setGraphic(hbox);
      }
    });
    myListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.isSelected.setValue(false);
      }
      if (newValue != null) {
        newValue.isSelected.setValue(true);
      }
    });
  }

  ListView<ListViewItem> getListView() {
    return myListView;
  }

   void setResources(ObservableList<WebDavResource> webDavResources) {
    ObservableList<ListViewItem> items = FXCollections.observableArrayList(ListViewItem.asObservables());
    myListView.setItems(items);
    webDavResources.stream().map(ListViewItem::new).forEach(items::add);
  }


  Optional<WebDavResource> getSelectedResource() {
    ListViewItem selectedItem = myListView.getSelectionModel().getSelectedItem();
    return selectedItem == null ? Optional.empty() : Optional.ofNullable(selectedItem.resource.getValue());
  }

}
