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
import er.extensions.*;

/**
 * Nice edit button for editing a toMany relationship in another page.<br />
 * 
 */

public class ERDEditButton extends ERDActionButton {
    /** logging support */
    private static final ERXLogger log = ERXLogger.getLogger(ERDEditButton.class,"components,actions");

    public ERDEditButton(WOContext context) {super(context);}
    
    protected EOEnterpriseObject localInstanceOfObject() {
    	Object value = d2wContext().valueForKey("useNestedEditingContext");
    	boolean createNestedContext = ERXValueUtilities.booleanValue(value);
    	return ERXEOControlUtilities.editableInstanceOfObject(object(), createNestedContext);
    }

    public boolean isEditable() {
        boolean result = ERXValueUtilities.booleanValue(d2wContext().valueForKey("isEntityEditable"));
        Object o = object();
        if (o instanceof ERXGuardedObjectInterface) {
            result = result && ((ERXGuardedObjectInterface)o).canUpdate();
        }
        return result;
    }

    public WOComponent editObjectAction() {
        EOEnterpriseObject localObject = localInstanceOfObject();
        String configuration = (String)valueForBinding("editConfigurationName");
        if(log.isDebugEnabled()){
           log.debug("configuration = "+configuration);
        }
        EditPageInterface epi = (EditPageInterface)D2W.factory().pageForConfigurationNamed(configuration, session());
        epi.setObject(localObject);
        epi.setNextPage(context().page());
        localObject.editingContext().hasChanges(); // Ensuring it survives.
        return (WOComponent)epi;
    }
}
