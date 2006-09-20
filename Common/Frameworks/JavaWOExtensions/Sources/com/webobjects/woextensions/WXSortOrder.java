/*
 * WXSortOrder.java
 * � Copyright 2001 Apple Computer, Inc. All rights reserved.
 * This a modified version.
 * Original license: http://www.opensource.apple.com/apsl/
 */

package com.webobjects.woextensions;

import java.util.Enumeration;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODisplayGroup;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSSelector;

public class WXSortOrder extends WOComponent {
    protected WODisplayGroup _displayGroup;
    protected String _key;
    protected String _displayKey;

    public WXSortOrder(WOContext aContext)  {
        super(aContext);
    }

    /////////////
    // No-Sync
    ////////////
    public boolean synchronizesVariablesWithBindings() {
        return false;
    }

    /////////////
    // Bindings
    ////////////
    public WODisplayGroup displayGroup() {
        if (null==_displayGroup) {
            _displayGroup = (WODisplayGroup)_WOJExtensionsUtil.valueForBindingOrNull("displayGroup",this);
        }
        return _displayGroup;
    }

    public String key() {
        if (null==_key) {
            _key = (String)_WOJExtensionsUtil.valueForBindingOrNull("key",this);
        }
        return _key;
    }

    public String displayKey() {
        if (null==_displayKey) {
            _displayKey = (String)_WOJExtensionsUtil.valueForBindingOrNull("displayKey",this);
        }
        return _displayKey;
    }

    ///////////
    // Utility
    ///////////
    public String imageName() {
        if (_isCurrentKeyPrimary()) {
            NSSelector aCurrentState = _primaryKeyOrderingSelector();
            if (aCurrentState==EOSortOrdering.CompareCaseInsensitiveAscending) {
                return "Ascending.gif";
            }
            if (aCurrentState==EOSortOrdering.CompareCaseInsensitiveDescending) {
                return "Descending.gif";
            }
        }
        return "Unsorted.gif";
    }

    public boolean _isCurrentKeyPrimary() {
        EOSortOrdering anOrdering = _primaryOrdering();
        if (anOrdering.key().equals(key())) {
            return true;
        }
        return false;
    }

    public NSSelector _primaryKeyOrderingSelector() {
        EOSortOrdering anOrdering = _primaryOrdering();
        return anOrdering.selector();
    }

    public EOSortOrdering _primaryOrdering() {
        NSArray anArray = _sortOrderingArray();
        if (anArray.count() > 0) {
            EOSortOrdering anOrdering = (EOSortOrdering)anArray.objectAtIndex(0);
            return anOrdering;
        }
        return null;
    }

    protected NSArray _sortOrderingArray() {
        return (NSArray)_WOJExtensionsUtil.valueForBindingOrNull("sortOrderings",this);
    }

    protected NSMutableArray XX_sortOrderingArray() {
        WODisplayGroup displayGroup = displayGroup();
        NSArray orderingArray;
        if (null!=displayGroup)
            orderingArray = displayGroup.sortOrderings();
        else
            orderingArray = (NSArray)_WOJExtensionsUtil.valueForBindingOrNull("sortOrderings",this);

        if (null==orderingArray) {
            orderingArray = new NSMutableArray();
        } else {
            orderingArray = new NSMutableArray(orderingArray);
        }

        if (null!=displayGroup)
            displayGroup.setSortOrderings(orderingArray);
        else {
            setValueForBinding(orderingArray, "sortOrderings");
        }

        return (NSMutableArray)orderingArray;
    }

    protected void _removeOrderingWithKey(String aKey) {
        int anIndex = 0;
        EOSortOrdering ordering;
        NSArray orderingArray = _sortOrderingArray();
        Enumeration anEnumerator = orderingArray.objectEnumerator();
        while (anEnumerator.hasMoreElements()) {
            ordering = (EOSortOrdering) anEnumerator.nextElement();
            if (aKey.equals(ordering.key())) {
                ((NSMutableArray)orderingArray).removeObjectAtIndex(anIndex);
                return ;
            }
            anIndex++;
        }
    }

    protected void _makePrimaryOrderingWithSelector(NSSelector aSelector) {
        NSMutableArray orderingArray = (NSMutableArray)_sortOrderingArray();
        EOSortOrdering aNewOrdering = EOSortOrdering.sortOrderingWithKey(key(), 
            aSelector);
        orderingArray.insertObjectAtIndex(aNewOrdering, 0);
        if (orderingArray.count() > 3) {
            // ** limits ing to 3 levels
            orderingArray.removeLastObject();
        }
    }

    public String helpString() {
        return "Push to toggle sorting order according to "+displayKey();
    }

    /////////////
    // Actions
    /////////////


    public WOComponent toggleClicked() {
        if (_isCurrentKeyPrimary()) {
            NSSelector aCurrentState = _primaryKeyOrderingSelector();
            if (aCurrentState==EOSortOrdering.CompareCaseInsensitiveAscending) {
                _removeOrderingWithKey(key());
                _makePrimaryOrderingWithSelector( EOSortOrdering.CompareCaseInsensitiveDescending);
            } else if (aCurrentState==EOSortOrdering.CompareCaseInsensitiveDescending) {
                _removeOrderingWithKey(key());
            }
        } else {
            _removeOrderingWithKey(key());
            _makePrimaryOrderingWithSelector(EOSortOrdering.CompareCaseInsensitiveAscending);
        }
        displayGroup().updateDisplayedObjects();
        return null;
    }

}
