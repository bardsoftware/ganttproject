package biz.ganttproject.impex.csv

import net.sourceforge.ganttproject.CustomPropertyDefinition
import net.sourceforge.ganttproject.CustomPropertyManager
import net.sourceforge.ganttproject.GPLogger

internal fun readCustomProperties(
    names: Iterable<String>,
    record: SpreadsheetRecord,
    customPropertyMgr: CustomPropertyManager,
    receiver: (CustomPropertyDefinition, String?) -> Unit) {
  for (customField in names) {
    val def = customPropertyMgr.let {
      it.getCustomPropertyDefinition(customField)
        ?: record.getType(customField)?.let { type ->
          it.createDefinition(customField, type.id, customField, null)
        }
    }
    if (def == null) {
      GPLogger.logToLogger("Can't find custom field with name=$customField value=${record[customField]}")
      continue
    }

    receiver(def, record[customField])
  }

}
