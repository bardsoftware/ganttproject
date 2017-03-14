package biz.ganttproject.impex.google;

import biz.ganttproject.core.option.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.OptionPageProviderBase;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.language.GanttLanguage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class GoogleExportOptionPageProvider extends OptionPageProviderBase {

  public GoogleAuth googleAuth = new GoogleAuth();

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
    final Action authAction = new AbstractAction() {
      {
        putValue(Action.NAME, GanttLanguage.getInstance().getText("googleAuth"));
      }

      @Override
      public void actionPerformed(ActionEvent ev) {
        try {
          googleAuth.someSampleWork();
        } catch(IOException e) {getUiFacade().showErrorDialog(e);};
      }
    };

      JPanel result = new JPanel(new BorderLayout());
      result.setBorder(new EmptyBorder(5, 5, 5, 5));
      JButton testConnectionButton = new JButton(authAction);
      result.add(testConnectionButton,BorderLayout.NORTH);
      return result;
    }
  }



