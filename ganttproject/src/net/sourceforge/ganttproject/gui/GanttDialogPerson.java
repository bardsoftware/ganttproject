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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.calendar.GanttDaysOff;
import net.sourceforge.ganttproject.gui.DateIntervalListEditor.DateInterval;
import net.sourceforge.ganttproject.gui.DateIntervalListEditor.DefaultDateIntervalModel;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.StringOption;
import net.sourceforge.ganttproject.gui.taskproperties.CustomColumnsPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;

public class GanttDialogPerson {
    private boolean change;

    private HumanResource person;

    private static final GanttLanguage language = GanttLanguage.getInstance();

    private JTabbedPane tabbedPane;

    private final StringOption myNameField = new DefaultStringOption("name");
    private final StringOption myPhoneField = new DefaultStringOption("colPhone");
    private final StringOption myMailField = new DefaultStringOption("colMail");
    private final EnumerationOption myRoleField;
    private final GPOptionGroup myGroup;
    private final UIFacade myUIFacade;
    private final CustomPropertyManager myCustomPropertyManager;

    public GanttDialogPerson(CustomPropertyManager customPropertyManager, UIFacade uiFacade, HumanResource person) {
        myCustomPropertyManager = customPropertyManager;
        myUIFacade = uiFacade;
        this.person = person;
        Role[] enabledRoles = RoleManager.Access.getInstance().getEnabledRoles();
        String[] roleFieldValues = new String[enabledRoles.length];
        for (int i=0; i<enabledRoles.length; i++) {
            roleFieldValues[i]= enabledRoles[i].getName();
        }
        myRoleField = new DefaultEnumerationOption("colRole", roleFieldValues);
        myGroup = new GPOptionGroup("", new GPOption[] {myNameField, myPhoneField, myMailField,myRoleField});
        myGroup.setTitled(false);
    }

    public boolean result() {
        return change;
    }

    public void setVisible(boolean isVisible) {
        if (isVisible) {
            loadFields();
            Component contentPane = getComponent();
            OkAction okAction = new OkAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    myGroup.commit();
                    okButtonActionPerformed(e);
                }
            };
            CancelAction cancelAction = new CancelAction(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    myGroup.rollback();
                    change = false;
                }
            };
            myUIFacade.createDialog(contentPane, new Action[] {okAction, cancelAction}, language.getText("human")).show();
        }
    }

    private void loadFields() {
        myGroup.lock();
        myNameField.setValue(person.getName());
        myPhoneField.setValue(person.getPhone());
        myMailField.setValue(person.getMail());
        Role role = person.getRole();
        if (role!=null) {
            myRoleField.setValue(role.getName());
        }
        myGroup.commit();
        myGroup.lock();
    }

    private Component getComponent() {
        OptionsPageBuilder builder = new OptionsPageBuilder();
        OptionsPageBuilder.I18N i18n = new OptionsPageBuilder.I18N() {
            @Override
            public String getOptionLabel(GPOptionGroup group, GPOption option) {
                return getValue(option.getID());
            }
        };
        builder.setI18N(i18n);
        final JComponent mainPage = builder.buildPlanePage(new GPOptionGroup[] {myGroup});
        mainPage.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(language.getText("general"), new ImageIcon(getClass()
                .getResource("/icons/properties_16.gif")), mainPage);
        tabbedPane.addTab(language.getText("daysOff"), new ImageIcon(getClass()
                .getResource("/icons/holidays_16.gif")),
                constructDaysOffPanel());
        CustomColumnsPanel customColumnsPanel = new CustomColumnsPanel(
                myCustomPropertyManager, myUIFacade, person, myUIFacade.getResourceTree().getVisibleFields());
        tabbedPane.addTab(language.getText("customColumns"), new ImageIcon(getClass().getResource("/icons/custom.gif")),
                customColumnsPanel.getComponent());
        //mainPage.requestDefaultFocus();
//        final FocusTraversalPolicy defaultPolicy = mainPage.getFocusTraversalPolicy();
//        FocusTraversalPolicy customPolicy = new FocusTraversalPolicy() {
//            public Component getComponentAfter(Container aContainer, Component aComponent) {
//                return defaultPolicy.getComponentAfter(aContainer, aComponent);
//            }
//
//            public Component getComponentBefore(Container aContainer, Component aComponent) {
//                return defaultPolicy.getComponentBefore(aContainer, aComponent);
//            }
//
//            public Component getFirstComponent(Container aContainer) {
//                return defaultPolicy.getFirstComponent(aContainer);
//            }
//
//            public Component getLastComponent(Container aContainer) {
//                return defaultPolicy.getLastComponent(aContainer);
//            }
//
//            public Component getDefaultComponent(Container aContainer) {
//                return mainPage;
//            }
//        };
//        //mainPage.setFocusCycleRoot(true);
//        mainPage.setFocusTraversalPolicy(customPolicy);
        tabbedPane.addFocusListener(new FocusAdapter() {
            boolean isFirstTime = true;
            @Override
            public void focusGained(FocusEvent e) {
                if (isFirstTime) {
                    mainPage.requestFocus();
                    isFirstTime = false;
                }
                super.focusGained(e);
            }

        });
        return tabbedPane;
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_okButtonActionPerformed
        if (person.getId() != -1) {// person ID is -1 when it is new one
            // i.e. before the Person dialog is
            // closed
            myUIFacade.getUndoManager().undoableEdit(
                    "Resource properties changed", new Runnable() {
                        @Override
                        public void run() {
                            applyChanges();
                        }
                    });
        }
        else {
            applyChanges();
        }
        change = true;
    }

    private void applyChanges() {
        person.setName(myNameField.getValue());
        person.setMail(myMailField.getValue());
        person.setPhone(myPhoneField.getValue());
        Role role = findRole(myRoleField.getValue());
        if (role!=null) {
            person.setRole(role);
        }
        DateInterval[] intervals = myDaysOffModel.getIntervals();
        person.getDaysOff().clear();
        for (int i=0; i<intervals.length; i++) {
            person.addDaysOff(new GanttDaysOff(intervals[i].start, intervals[i].getEnd()));
        }
    }

    private Role findRole(String roleName) {
        Role[] enabledRoles = RoleManager.Access.getInstance().getEnabledRoles();
        for (int i=0; i<enabledRoles.length; i++) {
            if (enabledRoles[i].getName().equals(roleName)) {
                return enabledRoles[i];
            }
        }
        return null;
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
        DefaultListModel daysOff = person.getDaysOff();
        for (int i=0; i<daysOff.getSize(); i++) {
            GanttDaysOff next = (GanttDaysOff) daysOff.get(i);
            myDaysOffModel.add(DateIntervalListEditor.DateInterval.createFromModelDates(next.getStart().getTime(), next.getFinish().getTime()));
        }
        DateIntervalListEditor editor = new DateIntervalListEditor(myDaysOffModel);
        return editor;
    }
}
