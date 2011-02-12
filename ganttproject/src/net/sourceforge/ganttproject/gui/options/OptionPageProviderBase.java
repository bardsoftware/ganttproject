package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;
import net.sourceforge.ganttproject.language.GanttLanguage;

public abstract class OptionPageProviderBase implements OptionPageProvider {
    private String myPageID;
    private IGanttProject myProject;
    private UIFacade myUiFacade;

    protected OptionPageProviderBase(String pageID) {
        myPageID = pageID;
    }

    public String getPageID() {
        return myPageID;
    }

    public boolean hasCustomComponent() {
        return false;
    }

    public Component buildPageComponent() {
        return null;
    }

    public void init(IGanttProject project, UIFacade uiFacade) {
        myProject = project;
        myUiFacade = uiFacade;
    }

    public void commit() {
        for (GPOptionGroup optionGroup : getOptionGroups()) {
            optionGroup.commit();
        }
    }

    public abstract GPOptionGroup[] getOptionGroups();

    protected IGanttProject getProject() {
        return myProject;
    }

    protected UIFacade getUiFacade() {
        return myUiFacade;
    }

    @Override
    public String toString() {
        return GanttLanguage.getInstance().getText(new OptionsPageBuilder.I18N().getCanonicalOptionPageLabelKey(getPageID()));
    }

    protected static JPanel wrapContentComponent(JComponent contentComponent, String title, String description) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(new TopPanel(title, description), BorderLayout.NORTH);
        contentComponent.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 0, 0, 0), contentComponent.getBorder()));
        panel.add(contentComponent, BorderLayout.CENTER);

        JPanel result = new JPanel(new BorderLayout());
        result.add(panel, BorderLayout.NORTH);
        return result;
    }

    protected String getCanonicalPageTitle() {
        return GanttLanguage.getInstance().getText(
            new OptionsPageBuilder.I18N().getCanonicalOptionPageTitleKey(getPageID()));
    }
}
