/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */

package er.bugtracker;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import er.directtoweb.*;

public class BTListPage extends ERD2WListPageTemplate {

    public BTListPage(WOContext c) {
        super(c);
    }

    public void editingContextDidSaveChanges(NSNotification notif) {
        super.editingContextDidSaveChanges(notif);
        // disabling the refetching that occurs
        _hasToUpdate=false;
    }
    
}

