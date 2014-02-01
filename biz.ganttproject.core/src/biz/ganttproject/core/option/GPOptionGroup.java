/*
 * Created on 02.04.2005
 */
package biz.ganttproject.core.option;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bard
 */
public class GPOptionGroup {
  private Map<String, String> myCanonicalKey_customKey;
  private final String myID;

  private final GPOption[] myOptions;

  private boolean isTitled = true;

  public GPOptionGroup(String id, GPOption... options) {
    myID = id;
    myOptions = options;
  }

  public String getID() {
    return myID;
  }

  public GPOption[] getOptions() {
    return myOptions;
  }

  public GPOption getOption(String optionID) {
    assert optionID != null;
    for (int i = 0; i < myOptions.length; i++) {
      if (myOptions[i].getID().equals(optionID)) {
        return myOptions[i];
      }
    }
    return null;
  }

  public void lock() {
  }

  public void commit() {
    for (int i = 0; i < myOptions.length; i++) {
      myOptions[i].commit();
    }
  }

  public void rollback() {
  }

  public boolean isTitled() {
    return isTitled;
  }

  public void setTitled(boolean isTitled) {
    this.isTitled = isTitled;
  }

  public void copyFrom(GPOptionGroup originalGroup) {
    if (!getID().equals(originalGroup.getID())) {
      throw new IllegalArgumentException("You can copy only identically structured option groups");
    }
    lock();
    try {
      Map<String, GPOption> id2option = new HashMap<String, GPOption>();
      for (int i = 0; i < myOptions.length; i++) {
        id2option.put(myOptions[i].getID(), myOptions[i]);
      }
      GPOption[] originals = originalGroup.getOptions();
      for (int i = 0; i < originals.length; i++) {
        GPOption copy = id2option.get(originals[i].getID());
        if (copy == null) {
          throw new IllegalStateException("Can't find option (id=" + originals[i].getID() + ") in my options");
        }
        copy.loadPersistentValue(originals[i].getPersistentValue());
      }
    } finally {
      commit();
    }
  }

  public String getI18Nkey(String canonicalKey) {
    return (String) (myCanonicalKey_customKey == null ? null : myCanonicalKey_customKey.get(canonicalKey));
  }

  public void setI18Nkey(String canonicalKey, String customKey) {
    if (myCanonicalKey_customKey == null) {
      myCanonicalKey_customKey = new HashMap<String, String>();
    }
    myCanonicalKey_customKey.put(canonicalKey, customKey);
  }
}
