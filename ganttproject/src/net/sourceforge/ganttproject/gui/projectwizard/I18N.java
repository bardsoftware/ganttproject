package net.sourceforge.ganttproject.gui.projectwizard;

import java.text.MessageFormat;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleSet;

public class I18N {
    public I18N() {
        myDayNames = new String[7];
        for (int i = 0; i < 7; i++) {
            myDayNames[i] = GanttLanguage.getInstance().getDay(i);
        }
    }

    public String getNewProjectWizardWindowTitle() {
        return GanttLanguage.getInstance().getText("createNewProject");
    }

    public String getProjectDomainPageTitle() {
        return GanttLanguage.getInstance().getText("selectProjectDomain");
    }

    public String getProjectWeekendPageTitle() {
        return GanttLanguage.getInstance().getText("selectProjectWeekend");
    }

    public String getRolesetTooltipHeader(String roleSetName) {
        return MessageFormat.format("<html><body><h3>{0}</h3><ul>",
                (Object[]) new String[] { roleSetName });
    }

    public String getRolesetTooltipFooter() {
        return "</ul></body></html>";
    }

    public String formatRoleForTooltip(Role role) {
        return MessageFormat.format("<li>{0}</li>",
                (Object[]) new String[] { role.getName() });
    }

    String[] getDayNames() {
        return myDayNames;
        // DateFormatSymbols symbols = new
        // DateFormatSymbols(Locale.getDefault());
        // return symbols.getWeekdays();
    }

    final String[] myDayNames;

    public String getRoleSetDisplayName(RoleSet roleSet) {
        return GanttLanguage.getInstance().getText(
                "roleSet." + roleSet.getName() + ".displayName");
    }
}
