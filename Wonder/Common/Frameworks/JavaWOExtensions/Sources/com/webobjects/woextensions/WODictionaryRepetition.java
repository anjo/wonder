/*
 * WODictionaryRepetition.java
 * � Copyright 2001 Apple Computer, Inc. All rights reserved.
 * This a modified version.
 * Original license: http://www.opensource.apple.com/apsl/
 */

package com.webobjects.woextensions;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;

public class WODictionaryRepetition extends WOComponent {

    protected NSArray _keyList;
    protected NSDictionary _dictionary = null;

    public WODictionaryRepetition(WOContext aContext)  {
        super(aContext);
    }

    public boolean isStateless() {
        return true;
    }

    protected void _invalidateCaches() {
        // ** By setting these to null, we allow the dictionary to change after the action and before the next cycle of this component (if the component is on a page which is recycled)
        _dictionary = null;
        _keyList = null;
    }

    public void reset()  {
        _invalidateCaches();
    }

    public NSDictionary dictionary()  {
        if (_dictionary==null) {
            _dictionary = (NSDictionary)_WOJExtensionsUtil.valueForBindingOrNull("dictionary",this);
            if (_dictionary == null) {
                _dictionary = NSDictionary.EmptyDictionary;
                _keyList = NSArray.EmptyArray;
            } else {
                _keyList = _dictionary.allKeys();
            }
        }
        return _dictionary;
    }

    public NSArray keyList()  {
        if (_keyList==null) dictionary();
        return _keyList;
    }

    public Object currentKey() {
        // ** this is required by key/value coding.
        return "";
    }

    public void setCurrentKey(Object aKey)  {
        if ((dictionary()!=null) && (aKey!=null)) {
                Object anObject = dictionary().objectForKey(aKey);
                setValueForBinding(aKey, "key");
                setValueForBinding(anObject, "item");
        }
    }
}
