/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.directtoweb.*;
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;

// Only difference between this component and D2WList is that this one uses ERD2WSwitchComponent
public class ERXD2WList extends D2WList {
    protected EOArrayDataSource _dataSource = null;

    public ERXD2WList(WOContext context) {
        super(context);
    }

    /**
     * Calling super is a bad thing in 5.2 when used as an embedded list.
     */
    public void awake() {}

    public EODataSource dataSource() {
        if (this.hasBinding("dataSource") && this.valueForBinding("list") == null)
            return (EODataSource) this.valueForBinding("dataSource");
        if (this.hasBinding("list")) {
            NSArray nsarray = (NSArray) this.valueForBinding("list");
            EOEditingContext eoeditingcontext
                = (nsarray != null && nsarray.count() > 0
                   ? ((EOEnterpriseObject) nsarray.objectAtIndex(0))
                   .editingContext()
                   : null);
            String entityName = (String) this.valueForBinding("entityName");
            if(entityName == null) {
                entityName = (nsarray != null && nsarray.count() > 0
                              ? ((EOEnterpriseObject) nsarray.objectAtIndex(0))
                              .entityName()
                              : null);
            }
            if(entityName != null) {
                if (_dataSource == null || !entityName.equals(_dataSource.classDescriptionForObjects().entityName()) || _dataSource.editingContext() != eoeditingcontext)
                    _dataSource = (eoeditingcontext != null
                                   ? (new EOArrayDataSource(EOClassDescription.classDescriptionForEntityName(entityName), eoeditingcontext)) : null);
            }
            if (_dataSource != null)
                _dataSource.setArray(nsarray);
        }
        return _dataSource;
    }
}
