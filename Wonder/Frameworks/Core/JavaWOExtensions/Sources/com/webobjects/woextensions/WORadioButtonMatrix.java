/*
 * WORadioButtonMatrix.java
 * (c) Copyright 2001 Apple Computer, Inc. All rights reserved.
 * This a modified version.
 * Original license: http://www.opensource.apple.com/apsl/
 */

package com.webobjects.woextensions;


import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSArray;

public class WORadioButtonMatrix extends WOComponent {
    public Object currentItem;
    public int index;
    public String wrapperElementID;
    protected Object _selection = null;

    public WORadioButtonMatrix(WOContext aContext) {
        super(aContext);
    }
    
    public void setCurrentItem(Object aValue) {
        currentItem = aValue;
        setValueForBinding(aValue, "item");
    }

    public Object selection() {
        if (_selection==null) {
            // ** only pull this one time
            _selection = valueForBinding("selection");
        }
        return _selection;
    }

    public void setSelection(String anIndex) {
        if (anIndex != null) {
            int idx = Integer.parseInt(anIndex);
            // ** push the selection to the parent
            NSArray anItemList = (NSArray)_WOJExtensionsUtil.valueForBindingOrNull("list",this);
            Object aSelectedObject = anItemList.objectAtIndex(idx);
            setValueForBinding(aSelectedObject, "selection");
        }
        // ** and force it to be pulled if there's a next time.
        _selection = null;
    }

    public String isCurrentItemSelected() {

        if (selection() != null && selection().equals(currentItem)) {
            return "checked";
        }

        return null;
    }

    protected void _invalidateCaches() {
        _selection = null;
        currentItem = null;
    }

    public boolean isStateless() {
        return true;
    }

    public void reset()  {
        _invalidateCaches();
    }

    public Object nullValue() {
    	return null;
    }
}
