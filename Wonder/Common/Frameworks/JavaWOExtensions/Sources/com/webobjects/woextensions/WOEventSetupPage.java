/*
 * WOEventSetupPage.java
 * � Copyright 2001 Apple Computer, Inc. All rights reserved.
 * This a modified version.
 * Original license: http://www.opensource.apple.com/apsl/
 */

package com.webobjects.woextensions;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

public class WOEventSetupPage extends WOEventPage {
    public Class	currentClass;
    public String	currentEventDescription;
    public int		currentIndex;

    protected static final _ClassNameComparator _classNameAscendingComparator = new _ClassNameComparator(EOSortOrdering.CompareAscending);
    
    public WOEventSetupPage(WOContext aContext) {
        super(aContext);
    }

    public NSArray registeredEventClasses() {
        NSMutableArray	classes;


        classes = new NSMutableArray();
        classes.setArray(EOEventCenter.registeredEventClasses());
        
        try {
            classes.sortUsingComparator(_classNameAscendingComparator);
        } catch (NSComparator.ComparisonException e) {
            throw NSForwardException._runtimeExceptionForThrowable(e);
        }

        return classes;
    }

    public boolean isClassRegisteredForLogging() {
         return EOEventCenter.recordsEventsForClass(currentClass);
    }

    public void setIsClassRegisteredForLogging(boolean yn) {
         EOEventCenter.setRecordsEvents(yn, currentClass);
    }

    protected void _setAllRegisteredEvents(boolean tf) {
        NSArray	registered;
        int i, n;
        Class c;

        registered = EOEventCenter.registeredEventClasses();
        int count = registered.count();
        for (i = 0, n = count; i < n; i++) {
            c = (Class)registered.objectAtIndex(i);
            EOEventCenter.setRecordsEvents(tf, c);
        }
    }
    
    public WOComponent selectAll() {
        _setAllRegisteredEvents(true);
        return null;
    }

    public WOComponent clearAll() {
        _setAllRegisteredEvents(false);
        return null;
    }
    
    public NSArray currentEventDescriptions() {
        NSMutableArray	descs;
        NSDictionary	map;

        map = EOEvent.eventTypeDescriptions(currentClass);

        descs = new NSMutableArray();
        descs.setArray(map.allValues());
        descs.removeObject(map.objectForKey(EOEvent.EventGroupName));
        try {
            descs.sortUsingComparator(NSComparator.AscendingStringComparator);
        } catch (NSComparator.ComparisonException e) {
            throw NSForwardException._runtimeExceptionForThrowable(e);
        }
        descs.insertObjectAtIndex(map.objectForKey(EOEvent.EventGroupName), 0);

        return descs;
    }

    public boolean isClassName() {
        return (currentIndex == 0);
    }

}
