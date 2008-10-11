/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb.components.relationships;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2W;
import com.webobjects.directtoweb.ListPageInterface;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;

import er.directtoweb.components.ERDCustomEditComponent;
import er.extensions.eof.ERXEOControlUtilities;

/**
 * Generic link component used to view a list.<br />
 * @binding list the list to show
 * @binding object object to get list from
 * @binding key keypath to get list from object
 * @binding listConfigurationName name of the page configuration to jump to
 * @binding entityName
 */

public class ERDLinkToViewList extends ERDCustomEditComponent {

    public ERDLinkToViewList(WOContext context) { super(context); }
    
    public static final Logger log = Logger.getLogger(ERDLinkToViewList.class);

    public boolean isStateless() { return true; }
    public boolean synchronizesVariablesWithBindings() { return false; }

    public void reset() {
        super.reset();
        _list = null;
    }

    private NSArray _list;
    public NSArray list() {
        if (_list==null) {
            if (hasBinding("list"))
                _list=(NSArray)valueForBinding("list");
            else
                _list=(NSArray)objectPropertyValue();
        }
        return _list;
    }

    public WOComponent view() {
    	String listConfigurationName = (String)valueForBinding("listConfigurationName");
        ListPageInterface ipi = (ListPageInterface)D2W.factory().pageForConfigurationNamed(listConfigurationName, session());
        ipi.setNextPage(context().page());
        ipi.setDataSource(ERXEOControlUtilities.dataSourceForArray(list()));
        return (WOComponent)ipi;
    }

    public String linkName() {
    	String displayName = (String) d2wContext().valueForKey("displayNameForLinkToViewList");
    	if (displayName == null) {
	    	displayName = (String) valueForBinding("entityName");
	    	if(displayName == null && !listIsEmpty() && list().lastObject() instanceof EOEnterpriseObject) {
	    		displayName = ((EOEnterpriseObject)list().lastObject()).entityName();
	    	} else {
	    		displayName = "";
	    	}
    	}
    	return displayName;
    }
    
    public boolean listIsEmpty() {
        return list()==null || list().count()==0;
    }
}
