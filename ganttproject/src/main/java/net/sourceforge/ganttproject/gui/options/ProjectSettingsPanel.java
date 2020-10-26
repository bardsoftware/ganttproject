/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Alexandre Thomas, GanttProject Team

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
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.GanttDialogInfo;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.BrowserControl;

/**
 * Panel to edit the project properties
 *
 * @author athomas
 */
public class ProjectSettingsPanel {

  private final JTextField tfName;

  private final JTextField tfOrganization;

  private final JTextField tfWebLink;

  private final JTextArea taDescr;

  private final IGanttProject myProject;

  private JPanel myComponent;

  private static final GanttLanguage language = GanttLanguage.getInstance();

  public ProjectSettingsPanel(IGanttProject project) {
    myProject = project;
    Box vbproject = Box.createVerticalBox();

    JPanel namePanel = new JPanel(new BorderLayout());
    namePanel.add(new JLabel(language.getText("name")), BorderLayout.WEST);
    vbproject.add(namePanel);
    vbproject.add(tfName = new JTextField());
    vbproject.add(new JPanel());

    JPanel orgaPanel = new JPanel(new BorderLayout());
    orgaPanel.add(new JLabel(language.getText("organization")), BorderLayout.WEST);
    vbproject.add(orgaPanel);
    vbproject.add(tfOrganization = new JTextField());
    vbproject.add(new JPanel());

    tfWebLink = new JTextField();
    JButton bWeb = new TestGanttRolloverButton(new ImageIcon(getClass().getResource("/icons/web_16.gif")));
    bWeb.setToolTipText(GanttProject.getToolTip(language.getText("openWebLink")));
    bWeb.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // link to open the web link
        if (!BrowserControl.displayURL(tfWebLink.getText())) {
          GanttDialogInfo gdi = new GanttDialogInfo(null, GanttDialogInfo.ERROR, GanttDialogInfo.YES_OPTION,
              language.getText("msg4"), language.getText("error"));
          gdi.setVisible(true);
        }
      }
    });

    JPanel webPanel = new JPanel(new BorderLayout());
    webPanel.add(new JLabel(language.getText("webLink")), BorderLayout.WEST);
    webPanel.add(bWeb, BorderLayout.EAST);
    vbproject.add(webPanel);
    vbproject.add(tfWebLink);
    vbproject.add(new JPanel());

    JPanel descrPanel = new JPanel(new BorderLayout());
    descrPanel.add(new JLabel(language.getText("shortDescription")), BorderLayout.WEST);
    vbproject.add(descrPanel);
    taDescr = new JTextArea(12, 25);
    taDescr.setLineWrap(true);
    taDescr.setWrapStyleWord(true);
    JScrollPane scpArea = new JScrollPane(taDescr);
    vbproject.add(scpArea);

    JPanel projectPanel = new JPanel(new BorderLayout());
    projectPanel.add(vbproject, BorderLayout.NORTH);

    myComponent = projectPanel;
  }

  public boolean applyChanges(boolean askForApply) {
    boolean hasChange;
    if (myProject.getProjectName().equals(tfName.getText())
        && myProject.getOrganization().equals(tfOrganization.getText())
        && myProject.getWebLink().equals(tfWebLink.getText()) && myProject.getDescription().equals(taDescr.getText())) {
      hasChange = false;
    } else {
      hasChange = true;
      // apply changes if user clicked apply (or warn about pending changes and
      // ask whether to apply or not)
      myProject.setProjectName(getProjectName());
      myProject.setDescription(getProjectDescription());
      myProject.setOrganization(getProjectOrganization());
      myProject.setWebLink(getWebLink());
    }
    return hasChange;
  }

  public void initialize() {
    tfName.setText(myProject.getProjectName());
    tfOrganization.setText(myProject.getOrganization());
    tfWebLink.setText(myProject.getWebLink());
    taDescr.setText(myProject.getDescription());
  }

  /** @return the selected project name */
  public String getProjectName() {
    return tfName.getText();
  }

  /** @return the organization */
  public String getProjectOrganization() {
    return tfOrganization.getText();
  }

  /** @return the web link */
  public String getWebLink() {
    return tfWebLink.getText();
  }

  /** @return the project description */
  public String getProjectDescription() {
    return taDescr.getText();
  }

  public String getTitle() {
    return language.correctLabel(language.getText("project"));
  }

  public String getComment() {
    return language.getText("settingsProject");
  }

  public JComponent getComponent() {
    return myComponent;
  }
}
