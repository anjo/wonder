/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import com.webobjects.foundation.NSArray;
import com.webobjects.appserver.*;

/*
 This component takes a list of tabs, the same as ERTabPanel
 to find images, the naming convention is:

 /nsi/tab<tabName>.gif : tab is not selected
 /nsi/tab<tabName>Selected.gif : tab is selected

 where tabName is the name of the tab, minus spaces
 */

public class ERXImageTabPanel extends ERXTabPanel  {

    public ERXImageTabPanel(WOContext context) {
        super(context);
    }

    public String currentImage() {
        // FIXME: could use a few more bindings to more naming more generic here!
        String name = currentTabName();
        name = NSArray.componentsSeparatedByString(name, " ").componentsJoinedByString("");
        return "/nsi/tab"+(name)+""+((isCellShaded()) ? "" : "Selected")+".gif";
    }
}