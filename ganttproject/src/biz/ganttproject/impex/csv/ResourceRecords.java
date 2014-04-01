package biz.ganttproject.impex.csv;

import java.util.List;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;

import org.apache.commons.csv.CSVRecord;

import com.google.common.collect.Sets;

class ResourceRecords extends RecordGroup {
  public enum ResourceFields {
    ID("tableColID"), NAME("tableColResourceName"), EMAIL("tableColResourceEMail"), PHONE("tableColResourcePhone"), ROLE("tableColResourceRole");

    private final String text;

    private ResourceFields(final String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      // Return translated field name
      return GanttLanguage.getInstance().getText(text);
    }
  }

  private final HumanResourceManager resourceManager;

  ResourceRecords(HumanResourceManager resourceManager) {
    super("Resource group",
      Sets.newHashSet(GanttCSVOpen.getFieldNames(ResourceFields.values())),
      Sets.newHashSet(GanttCSVOpen.getFieldNames(ResourceFields.ID, ResourceFields.NAME)));
    this.resourceManager = resourceManager;
  }
  @Override
  public void setHeader(List<String> header) {
    super.setHeader(header);
    GanttCSVOpen.createCustomProperties(getCustomFields(), resourceManager.getCustomPropertyManager());
  }

  @Override
  protected boolean doProcess(CSVRecord record) {
    if (!hasMandatoryFields(record)) {
      return false;
    }
    assert record.size() > 0;
    HumanResource hr = resourceManager.newResourceBuilder().withName(record.get(ResourceFields.NAME.toString())).withID(
        record.get(ResourceFields.ID.toString())).withEmail(record.get(ResourceFields.EMAIL.toString())).withPhone(
        record.get(ResourceFields.PHONE.toString())).withRole(record.get(ResourceFields.ROLE.toString())).build();
    for (String customField : getCustomFields()) {
      String value = record.get(customField);
      if (value != null) {
        hr.addCustomProperty(resourceManager.getCustomPropertyManager().getCustomPropertyDefinition(customField), value);
      }
    }
    return true;
  }
}