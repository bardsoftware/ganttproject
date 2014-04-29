package biz.ganttproject.impex.csv;

import java.util.List;

import net.sourceforge.ganttproject.ResourceDefaultColumn;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;

import org.apache.commons.csv.CSVRecord;

import com.google.common.collect.ImmutableSet;
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
      Sets.union(
          Sets.newHashSet(GanttCSVOpen.getFieldNames(ResourceFields.values())),
          ImmutableSet.of(ResourceDefaultColumn.STANDARD_RATE.getName())),
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
    HumanResource hr = resourceManager.newResourceBuilder()
        .withName(getOrNull(record, ResourceFields.NAME.toString()))
        .withID(getOrNull(record, ResourceFields.ID.toString()))
        .withEmail(getOrNull(record, ResourceFields.EMAIL.toString()))
        .withPhone(getOrNull(record, ResourceFields.PHONE.toString()))
        .withRole(getOrNull(record, ResourceFields.ROLE.toString()))
        .withStandardRate(getOrNull(record, ResourceDefaultColumn.STANDARD_RATE.getName()))
        .build();
    for (String customField : getCustomFields()) {
      String value = getOrNull(record, customField);
      if (value != null) {
        hr.addCustomProperty(resourceManager.getCustomPropertyManager().getCustomPropertyDefinition(customField), value);
      }
    }
    return true;
  }
}