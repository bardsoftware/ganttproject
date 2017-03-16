package biz.ganttproject.impex.google;

import biz.ganttproject.core.option.GPOptionGroup;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.options.OptionPageProviderBase;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class GoogleExportOptionPageProvider extends OptionPageProviderBase {

  public GoogleAuth myAuth = new GoogleAuth();

  public GoogleExportOptionPageProvider() {
    super("impex.google");
  }

  @Override
  public GPOptionGroup[] getOptionGroups() {
    return new GPOptionGroup[0];
  }

  @Override
  public boolean hasCustomComponent() {
    return true;
  }

  public Component buildPageComponent() {
      JPanel result = new JPanel(new BorderLayout());
      result.setBorder(new EmptyBorder(5, 5, 5, 5));
      JButton testConnectionButton = new JButton(new GPAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            myAuth.someSampleWork();
          } catch (Exception e1) {}
        }
      });
      result.add(testConnectionButton,BorderLayout.NORTH);
      return result;
    }
  }