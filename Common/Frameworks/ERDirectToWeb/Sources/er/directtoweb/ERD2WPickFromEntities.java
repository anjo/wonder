/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.directtoweb.*;

public class ERD2WPickFromEntities extends ERDCustomQueryComponent {

    public ERD2WPickFromEntities(WOContext context) {
        super(context);
    }
        
    // lets you pick from either an arbitrary list or a pool of shared EOs  
    public Object item; 

    // can't be stateless!
    public boolean isStateless() { return false; }
    public boolean synchronizesVariablesWithBindings() { return false; }

    public NSArray list() { return (NSArray)valueForBinding("list"); }

    public String displayString() {
        String result=null;
        if (item!=null) {
            D2WContext d2wContext = ERDirectToWeb.d2wContext();
            d2wContext.setEntity(EOModelGroup.defaultGroup().entityNamed((String)item));
            result=(String)d2wContext.valueForKey("displayNameForEntity");
        }
        return result;
    }
}
