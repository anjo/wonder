/*
 * WOSortOrder.java
 * � Copyright 2001 Apple Computer, Inc. All rights reserved.
 * This a modified version.
 * Original license: http://www.opensource.apple.com/apsl/
 */

package com.webobjects.woextensions;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

import java.util.Enumeration;

public class WOSortOrder extends WOComponent {
    protected String _key;
    protected String _displayKey;
    // ** Internal Caching
    protected WODisplayGroup _displayGroup;

    public WOSortOrder(WOContext aContext)  {
        super(aContext);
    }

    public boolean isStateless() {
        return true;
    }

    public void reset()  {
        _invalidateCaches();
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
    protected EOSortOrdering _primarySortOrdering() {
        NSArray anArray = displayGroup().sortOrderings();
        if ((anArray!=null) && (anArray.count() > 0)) {
            EOSortOrdering anOrdering = (EOSortOrdering)anArray.objectAtIndex(0);
            return anOrdering;
        }
        return null;
    }

    protected boolean _isCurrentKeyPrimary() {
        EOSortOrdering anOrdering = _primarySortOrdering();
        if ((anOrdering!=null) && anOrdering.key().equals(key())) {
            return true;
        }
        return false;
    }

    protected NSSelector _primaryKeySortOrderingSelector() {
        EOSortOrdering anOrdering = _primarySortOrdering();
        NSSelector anOrderingSelector = null;
        if (anOrdering!=null) anOrderingSelector = anOrdering.selector();
        return anOrderingSelector;
    }

    public String imageName() {
        String anImageName = "Unsorted.gif";
        if (_isCurrentKeyPrimary()) {
            NSSelector aCurrentState = _primaryKeySortOrderingSelector();
            if (aCurrentState == EOSortOrdering.CompareAscending) {
                anImageName = "Ascending.gif";
            } else if (aCurrentState == EOSortOrdering.CompareDescending) {
                anImageName = "Descending.gif";
            }
        }
        return anImageName;
    }

    protected void _removeSortOrderingWithKey(String aKey) {
        int anIndex = 0;
        EOSortOrdering aSortOrdering = null;
        WODisplayGroup aDisplayGroup = displayGroup();
        NSArray sortOrderings = aDisplayGroup.sortOrderings();
        if (sortOrderings!=null) {
            NSMutableArray aSortOrderingArray = sortOrderings.mutableClone();
            Enumeration anEnumerator = aSortOrderingArray.objectEnumerator();
            while (anEnumerator.hasMoreElements()) {
                aSortOrdering = (EOSortOrdering) anEnumerator.nextElement();
                if (aKey.equals(aSortOrdering.key())) {
                    aSortOrderingArray.removeObjectAtIndex(anIndex);
                    break;
                }
                anIndex++;
            }
            aDisplayGroup.setSortOrderings(aSortOrderingArray);
        }
    }

    protected void _makePrimarySortOrderingWithSelector(NSSelector aSelector) {
        String aKey = key();
        WODisplayGroup aDisplayGroup = displayGroup();
        NSArray sortOrderings = aDisplayGroup.sortOrderings();
        NSMutableArray aSortOrderingArray;
        if (sortOrderings!=null) {
            aSortOrderingArray = new NSMutableArray(sortOrderings);
        } else {
            aSortOrderingArray = new NSMutableArray();
        }
        EOSortOrdering aNewSortOrdering = EOSortOrdering.sortOrderingWithKey(aKey, aSelector);
        aSortOrderingArray.insertObjectAtIndex(aNewSortOrdering, 0);
        if (aSortOrderingArray.count() > 3) {
            // ** limits sorting to 3 levels
            aSortOrderingArray.removeLastObject();
        }
        aDisplayGroup.setSortOrderings(aSortOrderingArray);
    }

    public String helpString() {
        return "Push to toggle sorting order according to "+displayKey();
    }

    /////////////
    // Actions
    /////////////
    public WOComponent toggleClicked() {
        String aKey = key();
        if (_isCurrentKeyPrimary()) {
            NSSelector aCurrentState = _primaryKeySortOrderingSelector();
            if (aCurrentState == EOSortOrdering.CompareAscending) {
                // from ascending to descending
                _removeSortOrderingWithKey(aKey);
                _makePrimarySortOrderingWithSelector(EOSortOrdering.CompareDescending);
            } else if (aCurrentState == EOSortOrdering.CompareDescending) {
                // from descending to none
                _removeSortOrderingWithKey(aKey);
            } else {
                // from none to ascending
                _removeSortOrderingWithKey(aKey);
                _makePrimarySortOrderingWithSelector(EOSortOrdering.CompareAscending);
            }
        } else {
            _removeSortOrderingWithKey(aKey);
            _makePrimarySortOrderingWithSelector(EOSortOrdering.CompareAscending);
        }
        displayGroup().updateDisplayedObjects();
        return null;
    }
    
    protected void _invalidateCaches() {
        // ** By setting these to nil, we allow for cycling of the page)
        _key = null;
        _displayKey = null;
        _displayGroup = null;
    }
}
