/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WContext;
import com.webobjects.foundation.NSKeyValueCodingAdditions;

import er.extensions.components.ERXStatelessComponent;
import er.extensions.localization.ERXLocalizer;

/**
 * Displays section name as a string.<br />
 * 
 */

public class ERDDefaultSectionComponent extends ERXStatelessComponent {
    
    public ERDDefaultSectionComponent(WOContext context) {
        super(context);
    }
    
    public Object object() { 
        return valueForBinding("object"); 
    }
    
    public Object sectionTitle() {
        D2WContext c=(D2WContext)valueForBinding("d2wContext");
        Object result=object();
        if (result!=null) {
            if (c!=null) {
                String k=(String)c.valueForKey("keyWhenGrouping");
                if (k!=null) {
                    result=NSKeyValueCodingAdditions.Utility.valueForKeyPath(result,k);
                }
                String templateString=(String)c.valueForKey("templateString");
                if (templateString!=null) {
                    result=ERXLocalizer.currentLocalizer().localizedTemplateStringForKeyWithObjectOtherObject(templateString, object(), c);
                }
            }
        }
        return result;
    }
}
