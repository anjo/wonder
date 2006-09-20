/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.corebusinesslogic;

import org.apache.log4j.Logger;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSNotification;
import com.webobjects.foundation.NSNotificationCenter;
import com.webobjects.foundation.NSSelector;
import com.webobjects.foundation.NSTimestamp;

import er.extensions.ERXConstant;
import er.extensions.ERXExtensions;
import er.extensions.ERXGenericRecord;
import er.extensions.ERXRetainer;

/**
 * EO subclass that has a timestamp with its creation date, the most recent modification, 
 * and a log entry describing the change.
 */
public abstract class ERCStampedEnterpriseObject extends ERXGenericRecord {

    /** logging support */
    public static Logger log = Logger.getLogger(ERCStampedEnterpriseObject.class);

    public static String [] TimestampAttributeKeys = new String[] { "created", "lastModified"};
    
    private static NSMutableDictionary _datesPerEcID=new NSMutableDictionary();
    public static class Observer {
        public void updateTimestampForEditingContext(NSNotification n) {
            NSTimestamp now=new NSTimestamp();
            if (log.isDebugEnabled())  log.debug("Timestamp for "+n.object()+": "+now);
            synchronized (_datesPerEcID) {
            	int num = System.identityHashCode(n.object());
            	_datesPerEcID.setObjectForKey(now, new Integer(num));
			}
        }
    }
    
    static {
        NSNotificationCenter center = NSNotificationCenter.defaultCenter();
        Observer observer = new Observer();
        ERXRetainer.retain(observer);
        center.addObserver(observer,
                new NSSelector("updateTimestampForEditingContext", ERXConstant.NotificationClassArray),
                ERXExtensions.objectsWillChangeInEditingContext,
                null);
    }        


    public EOEnterpriseObject insertionLogEntry=null;

    protected Boolean _implementsLogEntryInterface;
    
    public boolean implementsLogEntryInterface() {
        if (_implementsLogEntryInterface == null) {
            if (this instanceof ERCLogEntryInterface) {
                _implementsLogEntryInterface = Boolean.TRUE;
            } else {
                _implementsLogEntryInterface = Boolean.FALSE;                
            }
        }
        return _implementsLogEntryInterface.booleanValue();
    }
    
    public void init(EOEditingContext ec) {
        super.init(ec);
        if (implementsLogEntryInterface()) {
            String relationshipName = ((ERCLogEntryInterface)this).relationshipNameForLogEntry();
            EOEnterpriseObject logType = ((ERCLogEntryInterface)this).logEntryType();
            if (relationshipName != null && logType != null) {
                insertionLogEntry=ERCLogEntry.createLogEntryLinkedToEO(logType, null, this, relationshipName);
            }
        }
        // We now set the date created/last modified in willInsert/Update/Delete
        // A side effect of this technique is that for new EOs, created/lastModified is null until the EO actually gets saved
        // which means it'll fail the validation
        // two options: either we poke a value in those attributes here (even though it will be modified in willInsert,
        // or we make the property keys not mandatory
        // I am option for the former.
        NSTimestamp t=new NSTimestamp();
        setCreated(t);
        setLastModified(t);
    }

    public void willInsert() {
        super.willInsert();
        touch();
        setCreated(lastModified());
    }

    public void willUpdate() {
        super.willUpdate();
        touch();
    }

    public void willDelete() {
        // this in theory should not have much effect
        // however EOF seems to have trouble with some cascade configuration
        // this will maybe help track them down
        super.willDelete();
        touch();
    }

    
    private void touch() {
        Number n=ERXConstant.integerForInt(System.identityHashCode(editingContext()));
        if(n==null){
            log.error("Null number n in touch() for:"+this);
            log.error("editingContext:"+editingContext());
            log.error("System.identityHashCode(editingContext()):"+System.identityHashCode(editingContext()));
        }
        NSTimestamp date;
        synchronized (_datesPerEcID) {
        	date = (NSTimestamp)_datesPerEcID.objectForKey(n);
		}
        if (date==null) {
            log.error("Null modification date found in touch() call - EC delegate is probably missing");
        }
        setLastModified(date);
    }
    
    public void addObjectToBothSidesOfLogEntryRelationshipWithKey(EOEnterpriseObject object,
                                                                  String key) {
        // if we said insertionLogEntry=null in validateForInsert, we run the risk of
        // the save failing and the user making a modif, which then would not be
        // propagated to the log entry.
        if (insertionLogEntry!=null && editingContext().insertedObjects().containsObject(this))
            insertionLogEntry.addObjectToBothSidesOfRelationshipWithKey(object,key);
    }

    public NSTimestamp created() { return (NSTimestamp)storedValueForKey(TimestampAttributeKeys[0]); }
    public void setCreated(NSTimestamp value) { takeStoredValueForKey(value, TimestampAttributeKeys[0]); }

    public NSTimestamp lastModified() { return (NSTimestamp)storedValueForKey(TimestampAttributeKeys[1]); }
    public void setLastModified(NSTimestamp value) { takeStoredValueForKey(value, TimestampAttributeKeys[1]); }
}
