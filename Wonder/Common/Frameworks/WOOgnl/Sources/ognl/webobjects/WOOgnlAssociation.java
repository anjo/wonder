/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */

/* WOOgnlAssociation.java created by max on Fri 28-Sep-2001 */
package ognl.webobjects;

import com.webobjects.appserver.WOAssociation;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver._private.WOKeyValueAssociation;
import com.webobjects.eocontrol.EOEventCenter;
import com.webobjects.foundation.NSLog;
import com.webobjects.foundation.NSProperties;
import com.webobjects.foundation.NSPropertyListSerialization;

public class WOOgnlAssociation extends WOKeyValueAssociation {

    public WOOgnlAssociation(String s) {
        super(s);
    }

    public Object clone() { return new WOOgnlAssociation(keyPath()); }

    public Object valueInComponent(WOComponent component) {
        WOAssociation.Event event = _markStartOfEventIfNeeded("valueForKeyPath", keyPath(), component);
        Object value = null;
	try {
	    value = WOOgnl.factory().getValue(keyPath(), component);
	} catch(Exception e) {
            if(shouldThrowException())
                throw new RuntimeException( e );
	    NSLog.err.appendln("Exception invoking valueInComponent on WOOgnlAssociation with keyPath '" + keyPath() + "' "+ e );
	}
        if(event != null)
            EOEventCenter.markEndOfEvent(event);
        if(_debugEnabled)
            _logPullValue(value, component);
        return value;
    }

    public void setValue(Object object, WOComponent component) {
        WOAssociation.Event event = _markStartOfEventIfNeeded("takeValueForKeyPath", keyPath(), component);
        try {
            // not sure how to manage validation or whether the current implementation is enough...
            WOOgnl.factory().setValue(keyPath(), component, object);
        } catch(Exception e) {
            if(shouldThrowException())
                throw new RuntimeException( e );
            NSLog.err.appendln( "Exception invoking setValue on WOOgnlAssociation: "+ e );
        }
	if (event != null)
	    EOEventCenter.markEndOfEvent(event);
	if (_debugEnabled)
	    _logPushValue(object, component);        
    }

    private boolean shouldThrowException() {
        return NSPropertyListSerialization.booleanForString
        ( NSProperties.getProperty( "ognl.webobjects.WOAssociation.shouldThrowExceptions" ) );
    }
    
}
