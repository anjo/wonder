/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WInspect;

// Only difference between this component and D2WInspect is that this one uses ERD2WSwitchComponent
public class ERXD2WInspect extends D2WInspect {

    public ERXD2WInspect(WOContext context) { super(context); }
    
    public void validationFailedWithException(Throwable e, Object value, String keyPath) {
        parent().validationFailedWithException(e, value, keyPath);
    }
    /**
     * Calling super is a bad thing in 5.2 when used as an embedded inspect.
     */
    public void awake() {}
}
