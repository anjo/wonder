/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */

/* PageDelegate.java created by patrice on Tue 06-Jun-2000 */
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.directtoweb.*;

public class ERDPageDelegate implements NextPageDelegate {

    public WOComponent _nextPage;
    public ERDPageDelegate(WOComponent np) { _nextPage=np; }
    public WOComponent nextPage(WOComponent sender) { return _nextPage; }    
}
