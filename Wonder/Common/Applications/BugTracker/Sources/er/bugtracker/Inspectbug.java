/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */

package er.bugtracker;
import com.webobjects.foundation.*;
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.appserver.*;
import com.webobjects.directtoweb.*;
import java.util.*;
import java.lang.*;
import er.extensions.ERXConstant;

public class Inspectbug extends WOComponent implements InspectPageInterface {

    protected NextPageDelegate nextPageCallback;
    /** @TypeInfo er.bugtracker.Bug */
    protected EOEnterpriseObject currentObject;
    protected WOComponent theNextPage;

    public Inspectbug(WOContext context) {
        super(context);
    }

    public EOEnterpriseObject object() { return currentObject; }

    public void setNextPage(WOComponent nextPage) {
        theNextPage=nextPage;
    }

    public void setObject(EOEnterpriseObject anObject) {
        currentObject = anObject;
        // This next line is the only reason we have this page at all
        EOEnterpriseObject bugOwner=(EOEnterpriseObject)anObject.valueForKey("owner");
        EOEnterpriseObject sessionOwner=((Session)session()).getUser();
        if (bugOwner==sessionOwner) currentObject.takeValueForKey(ERXConstant.OneInteger, "read");
        currentObject.editingContext().saveChanges();
    }

    public void setNextPageDelegate(NextPageDelegate Callback) {
           nextPageCallback= Callback;
    }

    public WOComponent nextPage() {
        return (nextPageCallback != null) ? nextPageCallback.nextPage(this) :
            (theNextPage!=null) ? theNextPage : null;
    }
}
