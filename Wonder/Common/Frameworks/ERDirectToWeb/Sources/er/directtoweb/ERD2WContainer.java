/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.foundation.*;

/**
 * Used to hold pieces of a tabsSectionsContents, holds either sections or tabs.
 */

public class ERD2WContainer {
    public String name;
    public String displayName;
    public NSMutableArray keys;
    
    public ERD2WContainer() {}
    
    public ERD2WContainer(String newName) {
        name = newName;
        keys = new NSMutableArray();
    }
    
    public ERD2WContainer(String newName, NSArray newKeys) {
        name = newName;
        keys = new NSMutableArray(newKeys);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (displayName != null && displayName.trim().length() > 0) {
            sb.append(displayName).append(": ");
        } else if (name != null && name.trim().length() > 0) {
            sb.append(name).append(": ");
        }
        sb.append(keys);
        return sb.toString();
    }
}
