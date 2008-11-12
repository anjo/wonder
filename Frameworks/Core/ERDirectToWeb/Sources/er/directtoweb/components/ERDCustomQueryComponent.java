/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr 
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb.components;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODisplayGroup;

import er.extensions.validation.ERXExceptionHolder;

/**
 * Superclass for most custom query components. <br />
 *  
 */

public class ERDCustomQueryComponent extends ERDCustomComponent implements ERXExceptionHolder {

    /** interface for all the keys used in this pages code */
    public static interface Keys {

        public static final String displayGroup = "displayGroup";
    }

    /** logging support */
    public final static Logger log = Logger.getLogger(ERDCustomQueryComponent.class);

    public ERDCustomQueryComponent(WOContext context) {
        super(context);
    }

    private WODisplayGroup _displayGroup;

    public WODisplayGroup displayGroup() {
        if (_displayGroup == null && !synchronizesVariablesWithBindings())
                _displayGroup = (WODisplayGroup) super.valueForBinding(Keys.displayGroup);
        return _displayGroup;
    }

    public void setDisplayGroup(WODisplayGroup value) {
        _displayGroup = value;
    }

    public Object displayGroupQueryMatchValue() {
        return key() != null && displayGroup() != null ? displayGroup().queryMatch().objectForKey(key()) : null;
    }

    public void setDisplayGroupQueryMatchValue(Object newValue) {
        if (key() != null && displayGroup() != null && displayGroup().queryMatch() != null) {
        	if(newValue != null) {
        		displayGroup().queryMatch().setObjectForKey(newValue, key());
        	} else {
                displayGroup().queryMatch().removeObjectForKey(key());
                displayGroup().queryOperator().removeObjectForKey(key());
            }
        }
    }

    public void reset() {
        super.reset();
        _displayGroup = null;
    }
}