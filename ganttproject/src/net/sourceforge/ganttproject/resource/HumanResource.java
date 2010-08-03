/*
 * HumanResource.java
 *
 * Created on 27. Mai 2003, 22:19
 */

package net.sourceforge.ganttproject.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.DefaultListModel;

import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyHolder;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.calendar.GanttDaysOff;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.ResourceAssignment;

/**
 * @author barmeier
 */
public class HumanResource extends ProjectResource implements CustomPropertyHolder {
	private boolean areEventsEnabled = true;
	
    private String phone = "";

    private String email = "";

    private int function;

    private Role myRole;

    private final DefaultListModel myDaysOffList = new DefaultListModel ();
    
    /* contains all the custom property values of a resource.
     * the key is the property name and the value is the property value */
    private final Map customFields;
    
    private final HumanResourceManager myManager;

    HumanResource(HumanResourceManager manager) {
        this.name = "";
        customFields = new HashMap();
        myManager = manager;
        // added
    }

    /** Creates a new instance of HumanResource */
    HumanResource(String name, int id, HumanResourceManager manager) {
    	super(id);
        this.name = name;
        customFields = new HashMap();
        myManager = manager;
    }

    void initCustomProperties(){
    	List/*<CustomPropertyDefinition>*/ defs = myManager.getCustomPropertyManager().getDefinitions();
    	for (int i=0; i<defs.size(); i++) {
    		CustomPropertyDefinition nextDefinition = (CustomPropertyDefinition) defs.get(i);
    		customFields.put(nextDefinition.getName(), nextDefinition.getDefaultValue());
    	}
    }
    private HumanResource(HumanResource copy) {
    	areEventsEnabled = false;
		setId(-1);
		setName(GanttLanguage.getInstance().getText("copy2")+"_"+copy.getName());
		setCostsPerUnit(copy.getCostsPerUnit());
		setDescription(copy.getDescription());
		setMaximumUnitsPerDay(copy.getMaximumUnitsPerDay());
		setUnitMeasure(copy.getUnitMeasure());
		setMail(copy.getMail());
		setPhone(copy.getPhone());
		setRole(copy.getRole());
		myManager = copy.myManager;
		DefaultListModel copyDaysOff = copy.getDaysOff();
		for (int i=0; i<copyDaysOff.getSize(); i++) {
			myDaysOffList.addElement(copyDaysOff.get(i));
		}
		customFields = new HashMap(copy.customFields);
		areEventsEnabled = true;
    }
 
    public void delete() {
        super.delete();
        myManager.remove(this);
    }

    public void setMail(String email) {
        if (email == null) {
            return;
        }
        this.email = email;
        fireResourceChanged();
    }

    public String getMail() {
        return email;
    }

    public void setPhone(String phone) {
        if (phone == null) {
            return;
        }
        this.phone = phone;
        fireResourceChanged();
    }

    public String getPhone() {
        return phone;
    }

    // public void setFunction (int function) {
    // this.function=function;
    // }

    // public int getFunction () {
    // return myRole==null ? 0 : myRole.getID();
    // }

    public void setRole(Role role) {
        myRole = role;
        fireResourceChanged();
    }

    public Role getRole() {
        if (myRole == null) {
            System.err
                    .println("[HumanResource] getRole(): I have no role :( name="
                            + getName());
        }
        return myRole;
    }

    public void addDaysOff(GanttDaysOff gdo) {
        myDaysOffList.addElement(gdo);
        fireResourceChanged();
    }

    public DefaultListModel getDaysOff() {
        return myDaysOffList;
    }
    
    
    public void addCustomField(String title, Object value) {
    	this.customFields.put(title,value);
    }
    
    public void removeCustomField(String title) {
    	this.customFields.remove(title);
    }
    
    public Object getCustomFieldVal(String title) {
    	return customFields.get(title);
    }
    
    public void setCustomFieldVal(String title, Object val) {
    	this.customFields.put(title,val);
    }

    public ResourceAssignment createAssignment(ResourceAssignment assignmentToTask) {
        ResourceAssignment result = super.createAssignment(assignmentToTask);
        fireAssignmentsChanged();
        return result;
    }    
    
    public ProjectResource unpluggedClone() {
    	return new HumanResource(this);
    }
    
    private void fireResourceChanged() {
    	if (areEventsEnabled) {
    		myManager.fireResourceChanged(this);
    	}
    }
    
    protected void fireAssignmentsChanged() {
        if (areEventsEnabled) {
            myManager.fireAssignmentsChanged(this);
        }        
    }

	public List/*<CustomProperty>*/ getCustomProperties() {
		List result = new ArrayList(customFields.size());
		for (Iterator entries = customFields.entrySet().iterator(); entries.hasNext();) {
			Map.Entry nextEntry = (Entry) entries.next();
			String nextName = (String) nextEntry.getKey();
			Object nextValue = nextEntry.getValue();
			CustomPropertyDefinition nextDefinition = myManager.getCustomPropertyDefinition(nextName);
			result.add(new CustomPropertyImpl(nextDefinition, nextValue));
		}
		return result;
	}
	
	public CustomProperty addCustomProperty(CustomPropertyDefinition definition, String valueAsString) {
		final CustomPropertyDefinition stubDefinition = CustomPropertyManager.PropertyTypeEncoder.decodeTypeAndDefaultValue(definition.getTypeAsString(), valueAsString);
		addCustomField(definition.getName(), stubDefinition.getDefaultValue());
		return new CustomPropertyImpl(definition, stubDefinition.getDefaultValue());
	}


	private static class CustomPropertyImpl implements CustomProperty {
		private CustomPropertyDefinition myDefinition;
		private Object myValue;

		public CustomPropertyImpl(CustomPropertyDefinition definition, Object value) {
			myDefinition = definition;
			myValue = value;
		}
		public CustomPropertyDefinition getDefinition() {
			return myDefinition;
		}

		public Object getValue() {
			return myValue;
		}

		public String getValueAsString() {
			return HumanResourceManager.getValueAsString(myValue);
		}
		
	}
}
