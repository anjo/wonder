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
import er.extensions.ERXConstant;

public class ERD2WListXMLPageTemplate extends ERD2WListPageTemplate {

    public ERD2WListXMLPageTemplate(WOContext context) {super(context);}
    
    protected int indexForOffset;
    
    public Object value() {
        return object().valueForKeyPath((String)d2wContext().valueForKey("propertyKey"));
    }

    public String displayNameForGroupingKey(){
        d2wContext().takeValueForKey(d2wContext().valueForKey("groupingKey"), "propertyKey");
        return (String)d2wContext().valueForKey("displayNameForProperty");
    }

    public boolean userPreferencesCanSpecifySorting(){ return false; }


    public String componentName() {
        d2wContext().takeValueForKey(ERXConstant.OneInteger, "frame");
        d2wContext().takeValueForKey(d2wContext().valueForKey("thirdLevelRelationshipKey"), "propertyKey");
        return (String)d2wContext().valueForKey("componentName");
    }
}
