/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

// Nice little unread marker.  Subclass in specific look frameworks to provide custom
//	unread marker images.
public class ERXUnreadMarker extends WOComponent {

    public ERXUnreadMarker(WOContext aContext) {
        super(aContext);
    }

    /////////////////////////////////  log4j category  /////////////////////////////////
    public static ERXLogger log = ERXLogger.getERXLogger(ERXUnreadMarker.class);
    
    public boolean synchronizesVariablesWithBindings() { return false; }
    public boolean isStateless() { return true; }

    public void reset() { super.reset(); initialized=false; }
    
    private boolean initialized=false;
    private boolean result=false;
    public boolean showUnread() {
        if (!initialized) {
            result=false;
            if (hasBinding("condition")) {
                Number n=(Number)valueForBinding("condition");
                result=n!=null && n.intValue()!=0;
            } else {
                NSArray list=(NSArray)valueForBinding("list");
                Object item=valueForBinding("item");
                result=list!=null && item!=null && list.containsObject(item);
            }
            if (hasBinding("negate")) {
                Integer negate=(Integer)valueForBinding("negate");
                if (negate!=null && negate.intValue()!=0) result=!result;
            }
            initialized=true;
        }
        return result;
    }
    
    public boolean doNotShowUnread() { return !showUnread(); }
}
