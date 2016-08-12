// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import net.sourceforge.ganttproject.document.webdav.WebDavResource;

import java.util.Optional;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavResourceListView {
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

  WebdavResourceListView() {
    myListView = new ListView<>();
    myListView.setCellFactory(param -> new ListCell<ListViewItem>() {
      @Override
      protected void updateItem(ListViewItem item, boolean empty) {
        if (item == null) {
          setText("");
          return;
        }
        super.updateItem(item, empty);
        if (empty) {
          setGraphic(null);
          return;
        }
        HBox hbox = new HBox();
        Label label = new Label(item.resource.getValue().getName());
        hbox.getChildren().add(label);
        if (item.isSelected.getValue()) {
          Button btn = new Button("Delete");
          hbox.getChildren().addAll(btn);
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

  public void setResources(ObservableList<WebDavResource> webDavResources) {
    ObservableList<ListViewItem> items = FXCollections.observableArrayList(ListViewItem.asObservables());
    myListView.setItems(items);
    webDavResources.stream().map(ListViewItem::new).forEach(items::add);
  }


  public Optional<WebDavResource> getSelectedResource() {
    ListViewItem selectedItem = myListView.getSelectionModel().getSelectedItem();
    return selectedItem == null ? Optional.empty() : Optional.ofNullable(selectedItem.resource.getValue());
  }

}
