/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;

public class LogoutPage extends WOComponent {

    public LogoutPage(WOContext context) {
        super(context);
    }

    public boolean isStateless() { return true; }
}
