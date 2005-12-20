/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.appserver.*;
import com.webobjects.directtoweb.*;
import com.webobjects.foundation.*;

import er.extensions.*;

/**
 * Allows the display of strings instead of Yes/No/Unset or checkboxes for boolean values.<br />
 * 
 */

public class ERD2WCustomDisplayBoolean extends D2WDisplayBoolean {

   public ERD2WCustomDisplayBoolean(WOContext context) {
        super(context);
    }
    
    protected NSArray _choicesNames;
    
    public NSArray choicesNames() {
         if (_choicesNames == null)
             _choicesNames = (NSArray)d2wContext().valueForKey("choicesNames");
         return _choicesNames;
     }

    public Object displayString() {
        Object o = objectPropertyValue();
        if(o == null && choicesNames().count() > 2)
            return choicesNames().objectAtIndex(2);
        if(ERXValueUtilities.booleanValue(o))
            return choicesNames().objectAtIndex(0);
        return choicesNames().objectAtIndex(1);
    }

    public void reset(){
        super.reset();
        _choicesNames = null;
    }
}
