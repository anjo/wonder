/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions.components;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

/**
 * Useful when given a list of n items and you want to display m keys. This will
 * construct a table nxm and push the current indexes up though the bindings.<br />
 * 
 * @binding list
 * @binding item
 * @binding repetetions
 * @binding index
 * @binding row
 * @binding col
 * @binding maxColumns
 * @binding cellBackgroundColor
 */

public class ERXRepeatingTable extends WOComponent {

    public ERXRepeatingTable(WOContext aContext) {
        super(aContext);
    }

	public boolean isStateless() {
		return true;
	}

	public void reset() {
		_repeatingList = null;
	}
    
    private NSMutableArray _repeatingList;

    public NSArray repeatingList() {
        if (_repeatingList == null) {
            _repeatingList = new NSMutableArray();
            NSArray list = (NSArray)valueForBinding("list");
            Integer numberOfRepetetions = (Integer)valueForBinding("repetetions");
            for (int i = 0; i < numberOfRepetetions.intValue(); i++) {
                    _repeatingList.addObjectsFromArray(list);
            }
        }
        return _repeatingList;
    }

}
