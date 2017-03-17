package biz.ganttproject.impex.google;

import biz.ganttproject.core.option.GPOptionGroup;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.options.OptionPageProviderBase;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.language.GanttLanguage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.logging.Level;

public class GoogleExportOptionPageProvider extends OptionPageProviderBase {

  private GoogleAuth myAuth = new GoogleAuth();

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
    JButton testConnectionButton = new JButton(new GPAction("googleConnect") {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          myAuth.someSampleWork(myAuth.getCalendarService(myAuth.authorize()));
        } catch (Exception e1) {
          GPLogger.getLogger(GoogleExportOptionPageProvider.class).log(Level.WARNING, "Something went wrong", e1);
        }
      }
    });
    result.add(testConnectionButton, BorderLayout.SOUTH);
    return result;
  }
}