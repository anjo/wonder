/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */

package er.bugtracker;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
public class HomePage extends WOComponent {

    public HomePage(WOContext aContext) {
        super(aContext);
    }

    public NSDictionary createdKeys() {return ((Session)session()).localizer().createdKeys();
    }
}
