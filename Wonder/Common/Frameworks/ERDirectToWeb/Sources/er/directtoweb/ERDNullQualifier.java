/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.directtoweb.*;

// FIXME: This may need to subclass EOQualifier now.
/**
 * Qualifer used to test if something is null.<br />
 * 
 */

public class ERDNullQualifier extends Object implements EOQualifierEvaluation {

    private String _keyPath;
    
    public ERDNullQualifier(String keyPath) {
        _keyPath = keyPath;
    }

    public ERDNullQualifier(EOKeyValueUnarchiver u) {
        _keyPath = (String)u.decodeObjectForKey("key");
    }
    
    public boolean evaluateWithObject (Object obj) {
        boolean result = false;
        if (obj instanceof D2WContext)
            result = ((D2WContext)obj).valueForKeyPath(_keyPath) == null;
        else if (obj instanceof EOKeyValueCodingAdditions)
            result = ((EOKeyValueCodingAdditions)obj).valueForKeyPath(_keyPath) == null;
        return result;
    }
    
    public java.lang.String description() { return this.toString(); }
    public java.lang.String toString() { return "NullQualifier: " + _keyPath + " is null."; }
}
