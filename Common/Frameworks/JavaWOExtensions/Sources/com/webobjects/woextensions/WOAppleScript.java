/*
 * WOAppleScript.java
 * [JavaWOExtensions Project]
 *
 * � Copyright 2001 Apple Computer, Inc. All rights reserved.
 * This code not the original code. */

package com.webobjects.woextensions;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;

/** @deprecated
    The WOAppleScript component is deprecated.
*/
public class WOAppleScript extends WOComponent {

    protected String _controller;

    protected String Undefined = "Undefined";
    
    public WOAppleScript(WOContext aContext)  {
        super(aContext);
        _controller = Undefined; // this marks an undefined id
    }

    public boolean synchronizesVariablesWithBindings() {
        return false;
    }

    public String controller()  {
        if (_controller == Undefined) {
            _controller = (String) _WOJExtensionsUtil.valueForBindingOrNull("controller",this);
        }
        return _controller;
    }

    public String height()  {
        String aHeight;
        String aController = controller();
        if ((aController!=null) && aController.toLowerCase().equals("true")) {
            aHeight = "25";
        } else {
            aHeight = valueForBinding("height").toString();
        }

        return aHeight;
    }

    public String width()  {
        String aWidth;
        String aController = controller();
        if ((aController!=null) && aController.toLowerCase().equals("true")) {
            aWidth = "108";
        } else {
            aWidth = valueForBinding("width").toString();
        }
        return aWidth;
    }
}