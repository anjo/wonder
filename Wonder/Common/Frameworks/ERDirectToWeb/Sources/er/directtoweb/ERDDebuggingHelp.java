/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.appserver.*;
import er.extensions.ERXUtilities;
import er.extensions.ERXSession;

/**
 * Little help component useful for debugging.<br />
 * 
 * @binding d2wContext
 * @binding condition" defaults="Boolean
 */

public class ERDDebuggingHelp extends WOComponent {

    public ERDDebuggingHelp(WOContext context) { super(context); }

    public boolean synchronizesVariablesWithBindings() { return false; }
    
    public boolean showHelp() {
        return ERDirectToWeb.d2wDebuggingEnabled(session()) || ERXUtilities.booleanValue(valueForBinding("condition"));
    }
    public boolean d2wComponentNameDebuggingEnabled() {
        return ERDirectToWeb.d2wComponentNameDebuggingEnabled(session());
    }
    public WOComponent toggleComponentNameDebugging() {
        ERDirectToWeb.setD2wComponentNameDebuggingEnabled(session(),
                                                          !ERDirectToWeb.d2wComponentNameDebuggingEnabled(session()));
        return null;
    }

    public String key;

    public Object debugValueForKey() {
        if(key != null && !"".equals(key))
            return parent().valueForKeyPath("d2wContext."+key);
        return null;
    }
}
