/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import org.apache.log4j.Logger;

import com.webobjects.directtoweb.D2WContext;
import com.webobjects.eocontrol.EOKeyValueUnarchiver;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;

import er.extensions.foundation.ERXValueUtilities;

///////////////////////////////////////////////////////////////////////////////
// Stepchild of DelayedConditionalAssignment
//	Takes three entries in a dictionary format.
//	conditionKey - keyPath of the condition fired off of the d2wContext.
//	trueValue - the value used if the condition returns true
//	falseValue - the value used if the condition returns false
///////////////////////////////////////////////////////////////////////////////
/**
 * Takes a condition and evalutaes this condition everytime the rule is asked for.<br />
 * 
 */

public class ERDDelayedBooleanAssignment extends ERDDelayedAssignment implements ERDComputingAssignmentInterface {

    /** logging support */
    public static final Logger log = Logger.getLogger("er.directtoweb.rules.DelayedBooleanAssignment");

    /**
     * Static constructor required by the EOKeyValueUnarchiver
     * interface. If this isn't implemented then the default
     * behavior is to construct the first super class that does
     * implement this method. Very lame.
     * @param eokeyvalueunarchiver to be unarchived
     * @return decoded assignment of this class
     */
    public static Object decodeWithKeyValueUnarchiver(EOKeyValueUnarchiver eokeyvalueunarchiver)  {
        return new ERDDelayedBooleanAssignment(eokeyvalueunarchiver);
    }    

    /** 
     * Public constructor
     * @param u key-value unarchiver used when unarchiving
     *		from rule files. 
     */
    public ERDDelayedBooleanAssignment(EOKeyValueUnarchiver u) { super(u); }
    
    /** 
     * Public constructor
     * @param key context key
     * @param value of the assignment
     */
    public ERDDelayedBooleanAssignment(String key, Object value) { super(key,value); }

    /**
     * Implementation of the {@link ERDComputingAssignmentInterface}. This
     * assignment depends upon the value of the key "conditionKey" of the
     * value of this assignment. This key is used when constructing the 
     * significant keys for the passed in keyPath.
     * @param keyPath to compute significant keys for. 
     * @return array of context keys this assignment depends upon.
     */
    public NSArray dependentKeys(String keyPath) {
        NSDictionary booleanConditions = (NSDictionary)value();
        return new NSArray(booleanConditions.objectForKey("conditionKey"));
    }

    public Object fireNow(D2WContext c) {
        NSDictionary booleanConditions = (NSDictionary)value();
        if (log.isDebugEnabled())
            log.debug("Resolving delayed fire for boolean conditions: " + booleanConditions);
        return ERXValueUtilities.booleanValue(c.valueForKeyPath((String)booleanConditions.objectForKey("conditionKey"))) ?
            booleanConditions.objectForKey("trueValue") : booleanConditions.objectForKey("falseValue");

    }
}
