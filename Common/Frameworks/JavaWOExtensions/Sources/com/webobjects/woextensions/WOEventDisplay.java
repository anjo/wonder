/*
 * WOEventDisplay.java
 * � Copyright 2001 Apple Computer, Inc. All rights reserved.
 * This a modified version.
 * Original license: http://www.opensource.apple.com/apsl/
 */

package com.webobjects.woextensions;

import com.webobjects.appserver.*;

public class WOEventDisplay extends WODirectAction {
    public WOEventDisplay(WORequest aRequest)  {
        super(aRequest);
    }

    public WOActionResults defaultAction()
    {
        return pageWithName("WOEventDisplayPage");
    }
}