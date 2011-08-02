package net.sourceforge.ganttproject.gui.options.model;

import java.util.Date;

import net.sourceforge.ganttproject.CustomPropertyClass;
import net.sourceforge.ganttproject.CustomPropertyDefinition;

public abstract class CustomPropertyDefaultValueAdapter {
    public static GPOption createDefaultValueOption(final CustomPropertyClass propertyClass, final CustomPropertyDefinition def) {
        switch (propertyClass) {
        case TEXT:
            class TextDefaultValue extends DefaultStringOption {
                TextDefaultValue() {
                    super("customPropertyDialog.defaultValue.text", def.getDefaultValueAsString());
                }

                @Override
                public void commit() {
                    if (isChanged()) {
                        assert propertyClass == def.getPropertyClass();
                        super.commit();
                        def.setDefaultValueAsString(getValue());
                    }
                }

            }
            return new TextDefaultValue();
        case BOOLEAN:
            class BooleanDefaultValue extends DefaultBooleanOption {
                BooleanDefaultValue() {
                    super("customPropertyDialog.defaultValue.boolean",
                        def.getDefaultValue() == null ? Boolean.FALSE : (Boolean)def.getDefaultValue());
                }
                @Override
                public void commit() {
                    if (isChanged()) {
                        assert propertyClass == def.getPropertyClass()
                            : "property class mismatch: " + propertyClass + " vs " + def.getPropertyClass();
                        super.commit();
                        def.setDefaultValueAsString(String.valueOf(getValue()));
                    }
                }
            }
            return new BooleanDefaultValue();
        case INTEGER:
            class IntegerDefaultValue extends DefaultIntegerOption {
                IntegerDefaultValue() {
                    super("customPropertyDialog.defaultValue.integer", (Integer)def.getDefaultValue());
                }
                @Override
                public void commit() {
                    if (isChanged()) {
                        assert propertyClass == def.getPropertyClass();
                        super.commit();
                        def.setDefaultValueAsString(String.valueOf(getValue()));
                    }
                }
            }
            return new IntegerDefaultValue();
        case DOUBLE:
            class DoubleDefaultValue extends DefaultDoubleOption {
                DoubleDefaultValue() {
                    super("customPropertyDialog.defaultValue.double", (Double)def.getDefaultValue());
                }
                @Override
                public void commit() {
                    if (isChanged()) {
                        assert propertyClass == def.getPropertyClass();
                        super.commit();
                        def.setDefaultValueAsString(String.valueOf(getValue()));
                    }
                }
            }
            return new DoubleDefaultValue();
        case DATE:
            class DateDefaultValue extends DefaultDateOption {
                DateDefaultValue() {
                    super("customPropertyDialog.defaultValue.date", (Date)def.getDefaultValue());
                }
                @Override
                public void commit() {
                    if (isChanged()) {
                        assert propertyClass == def.getPropertyClass();
                        super.commit();
                        def.setDefaultValueAsString(String.valueOf(getValue()));
                    }
                }
            }
            return new DateDefaultValue();
        default:
            return null;
        }
    }
}