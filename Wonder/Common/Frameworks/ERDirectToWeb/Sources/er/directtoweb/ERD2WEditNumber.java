/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.appserver.*;
import com.webobjects.directtoweb.*;
import java.math.BigDecimal;
import java.text.ParseException;
import er.extensions.*;

/**
 * Common superclass of all ER's edit number components.<br />
 * 
 */

public class ERD2WEditNumber extends D2WEditNumber {

    public ERD2WEditNumber(WOContext context) { super(context); }
    
    /** Logging support */
    public final static ERXLogger log = ERXLogger.getERXLogger(ERD2WEditNumber.class);

    public void reset() {
        super.reset();
        _numberFormatter = null;
    }

    public EOAttribute attribute() {
        return super.attribute() != null ? super.attribute() : (EOAttribute)d2wContext().valueForKey("smartAttribute");
    }
    
    private ERXNumberFormatter _numberFormatter;
    protected java.text.Format numberFormatter() {
        if (_numberFormatter == null) {
            _numberFormatter = ERXNumberFormatter.sharedInstance();
            _numberFormatter.setPattern(formatter());
        }
        return (java.text.Format)_numberFormatter;
    }

    public Object validateTakeValueForKeyPath (Object anObject, String aPath) throws NSValidation.ValidationException {
        Number number = null;
        try {
            if (anObject instanceof String) {
                number = (Number)numberFormatValueForString((String)anObject);
            } else if (anObject!=null && !(anObject instanceof Number)) {
                log.warn("Unable to read number: " + anObject);
                throw ERXValidationFactory.defaultFactory().createException(object(), propertyKey(), anObject, "NotANumberException");
            }
        } catch(NSValidation.ValidationException ex) {
            //validationFailedWithException(ex, anObject, propertyKey());
            throw ex;
        }
        return super.validateTakeValueForKeyPath(convertNumber(number), propertyKey());
    }
    public void validationFailedWithException(Throwable theException,Object theValue, String theKeyPath)  {
        // This is for number formatting exceptions
        String keyPath = theKeyPath.equals("stringValue") ? propertyKey() : theKeyPath;
        Number formatValue = null;
        try {
        	if (theValue != null)
        		formatValue = (Number) numberFormatter().parseObject((String) theValue);
        } catch(Exception ex) {
        	formatValue = (Number)objectPropertyValue();
        }
        parent().validationFailedWithException(theException, formatValue, keyPath);
    }

    /* following needed because we do not want to leave control over our (light) numberFormatter to WebObjects, which needs a full fledged NSNumberFormatter */
    public String stringValue() {
        return value() != null ? numberFormatter().format(value()) : null;
    }
    public void setStringValue(String newStringValue) {
        if (newStringValue != null)
            setValue(numberFormatValueForString(newStringValue));
        else
            setValue(null);
    }
    protected Object numberFormatValueForString(String value) {
        Object formatValue = null;
        try {
            if (value != null)
                formatValue = numberFormatter().parseObject(value);
        } catch (ParseException e) {
            log.debug("Unable to parse number: " + value + " in " + propertyKey());
            throw ERXValidationFactory.defaultFactory().createException(object(), propertyKey(), value, "IllegalCharacterInNumberException");
        }
        return formatValue;
    }
    protected Object convertNumber(Object anObject) {
        Number newValue=null;
        if (anObject!=null && anObject instanceof Number) {
            newValue=(Number)anObject;
            if (newValue instanceof BigDecimal && !isDecimalNumber() && attribute().valueType() != null && attribute().valueType().equals("i")) {
                // we are getting a BigDecimal from WOTextField even though we asked for an Integer!
                newValue=ERXConstant.integerForInt(newValue.intValue());
            }
        }
        return newValue;
    }
}
