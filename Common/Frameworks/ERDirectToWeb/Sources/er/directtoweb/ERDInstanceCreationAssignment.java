/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.directtoweb.*;
import com.webobjects.eocontrol.*;
import er.extensions.*;

public class ERDInstanceCreationAssignment extends ERDDelayedAssignment {
    /** logging support */
    public final static ERXLogger log = ERXLogger.getERXLogger(ERDDelayedAssignment.class);
    
    /** holds the array of keys this assignment depends upon */
    public static final NSArray _DEPENDENT_KEYS=new NSArray();

    /**
     * Static constructor required by the EOKeyValueUnarchiver
     * interface. If this isn't implemented then the default
     * behavior is to construct the first super class that does
     * implement this method. Very lame.
     * @param eokeyvalueunarchiver to be unarchived
     * @return decoded assignment of this class
     */
    public static Object decodeWithKeyValueUnarchiver(EOKeyValueUnarchiver eokeyvalueunarchiver)  {
        return new ERDInstanceCreationAssignment(eokeyvalueunarchiver);
    }
    
    /** 
     * Public constructor
     * @param u key-value unarchiver used when unarchiving
     *		from rule files. 
     */    
    public ERDInstanceCreationAssignment (EOKeyValueUnarchiver u) { super(u); }
    
    /** 
     * Public constructor
     * @param key context key
     * @param value of the assignment
     */
    public ERDInstanceCreationAssignment (String key, Object value) { super(key,value); }

    /**
     * Implementation of the {@link ERDComputingAssignmentInterface}. This
     * assignment depends upon the context key: "entity.name". This array 
     * of keys is used when constructing the 
     * significant keys for the passed in keyPath.
     * @param keyPath to compute significant keys for. 
     * @return array of context keys this assignment depends upon.
     */
    public NSArray dependentKeys(String keyPath) { return _DEPENDENT_KEYS; }

    public Object fireNow(D2WContext c) {
        Object o = null;
        Object value = value();
        String className;
        if(value instanceof String ) {
            className = (String)value();
            try {
                o = Class.forName(className).newInstance();
            } catch(Exception ex) {
                log.error("Can't create instance: "+  className, ex);
            }
        } else {
            log.error("Value not a class name: " + value);
        }
        return o;
    }
}
