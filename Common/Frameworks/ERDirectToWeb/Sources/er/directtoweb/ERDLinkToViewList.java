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
import java.lang.*;
import org.apache.log4j.*;

public class ERDLinkToViewList extends ERDCustomEditComponent {

    public ERDLinkToViewList(WOContext context) {super(context);}
    
    public static final Category cat = Category.getInstance("er.directtoweb.components.ERDLinkToViewList");

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
        ListPageInterface ipi = (ListPageInterface)D2W.factory().pageForConfigurationNamed((String)valueForBinding("listPageConfigurationName"),
                                                                                           session());
        ipi.setNextPage(context().page());
        ipi.setDataSource(ERXExtensions.dataSourceForArray(list()));
        return (WOComponent)ipi;
    }

    public boolean listIsEmpty() {
        return list()==null || list().count()==0;
    }
}
