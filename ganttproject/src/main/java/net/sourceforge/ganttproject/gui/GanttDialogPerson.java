/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 GanttProject Team

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
package net.sourceforge.ganttproject.gui;

import biz.ganttproject.core.calendar.GanttDaysOff;
import biz.ganttproject.customproperty.CustomPropertyManager;
import com.google.common.collect.Lists;
import javafx.scene.control.Tab;
import kotlin.Unit;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.DateIntervalListEditor.DateInterval;
import net.sourceforge.ganttproject.gui.DateIntervalListEditor.DefaultDateIntervalModel;
import net.sourceforge.ganttproject.gui.resourceproperties.MainPropertiesPanel;
import net.sourceforge.ganttproject.gui.taskproperties.CustomColumnsPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.storage.ProjectDatabase;
import net.sourceforge.ganttproject.task.TaskManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class GanttDialogPerson {
  private static final GanttLanguage language = GanttLanguage.getInstance();

  private final TaskManager myTaskManager;
  private final ProjectDatabase myProjectDatabase;
  private final HumanResourceManager myResourceManager;
  private final HumanResource person;


  private final UIFacade myUIFacade;
  private final CustomPropertyManager myCustomPropertyManager;
  private final Runnable onHide;
  private ResourceAssignmentsPanel myAssignmentsPanel;
  private final MainPropertiesPanel mainPropertiesPanel;


  public GanttDialogPerson(HumanResourceManager resourceManager,
                           CustomPropertyManager customPropertyManager,
                           TaskManager taskManager,
                           ProjectDatabase projectDatabase,
                           UIFacade uiFacade,
                           HumanResource person,
                           Runnable onHide
                           ) {
    myResourceManager = resourceManager;
    myCustomPropertyManager = customPropertyManager;
    myTaskManager = taskManager;
    myUIFacade = uiFacade;
    myProjectDatabase = projectDatabase;
    this.person = person;
    mainPropertiesPanel = new MainPropertiesPanel(person);
    this.onHide = onHide;
  }

  public void setVisible(boolean isVisible) {
    if (isVisible) {
      OkAction okAction = new OkAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          okButtonActionPerformed();
          onHide.run();
          myUIFacade.getActiveChart().focus();
        }
      };
      CancelAction cancelAction = new CancelAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          onHide.run();
          myUIFacade.getActiveChart().focus();
        }
      };

      var actions = Lists.newArrayList(okAction, cancelAction);
      PropertiesDialogKt.propertiesDialog(
        language.getCorrectedLabel("human"),
        "resourceProperties", actions,
        mainPropertiesPanel.getValidationErrors(),
        Lists.newArrayList(
          new PropertiesDialogTabProvider(
            tabPane -> {
              tabPane.getTabs().add(new Tab(mainPropertiesPanel.getTitle(), mainPropertiesPanel.getFxComponent()));
              return Unit.INSTANCE;
            },
            () -> {
              mainPropertiesPanel.requestFocus();
              return Unit.INSTANCE;
            }
          ),
          PropertiesDialogKt.swingTab(
            language.getText("daysOff"),
            this::constructDaysOffPanel
          ),
          PropertiesDialogKt.swingTab(
            language.getText("customColumns"),
            () -> {
              var customColumnsPanel = new CustomColumnsPanel(myCustomPropertyManager, myProjectDatabase, CustomColumnsPanel.Type.RESOURCE,
                myUIFacade.getUndoManager(), person, myUIFacade.getResourceTree().getVisibleFields());
              return customColumnsPanel.getComponent();
            }
          ),
          PropertiesDialogKt.swingTab(
            language.getText("assignments"),
            () -> {
              constructAssignmentsPanel();
              return myAssignmentsPanel.getComponent();
            }
          )
        )
      );
    }
  }

  private void constructAssignmentsPanel() {
    myAssignmentsPanel = new ResourceAssignmentsPanel(person, myTaskManager);
  }

  private void okButtonActionPerformed() {
    if (person.getId() != -1) {
      // person ID is -1 when it is new one
      // i.e. before the Person dialog is closed
      myUIFacade.getUndoManager().undoableEdit("Resource properties changed", this::applyChanges);
    } else {
      myUIFacade.getUndoManager().undoableEdit(GanttLanguage.getInstance().formatText("resource.new.description"), () -> {
        applyChanges();
        myResourceManager.add(person);
//        myUIFacade.getResourceTree().setSelected(person, true);
        myUIFacade.getViewManager().getView(String.valueOf(UIFacade.RESOURCES_INDEX)).setActive(true);
      });
    }
  }

  private void applyChanges() {
    mainPropertiesPanel.save();
    person.getDaysOff().clear();
    for (DateInterval interval : myDaysOffModel.getIntervals()) {
      person.addDaysOff(new GanttDaysOff(interval.start, interval.getEnd()));
    }
    myAssignmentsPanel.commit();
  }

  private DefaultDateIntervalModel myDaysOffModel;

  public JPanel constructDaysOffPanel() {
    myDaysOffModel = new DateIntervalListEditor.DefaultDateIntervalModel() {
      @Override
      public int getMaxIntervalLength() {
        return 2;
      }

      @Override
      public void add(DateInterval interval) {
        super.add(interval);
      }

      @Override
      public void remove(DateInterval interval) {
        super.remove(interval);
      }
    };
    DefaultListModel<GanttDaysOff> daysOff = person.getDaysOff();
    for (int i = 0; i < daysOff.getSize(); i++) {
      GanttDaysOff next = daysOff.get(i);
      myDaysOffModel.add(DateIntervalListEditor.DateInterval.createFromModelDates(next.getStart().getTime(),
          next.getFinish().getTime()));
    }
    return new DateIntervalListEditor(myDaysOffModel);
  }
}
