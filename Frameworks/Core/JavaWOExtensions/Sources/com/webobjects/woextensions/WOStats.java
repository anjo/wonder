/*
 * WOStats.java
 * � Copyright 2001 Apple Computer, Inc. All rights reserved.
 * This a modified version.
 * Original license: http://www.opensource.apple.com/apsl/
 */

package com.webobjects.woextensions;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WODirectAction;
import com.webobjects.appserver.WORequest;

public class WOStats extends WODirectAction {

    public WOStats(WORequest aRequest)  {
        super(aRequest);
    }
    
    public WOActionResults defaultAction()  {
        return pageWithName("WOStatsPage");
    }
}