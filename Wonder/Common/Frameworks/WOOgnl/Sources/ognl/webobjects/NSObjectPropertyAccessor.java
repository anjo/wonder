/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */

/* NSObjectPropertyAccessor.java created by max on Fri 28-Sep-2001 */
package ognl.webobjects;

import com.webobjects.foundation.*;
import ognl.PropertyAccessor;
import ognl.OgnlException;
import java.util.Map;

public class NSObjectPropertyAccessor implements PropertyAccessor {

    public Object getProperty(Map map, Object target, Object name) throws OgnlException {
        return getProperty(target, name);
    }
    
    public Object getProperty(Object target, Object name) throws OgnlException {
        Object property = null;
        //try {
            property = NSKeyValueCoding.Utility.valueForKey(target, (String)name);
        //} catch (Exception e) {
        //    throw new OgnlException(name.toString(), e);
        //}
        return property;
    }

    public void setProperty(Object target, Object name, Object value ) throws OgnlException {
        Object property = null;
        try {
            //if (target implements NSValidation)
            //    ((NSValidation)target).validateTakeValueForKeyPath(value, (String)name);
            //else
                NSKeyValueCoding.Utility.takeValueForKey(target, value, (String)name);
        } catch (Exception e) {
            throw new OgnlException(name.toString(), e);
        }
    }

    public void setProperty(Map map, Object target, Object name, Object value) throws OgnlException {
        setProperty(target, name, value);
    }
}
