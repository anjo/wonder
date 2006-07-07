/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import java.util.*;

import org.apache.log4j.Logger;

import com.webobjects.directtoweb.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

import er.extensions.*;

/**
 * Very useful when you want to restrict the things a user can see during searches or in list pages.<br />
 * set it up via a rule like:
 *
 *  entity.name = "Movie" and session.user.role <> "admin"
 *  =>
 *  extraRestrictingQualifier = {
 *      "studio" = "session.user.studios";
 *  } [er.directtoweb.ERDDelayedExtraQualifierAssignment]
 *
 * then in your query page use sth like:
 public EODataSource queryDataSource() {
     EODataSource ds = super.queryDataSource();
     if (ds != null && (ds instanceof EODatabaseDataSource)) {
         EOFetchSpecification fs = ((EODatabaseDataSource)ds).fetchSpecification();
         EOQualifier q = fs.qualifier();
         EOQualifier extraQualifier = (EOQualifier)d2wContext().valueForKey("extraRestrictingQualifier");

         if(q != null && extraQualifier != null) {
             q = new EOAndQualifier(new NSArray(new Object[] {q, extraQualifier}));
         } else if(extraQualifier != null) {
             q = extraQualifier;
         }
         fs.setQualifier(q);
     }
     return ds;
 }
 * This should guarantee that the user can only see the Movies that
 * are made by studios contained in his studio relationship.
 * If the value is null, then this qualifier will not be added. To search for NULL,
 * return NSKeyValueCoding.NullValue. 
 * @author ak
 */

public class ERDDelayedExtraQualifierAssignment extends ERDDelayedAssignment {

    /** logging support */
    public static final Logger log = Logger.getLogger(ERDDelayedExtraQualifierAssignment.class);
    
    /**
     * Static constructor required by the EOKeyValueUnarchiver
     * interface. If this isn't implemented then the default
     * behavior is to construct the first super class that does
     * implement this method. Very lame.
     * @param eokeyvalueunarchiver to be unarchived
     * @return decoded assignment of this class
     */
    public static Object decodeWithKeyValueUnarchiver(EOKeyValueUnarchiver eokeyvalueunarchiver)  {
        return new ERDDelayedExtraQualifierAssignment(eokeyvalueunarchiver);
    }

    /** 
     * Public constructor
     * @param u key-value unarchiver used when unarchiving
     *		from rule files. 
     */    
    public ERDDelayedExtraQualifierAssignment (EOKeyValueUnarchiver u) { super(u); }
    
    /** 
     * Public constructor
     * @param key context key
     * @param value of the assignment
     */
    public ERDDelayedExtraQualifierAssignment (String key, Object value) { super(key,value); }

    protected EOQualifier qualifierForArray(String key, NSArray objects) {
        if(objects == null)
            return null;
        if(objects.count() == 0)
            return new EOKeyValueQualifier(key, EOQualifier.QualifierOperatorEqual, null);
        return new ERXPrimaryKeyListQualifier(key, objects);
    }

    protected EOQualifier qualifierForObject(String key, Object object) {
        return new EOKeyValueQualifier(key, EOQualifier.QualifierOperatorEqual, object);
    }

    protected EOQualifier extraQualifier(D2WContext c, NSDictionary dict) {
        NSMutableArray qualifiers = new NSMutableArray();
        EOQualifier result = null;
        for(Enumeration e = dict.keyEnumerator(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            Object value = c.valueForKeyPath((String)dict.objectForKey(key));
            if(value != null) {
                EOQualifier q;
                if(value instanceof NSArray) {
                    q = qualifierForArray(key, (NSArray)value);
                } else {
                    if(value == NSKeyValueCoding.NullValue) {
                        value = null;
                    }
                    q = qualifierForObject(key, value);
                }
                if(q != null) qualifiers.addObject(q);
            }
        }
        if(qualifiers.count() > 0)
            result = new EOAndQualifier(qualifiers);
        return result;
    }

    public Object fireNow(D2WContext c) {
        Object result = null;
        Object value = value();
        if(value != null && value instanceof NSDictionary) {
            result = extraQualifier(c, (NSDictionary)value);
        }
        return result;
    }
}