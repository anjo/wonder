/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;

/**
 * Nice for adjusting the query specs for dates on a display group.<br />
 * 
 * @binding displayGroup
 * @binding key
 */

public class ERXQueryRecentDates extends WOComponent {

    public ERXQueryRecentDates(WOContext aContext) {
        super(aContext);
    }
    
    /** logging support */
    public final static ERXLogger log = ERXLogger.getERXLogger(ERXQueryRecentDates.class);
    
    public WODisplayGroup displayGroup;
    public String key;

    static final int[] daysAgoArray={ 0,1,3,7,30,90,180 };
    static final String[] daysAgoString={
        "-",
        "day",
        "3 days",
        "week",
        "month",
        "3 months",
        "6 months"
    };
    private final static Integer[] indices={
        ERXConstant.ZeroInteger,
        ERXConstant.OneInteger,
        ERXConstant.TwoInteger,
        ERXConstant.integerForInt(3),
        ERXConstant.integerForInt(4),
        ERXConstant.integerForInt(5),
        ERXConstant.integerForInt(6) };
    private static NSArray indexes=new NSArray(indices);

    public NSArray indexes() { return indexes; }
    public Integer dateItem;

    public String displayString() {
        return daysAgoString[dateItem.intValue()];
    }

    public Object date() {
        int found=0;
        NSTimestamp dateFromQueryMin=(NSTimestamp)displayGroup.queryMin().valueForKey(key);
        if (dateFromQueryMin!=null) {
            NSTimestamp now=new NSTimestamp();
            int d = (int)ERXTimestampUtility.differenceByDay(dateFromQueryMin, now);
            if (d>0) {
                for (int i=0;i<daysAgoArray.length-1;i++) {
                    if (d>=daysAgoArray[i] && d<= daysAgoArray[i+1]) {
                        found=i+1;
                        break;
                    }
                }
            }
        }
        return indexes.objectAtIndex(found);
    }

    public void setDate(Integer dateIndex) {
        NSTimestamp now=new NSTimestamp();
        int howManyDaysAgo=dateIndex!=null ? daysAgoArray[dateIndex.intValue()] : 0;
        if(howManyDaysAgo==0)
            displayGroup.queryMin().removeObjectForKey(key);
        else
            displayGroup.queryMin().takeValueForKey(now.timestampByAddingGregorianUnits(0,0,-howManyDaysAgo,0,0,0), key);
    }
}