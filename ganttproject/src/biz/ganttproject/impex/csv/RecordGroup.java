package biz.ganttproject.impex.csv;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import net.sourceforge.ganttproject.GPLogger;

import org.apache.commons.csv.CSVRecord;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public abstract class RecordGroup {
  private final Set<String> myFields;
  private final Set<String> myMandatoryFields;
  private SetView<String> myCustomFields;
  private final String myName;

  public RecordGroup(String name, Set<String> fields) {
    myName = name;
    myFields = fields;
    myMandatoryFields = fields;
  }

  public RecordGroup(String name, Set<String> regularFields, Set<String> mandatoryFields) {
    myName = name;
    myFields = regularFields;
    myMandatoryFields = mandatoryFields;
  }

  boolean isHeader(CSVRecord record) {
    Set<String> thoseFields = Sets.newHashSet();
    for (Iterator<String> it = record.iterator(); it.hasNext();) {
      thoseFields.add(it.next());
    }
    return thoseFields.containsAll(myMandatoryFields);
  }

  boolean process(CSVRecord record) {
    assert record.size() > 0;
    boolean allEmpty = true;
    for (Iterator<String> it = record.iterator(); it.hasNext();) {
      if (!Strings.isNullOrEmpty(it.next())) {
        allEmpty = false;
        break;
      }
    }
    if (allEmpty) {
      return false;
    }
    try {
      return doProcess(record);
    } catch (Throwable e) {
      GPLogger.getLogger(GanttCSVOpen.class).log(Level.WARNING, String.format("Failed to process record:\n%s", record), e);
      return false;
    }
  }

  protected boolean hasMandatoryFields(CSVRecord record) {
    for (String s : myMandatoryFields) {
      if (Strings.isNullOrEmpty(record.get(s))) {
        return false;
      }
    }
    return true;
  }

  protected abstract boolean doProcess(CSVRecord record);

  protected void postProcess() {}

  public void setHeader(List<String> header) {
    myCustomFields = Sets.difference(Sets.newHashSet(header), myFields);
  }

  protected Collection<String> getCustomFields() {
    return myCustomFields;
  }

  @Override
  public String toString() {
    return myName;
  }
}