/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;

public class ERDTabText extends WOComponent {

    public ERDTabText(WOContext context) { super(context); }
    
    public boolean isStateless() { return true; }
}
