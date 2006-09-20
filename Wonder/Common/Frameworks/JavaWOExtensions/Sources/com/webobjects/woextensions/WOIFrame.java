/*
 * WOIFrame.java
 * � Copyright 2001 Apple Computer, Inc. All rights reserved.
 * This a modified version.
 * Original license: http://www.opensource.apple.com/apsl/
 */

package com.webobjects.woextensions;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOElement;

public class WOIFrame extends WOComponent
{
    public WOIFrame(WOContext aContext)  {
        super(aContext);
    }

    public boolean synchronizesVariablesWithBindings() {
        return false;
    }

    public String srcUrl()  {
        if (hasBinding("src")) {
            return (String)_WOJExtensionsUtil.valueForBindingOrNull("src",this);
        }
        if (hasBinding("pageName") || hasBinding("value") || hasBinding("actionName")) {
            return context().componentActionURL();
        }
        return "ERROR_URL_NOT_FOUND";
    }

    public WOElement frameContent()  {
        WOElement aContentElement = null;
        if (hasBinding("pageName")) {
            String  aPageName = (String)_WOJExtensionsUtil.valueForBindingOrNull("pageName",this);
            aContentElement = pageWithName(aPageName);
        } else if(hasBinding("value")) {
            aContentElement = (WOElement)_WOJExtensionsUtil.valueForBindingOrNull("value",this);
        } else if(hasBinding("actionName")) {
            aContentElement = (WOElement)parent().valueForBinding((String)valueForBinding("actionName"));
        }
        return aContentElement;
    }
}
