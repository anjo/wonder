/*
 * WOAnyField.java
 * [JavaWOExtensions Project]
 */

package com.webobjects.woextensions;

import java.text.*;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.*;
import com.webobjects.foundation.*;

public class WOAnyField extends WOComponent {
    protected static String DEFAULT_DATE_FORMAT = "YYYY/MM/DD";
    protected static String DEFAULT_NUMBER_FORMAT = "0";

    private String _relationshipKey;
    private String _selectedKey;
    private String _selectedOperator;
    private Object _value;
    private String _textFieldValue;
    private WODisplayGroup _displayGroup;
    // ivars for PopUp
    public String selectedKeyItem;
    public String selectedOperatorItem;

    public WOAnyField(WOContext aContext)  {
        super(aContext);
    }

    public boolean isStateless() {
        return true;
    }

    public String relationshipKey() {
        if (_relationshipKey == null) {
            _relationshipKey = (String) _WOJExtensionsUtil.valueForBindingOrNull("relationshipKey", this);
        }
        return _relationshipKey;
    }

    public String selectedKey() {
        if (_selectedKey == null) {
            _selectedKey = (String) _WOJExtensionsUtil.valueForBindingOrNull("selectedKey", this);
        }
        return _selectedKey;
    }

    public void setSelectedKey(String key) {
        _selectedKey = key;
    }

    public String valueClassNameForKey(String key) {
        String entityName = (String) _WOJExtensionsUtil.valueForBindingOrNull("sourceEntityName", this);
        EOModelGroup modelGroup = EOModelGroup.defaultGroup();
        EOEntity entity = modelGroup.entityNamed(entityName);
        EOAttribute selectedAttribute = null;
        if (relationshipKey() != null) {
            EORelationship relationship = entity.relationshipNamed(relationshipKey());
            EOEntity destinationEntity = relationship.destinationEntity();
            selectedAttribute = destinationEntity.attributeNamed(key);
        } else {
            selectedAttribute = entity.attributeNamed(key);
        }
        return selectedAttribute.className();
    }

    public String formatterForKey(String key) {
        String formatter = null;
        if (hasBinding("formatter")) {
            setValueForBinding(key, "key");
            formatter = (String) _WOJExtensionsUtil.valueForBindingOrNull("formatter", this);
        }
        if (formatter == null) {
            String className = valueClassNameForKey(key);
            if (className.equals("com.webobjects.foundation.NSTimestamp")) {
                formatter = DEFAULT_DATE_FORMAT;
            } else if (className.equals("java.lang.Number") || className.equals("java.math.BigDecimal")) {
                formatter = DEFAULT_NUMBER_FORMAT;
            }
        }
        return formatter;
    }

    public WODisplayGroup displayGroup() {
        if (_displayGroup == null) {
            _displayGroup = (WODisplayGroup) _WOJExtensionsUtil.valueForBindingOrNull("displayGroup", this);
        }
        return _displayGroup;
    }

    public String selectedOperator() {
        return _selectedOperator;
    }

    public void setSelectedOperator(String anOperator) {
        _selectedOperator = (anOperator.equals("=")) ? "": anOperator;
    }

    public Object value() {
        if (_value == null) {
            _value = _WOJExtensionsUtil.valueForBindingOrNull("value", this);
        }
        return _value;
    }

    public void setValue(Object newValue) {
        _value = newValue;
        WODisplayGroup displayGroup = displayGroup();
        if (displayGroup != null) {
            displayGroup.queryMatch().removeAllObjects();
            if (relationshipKey() != null) {
                displayGroup.queryMatch().takeValueForKey(newValue, relationshipKey() + "." + selectedKey());
                if (newValue != null) {
                    displayGroup.queryOperator().takeValueForKey(selectedOperator(), relationshipKey() + "." + selectedKey());
                }
            } else {
                displayGroup.queryMatch().takeValueForKey(newValue, selectedKey());
                if (newValue != null) {
                    displayGroup.queryOperator().takeValueForKey(selectedOperator(), selectedKey());
                }
            }
        }
    }

    public String textFieldValue() {
        if (_textFieldValue != null) {
            return _textFieldValue;
        }
        Object value = value();
        setValue(value);
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else {
            java.text.Format formatter = null;
            String className = valueClassNameForKey(selectedKey());
            if (className.equals("com.webobjects.foundation.NSTimestamp")) {
                String dateFormatterString = formatterForKey(selectedKey());
                formatter = new NSTimestampFormatter(dateFormatterString);
            } else if (className.equals("java.lang.Number") || className.equals("java.math.BigDecimal")) {
                String numberFormatterString = formatterForKey(selectedKey());
                formatter = new NSNumberFormatter(numberFormatterString);
            }
            return (formatter != null) ? formatter.format(value) : value.toString();
        }
    }

    public void setTextFieldValue(String value) {
        String className = valueClassNameForKey(selectedKey());
        if (className.equals("com.webobjects.foundation.NSTimestamp")) {
            String dateFormatterString = formatterForKey(selectedKey());
            NSTimestampFormatter dateFormatter = new NSTimestampFormatter(dateFormatterString);
            Object objectValue = null;
            try {
                objectValue = dateFormatter.parseObject((value != null) ? value.toString() : "");
            } catch (ParseException e) {
                if (NSLog.debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelDetailed, NSLog.DebugGroupWebObjects)) {
                    NSLog.debug.appendln(e);
                }
            }
            setValue(objectValue);
        } else if (className.equals("java.lang.Number") || className.equals("java.math.BigDecimal")) {
            String numberFormatterString = formatterForKey(selectedKey());
            NSNumberFormatter numberFormatter = new NSNumberFormatter(numberFormatterString);
            Object objectValue = null;
            try {
                objectValue = numberFormatter.parseObject((value != null) ? value.toString() : "");
            } catch (ParseException e) {
                if (NSLog.debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelDetailed, NSLog.DebugGroupWebObjects)) {
                    NSLog.debug.appendln(e);
                }
            }
            setValue(objectValue);
        } else {
            // Assume String
            setValue(value);
        }
    }

    public void invalidateCaches() {
        // In order for this to behave like an element, all instance
        // variables need to be flushed before this components is used again
        // so that it will pull via association.
        _relationshipKey = null;
        _selectedKey = null;
        _selectedOperator = null;
        _value = null;
        _textFieldValue = null;
        _displayGroup = null;
    }

    public void finalize() throws Throwable {
        super.finalize();
        invalidateCaches();
    }

    public void reset() {
        invalidateCaches();
    }
}
