/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;
import java.util.Collections;

import javax.swing.SwingUtilities;

import biz.ganttproject.app.DialogKt;
import biz.ganttproject.app.InternationalizationCoreKt;
import biz.ganttproject.app.Localizer;
import biz.ganttproject.app.PropertySheetKt;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.ObservableObject;
import biz.ganttproject.core.option.ObservableString;
import biz.ganttproject.core.option.ValidatorsKt;
import biz.ganttproject.ganttview.Item;
import biz.ganttproject.ganttview.ItemEditorPaneImpl;
import biz.ganttproject.ganttview.ItemListDialogModel;
import biz.ganttproject.ganttview.ItemListDialogPane;
import biz.ganttproject.ganttview.ShowHideListItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import kotlin.Unit;
import net.sourceforge.ganttproject.GanttPreviousState;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;

public class BaselineDialogAction extends GPAction {
  private final IGanttProject myProject;
  private final UIFacade myUiFacade;

  public BaselineDialogAction(IGanttProject project, UIFacade uiFacade) {
    super("baseline.dialog");
    myProject = project;
    myUiFacade = uiFacade;
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    DialogKt.dialog(getI18n("baseline.dialog.title"), "baseline", dlg -> {
      GanttPreviousState currentBaseline = myUiFacade.getGanttChart().getBaseline();
      ObservableList<BaselineItem> listItems = FXCollections.observableArrayList();
      BaselineItem currentItem = null;
      for (GanttPreviousState baseline : myProject.getBaselines()) {
        BaselineItem item = new BaselineItem(baseline, baseline == currentBaseline);
        if (baseline == currentBaseline) {
          currentItem = item;
        }
        listItems.add(item);
      }

      ObservableObject<BaselineItem> selectedItem = new ObservableObject<>("", null);
      ItemListDialogModel<BaselineItem> dialogModel = new ItemListDialogModel<>(listItems,
          () -> new BaselineItem(new GanttPreviousState("", GanttPreviousState.createTasks(myProject.getTaskManager())),
              false),
          ourLocalizer);

      // Wire the show/hide behavior of the items which are already in the list and of those which
      // will be added later on. If the shown baseline is removed from the list, hide it in the chart.
      for (BaselineItem item : listItems) {
        wireItem(item, listItems, dialogModel);
      }
      listItems.addListener((ListChangeListener<BaselineItem>) change -> {
        while (change.next()) {
          for (BaselineItem removed : change.getRemoved()) {
            removed.isEnabledProperty().set(false);
          }
          for (BaselineItem added : change.getAddedSubList()) {
            wireItem(added, listItems, dialogModel);
          }
        }
      });
      dialogModel.getBtnApplyController().setOnAction(() -> {
        SwingUtilities.invokeLater(() -> {
          myProject.getBaselines().clear();
          for (BaselineItem item : listItems) {
            myProject.getBaselines().add(item.getBaseline());
          }
          myProject.setModified();
        });
        return Unit.INSTANCE;
      });

      BaselineItemEditor editor = new BaselineItemEditor(selectedItem, dialogModel, ourLocalizer);
      ItemListDialogPane<BaselineItem> dialogPane = new ItemListDialogPane<>(listItems, selectedItem,
          item -> new ShowHideListItem(
              item::getTitle,
              () -> item.isEnabledProperty().get(),
              () -> {
                item.isEnabledProperty().set(!item.isEnabledProperty().get());
                return Unit.INSTANCE;
              },
              "", true),
          dialogModel, editor, ourLocalizer);

      // Build the baseline color options pane with the JavaFX property pane builder.
      GPOptionGroup colorOptions = myUiFacade.getGanttChart().getBaselineColorOptions();
      Node colorOptionsPane = PropertySheetKt.properties(ourOptionLocalizer, builder -> {
        builder.title(InternationalizationCoreKt.getRootLocalizer().formatText(
            OptionsPageBuilder.I18N.getCanonicalOptionGroupLabelKey(colorOptions)));
        for (GPOption<?> option : colorOptions.getOptions()) {
          option.visitPropertyPaneBuilder(builder);
        }
        return Unit.INSTANCE;
      });

      BorderPane contentPane = new BorderPane();
      contentPane.setCenter(dialogPane.getContentNode());
      contentPane.setBottom(colorOptionsPane);
      dialogPane.setContentNode(contentPane);
      dialogPane.build(dlg);

      dlg.setupButton(new CancelAction(), btn -> Unit.INSTANCE);

      if (currentItem != null) {
        dialogPane.getListView().getSelectionModel().select(currentItem);
      }
      return Unit.INSTANCE;
    });
  }

  /**
   * Adds listeners which keep the item show/hide flag in sync with the chart: at most one baseline
   * can be shown, so showing one item hides all the others, and hiding the shown item removes
   * the baseline from the chart.
   */
  private void wireItem(BaselineItem item, ObservableList<BaselineItem> allItems,
      ItemListDialogModel<BaselineItem> dialogModel) {
    item.isEnabledProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        // Only one baseline can be shown in the chart.
        for (BaselineItem other : allItems) {
          if (other != item) {
            other.isEnabledProperty().set(false);
          }
        }
        dialogModel.getRequireRefresh().set(true);
      }
      SwingUtilities.invokeLater(() -> {
        if (newValue) {
          myUiFacade.getGanttChart().setBaseline(item.getBaseline());
          myUiFacade.getGanttChart().reset();
        } else if (myUiFacade.getGanttChart().getBaseline() == item.getBaseline()) {
          myUiFacade.getGanttChart().setBaseline(null);
          myUiFacade.getGanttChart().reset();
        }
      });
    });
  }

  /**
   * A baseline as an item of the list view. The item title is the baseline name, and the
   * "enabled" flag indicates if the baseline is shown in the chart.
   */
  private static class BaselineItem implements Item<BaselineItem> {
    private final GanttPreviousState myBaseline;
    private final BooleanProperty myIsShownProperty = new SimpleBooleanProperty();

    BaselineItem(GanttPreviousState baseline, boolean isShown) {
      myBaseline = baseline;
      myIsShownProperty.set(isShown);
    }

    GanttPreviousState getBaseline() {
      return myBaseline;
    }

    @Override
    public String getTitle() {
      return myBaseline.getName();
    }

    @Override
    public void setTitle(String title) {
      myBaseline.setName(title);
    }

    @Override
    public BooleanProperty isEnabledProperty() {
      return myIsShownProperty;
    }
  }

  /**
   * Editor pane for the baseline selected in the list view. Just the baseline name can be edited.
   */
  private static class BaselineItemEditor extends ItemEditorPaneImpl<BaselineItem> {
    private final ObservableString myNameOption;

    BaselineItemEditor(ObservableObject<BaselineItem> editItem, ItemListDialogModel<BaselineItem> dialogModel,
        Localizer localizer) {
      this(new ObservableString("name", "", ValidatorsKt.getVoidValidator(), false), editItem, dialogModel, localizer);
    }

    private BaselineItemEditor(ObservableString nameOption, ObservableObject<BaselineItem> editItem,
        ItemListDialogModel<BaselineItem> dialogModel, Localizer localizer) {
      super(Collections.singletonList(nameOption), editItem, dialogModel, localizer);
      myNameOption = nameOption;
    }

    @Override
    protected void loadData(BaselineItem item) {
      if (item != null) {
        myNameOption.setValue(item.getTitle());
        getVisibilityToggle().setSelected(item.isEnabledProperty().get());
      }
    }

    @Override
    protected void saveData(BaselineItem item) {
      item.setTitle(myNameOption.getValue() == null ? "" : myNameOption.getValue());
      item.isEnabledProperty().set(getVisibilityToggle().isSelected());
    }
  }

  private static final Localizer ourLocalizer = InternationalizationCoreKt.getRootLocalizer()
      .createWithRootKey("baseline.dialog", InternationalizationCoreKt.getRootLocalizer());
  // Localizer which resolves option label keys, such as option.<optionID>.label
  private static final Localizer ourOptionLocalizer = InternationalizationCoreKt.getRootLocalizer()
      .createWithRootKey("option", InternationalizationCoreKt.getRootLocalizer());
}
