/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import com.webobjects.appserver.*;

/**
 * Tab panel with color backgrounds.<br />
 * 
 * @binding tabs
 * @binding selectedTab
 * @binding tabNameKey
 * @binding nonSelectedBgColor
 * @binding bgcolor
 * @binding submitActionName
 * @binding textColor
 */

public class ERXColoredTabPanel extends ERXTabPanel  {

    public ERXColoredTabPanel(WOContext context) {
        super(context);
    }
}