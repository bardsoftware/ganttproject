// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.storage.StorageDialogBuilder;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import org.controlsfx.control.BreadCrumbBar;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavStorage implements StorageDialogBuilder.Ui {
  @Override
  public String getId() {
    return null;
  }

  @Override
  public Pane createUi() {
    BorderPane borderPane = new BorderPane();
    BreadCrumbBar<String> breadcrumbs = new BreadCrumbBar<>(BreadCrumbBar.buildTreeModel("GanttProject Cloud", "Folder1"));
    borderPane.setTop(breadcrumbs);
    return borderPane;
  }
}
