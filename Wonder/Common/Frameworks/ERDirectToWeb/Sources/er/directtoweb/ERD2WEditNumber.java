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

    public Object convertNumber(Object anObject) {
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

    public Object validateTakeValueForKeyPath (Object anObject, String aPath) throws com.webobjects.foundation.NSValidation.ValidationException {
        Number number = null;
        if (anObject instanceof String) {
            try {
                number = (Number)numberFormatter().parseObject((String)anObject);
            } catch (Exception e) {
                log.warn("Unable to parse number: " + anObject + " + " + propertyKey());
                throw ERXValidationFactory.defaultFactory().createException(object(), propertyKey(), anObject, "IllegalCharacterInNumberException");
            }
        } else if (anObject!=null && !(anObject instanceof Number)) {
            log.warn("Unable to read number: " + anObject);
            throw ERXValidationFactory.defaultFactory().createException(object(), propertyKey(), anObject, "NotANumberException");
        }
        return super.validateTakeValueForKeyPath(convertNumber(number), aPath);
    }
/*
    public void validationFailedWithException(Throwable theException,Object theValue, String theKeyPath) {
        // This is for number formatting exceptions
        String keyPath = theKeyPath.equals("stringValue") ? propertyKey() : theKeyPath;
        parent().validationFailedWithException(theException, convertNumber(numberFormatValueForString((String)theValue)), keyPath);
    }
*/
    /* following needed because we do not want to leave control over our (light) numberFormatter to WebObjects, which needs a full fledged NSNumberFormatter */
    public String stringValue() {
        String stringValue;
        try {
            stringValue = value() != null ? numberFormatter().format(value()) : null;
        } catch (Exception e) {
            throw new RuntimeException("A formatting error occured: " + e);
        }
        return stringValue;
    }
    public void setStringValue(String newStringValue) {
            if (newStringValue != null)
                setValue(numberFormatValueForString(newStringValue));
            else
                setValue(null);
    }

    public Object numberFormatValueForString(String value) {
        Object formatValue = null;
        try {
            if (value != null)
                formatValue = numberFormatter().parseObject(value);
        } catch (Exception e) {
            throw new RuntimeException("A formatting error occured: " + e);
        }
        return formatValue;
    }
}
