/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2003-2012 GanttProject Team

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

package net.sourceforge.ganttproject.document.webdav;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.ganttproject.document.DocumentStorageUi.DocumentDescriptor;
import net.sourceforge.ganttproject.document.DocumentStorageUi.DocumentReceiver;
import net.sourceforge.ganttproject.document.webdav.WebDavResource.WebDavException;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.SpringUtilities;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueEvent;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueListener;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.IntegerOption;
import net.sourceforge.ganttproject.gui.options.model.StringOption;
import net.sourceforge.ganttproject.language.GanttLanguage;

import org.jdesktop.swingx.JXList;

/**
 * UI component for WebDAV operarions.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class GanttURLChooser {
  private static final GanttLanguage language = GanttLanguage.getInstance();

  private final StringOption myUrl;

  private final StringOption myUsername;

  private final StringOption myPassword;

  private final BooleanOption myLock;

  private final IntegerOption myTimeout;

  private SelectionListener mySelectionListener;

  static interface SelectionListener {
    public void setSelection(WebDavResource resource);
  }

  GanttURLChooser(String url, StringOption username, String password, IntegerOption lockTimeoutOption, final DocumentReceiver receiver) {
    myUrl = new DefaultStringOption("url", url);
    myUsername = username;
    myPassword = new DefaultStringOption("password", password);
    myPassword.setScreened(true);
    myLock = new DefaultBooleanOption("lock", true);
    myTimeout = lockTimeoutOption;

//    ChangeValueListener listener = new ChangeValueListener() {
//      @Override
//      public void changeValue(ChangeValueEvent event) {
//        receiver.setDocument(createDocument());
//      }
//    };
//    myUrl.addChangeValueListener(listener);
//    myUsername.addChangeValueListener(listener);
//    myPassword.addChangeValueListener(listener);
  }

  protected DocumentDescriptor createDocument() {
    return new DocumentDescriptor(myUrl.getValue(), myUsername.getValue(), myPassword.getValue());
  }

  public JComponent createOpenDocumentUi() {
    return createComponent();
  }

  public JComponent createSaveDocumentUi() {
    return createComponent();
  }

  private JComponent createComponent() {
    OptionsPageBuilder builder = new OptionsPageBuilder();
    final JComponent timeoutComponent = (JComponent) builder.createOptionComponent(null, myTimeout);

    JPanel panel = new JPanel(new SpringLayout());
    panel.add(new JLabel(language.getText("fileFromServer")));
    panel.add(builder.createOptionComponent(null, myUrl));
    panel.add(new JLabel(language.getText("userName")));
    panel.add(builder.createOptionComponent(null, myUsername));
    panel.add(new JLabel(language.getText("password")));
    panel.add(builder.createOptionComponent(null, myPassword));

    addEmptyRow(panel);

    {
      final FilesTableModel tableModel = new FilesTableModel();
      JPanel filesTablePanel = new JPanel(new BorderLayout());
      JButton refreshButton = new JButton(new AbstractAction("Refresh") {
        @Override
        public void actionPerformed(ActionEvent event) {
          try {
            WebDavResource resource = WebDavStorageImpl.createResource(myUrl.getValue(), myUsername.getValue(), myPassword.getValue());
            if (resource.exists() && resource.isCollection()) {
              tableModel.setCollection(resource);
              return;
            }
            WebDavResource parent = resource.getParent();
            if (parent.exists() && parent.isCollection()) {
              tableModel.setCollection(parent);
              return;
            }
          } catch (WebDavException e) {
            e.printStackTrace();
          }
        }
      });
      filesTablePanel.add(refreshButton, BorderLayout.NORTH);
      //panel.add(filesActionsPanel);
      final JXList table = new JXList(tableModel);
      table.setHighlighters(UIUtil.ZEBRA_HIGHLIGHTER);
      table.setCellRenderer(new FilesCellRenderer());
      table.addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          WebDavResource resource = (WebDavResource) table.getSelectedValue();
          mySelectionListener.setSelection(resource);
          myUrl.setValue(resource.getUrl());
        }
      });
      filesTablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
      panel.add(new JPanel());
      panel.add(filesTablePanel);
    }

    addEmptyRow(panel);
//    panel.add(new JLabel(language.getText("webdav.lockResource.label")));
//    panel.add(lockComponent);
    panel.add(new JLabel(language.getText("webdav.lockTimeout.label")));

    myLock.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        timeoutComponent.setEnabled(myLock.isChecked());
      }
    });
    panel.add(timeoutComponent);
    SpringUtilities.makeCompactGrid(panel, 7, 2, 0, 0, 10, 5);

    JPanel properties = new JPanel(new BorderLayout());
    properties.add(panel, BorderLayout.NORTH);
    properties.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));

    return properties;
  }

  private static void addEmptyRow(JPanel form) {
    form.add(Box.createRigidArea(new Dimension(1, 10)));
    form.add(Box.createRigidArea(new Dimension(1, 10)));
  }

  String getUrl() {
    return myUrl.getValue();
  }

  String getUsername() {
    return myUsername.getValue();
  }

  String getPassword() {
    return myPassword.getValue();
  }

  int getLockTimeout() {
    return myTimeout.getValue();
  }

  void setSelectionListener(SelectionListener selectionListener) {
    mySelectionListener = selectionListener;
  }

  void showError(WebDavException e) {
    // TODO Auto-generated method stub

  }
}
