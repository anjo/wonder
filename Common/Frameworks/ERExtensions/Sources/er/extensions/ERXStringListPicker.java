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
import com.webobjects.directtoweb.NextPageDelegate;

public class ERXStringListPicker extends WOComponent {

    public ERXStringListPicker(WOContext aContext) {
        super(aContext);
    }

    public Object item, _selection;
    public String explanationComponentName;
    public NSDictionary choices;
    public NextPageDelegate nextPageDelegate;
    public WOComponent cancelPage, nextPage;
    

    private NSArray _list;
    public NSArray list() {
        if (_list == null) {
            _list = EOSortOrdering.sortedArrayUsingKeyOrderArray(choices.allKeys(),
                                                         new NSArray(EOSortOrdering.sortOrderingWithKey("description", EOSortOrdering.CompareAscending)));
        }
        return _list;
    }

    public Object selection() {
        if (_selection==null && list().count() > 0)
                _selection = (String)list().objectAtIndex(0);
        return _selection;
    }
    
    public String entityNameForNewInstances() { return (String)choices.objectForKey(_selection); }
    
    public WOComponent next() { return nextPageDelegate != null ? nextPageDelegate.nextPage(this) : nextPage; }
    public WOComponent cancel() { return cancelPage; }
}
