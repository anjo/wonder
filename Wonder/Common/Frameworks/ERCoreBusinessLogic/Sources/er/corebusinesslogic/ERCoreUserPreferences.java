/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.corebusinesslogic;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.appserver.*;
import er.extensions.*;
import java.util.Enumeration;

public class ERCoreUserPreferences implements NSKeyValueCoding {

    //	===========================================================================
    //	Class Constant(s)
    //	---------------------------------------------------------------------------    
    
    /** Logging support */
    public static final ERXLogger log = ERXLogger.getERXLogger(ERCoreUserPreferences.class);

    /** EOEncoding key */
    private final static String VALUE="_V";

    /** Notification that is posted when preferences change */
    public final static String PreferenceDidChangeNotification = "PreferenceChangedNotification";

    //	===========================================================================
    //	Class Variable(s)
    //	---------------------------------------------------------------------------    

    /** caches the singleton user preference object */
    private static ERCoreUserPreferences _userPreferences;

    //	===========================================================================
    //	Class Method(s)
    //	---------------------------------------------------------------------------

    /**
     * Gets the singleton instance for interacting with
     * the user preference system.
     * @return single instance of the user preferences
     */
    public static ERCoreUserPreferences userPreferences() {
        if (_userPreferences == null)
            _userPreferences = new ERCoreUserPreferences();
        return _userPreferences;
    }

    //	===========================================================================
    //	Instance Method(s)
    //	---------------------------------------------------------------------------    

    /**
     * Registers notification handlers for user preference notifications. These
     * are mainly used within the context of D2W pages.
     */
    public void registerHandlers() {
        log.debug("Registering preference handlers");
        _UserPreferenceHandler handler = new _UserPreferenceHandler();
        ERXRetainer.retain(handler);
        NSNotificationCenter.defaultCenter().addObserver(handler,
                                                         new NSSelector("handleBatchSizeChange", ERXConstant.NotificationClassArray),
                                                         ERXBatchNavigationBar.BatchSizeChanged,
                                                         null);
        NSNotificationCenter.defaultCenter().addObserver(handler,
                                                         new NSSelector("handleSortOrderingChange", ERXConstant.NotificationClassArray),
                                                         ERXSortOrder.SortOrderingChanged,
                                                         null);
        
    }
    
    protected NSArray preferences(EOEditingContext ec) {
        ERCoreUserInterface user = (ERCoreUserInterface)ERCoreBusinessLogic.actor(ec);
        return user!=null ? user.preferences() : NSArray.EmptyArray;
    }

    protected EOEnterpriseObject preferenceRecordForKey(String key, EOEditingContext ec) {
        EOEnterpriseObject result=null;
        if (key != null) {
            if (log.isDebugEnabled())
                log.debug("Preference value for Key = "+key);
            for (Enumeration e = preferences(ec).objectEnumerator(); e.hasMoreElements();) {
                EOEnterpriseObject pref = (EOEnterpriseObject)e.nextElement();
                String prefKey = (String)pref.valueForKey("key");
                if (log.isDebugEnabled()) {
                    log.debug("prefKey \"" + prefKey + "\"");   
                }
                if (prefKey != null && prefKey.equals(key)) {
                    result = pref;
                    break;
                }
            }
        }
        return result;
    }

    protected String encodedValue(Object value) {
        EOKeyValueArchiver archiver = new EOKeyValueArchiver();
        archiver.encodeObject(value,VALUE);
        String encodedValue = NSPropertyListSerialization.stringFromPropertyList(archiver.dictionary());
        return encodedValue;
    }

    protected Object decodedValue(String encodedValue) {
        NSDictionary d = (NSDictionary )NSPropertyListSerialization.propertyListFromString(encodedValue);
        EOKeyValueUnarchiver u = new EOKeyValueUnarchiver(d);
        return u.decodeObjectForKey(VALUE);
    }    
    
    //	===========================================================================
    //	Implementation of NSKeyValueCoding
    //	---------------------------------------------------------------------------    
    
    // FIXME -- unarchiving - archiving probably could use optimization
    public Object valueForKey(String key) {
        Object result=null;
        EOEditingContext ec = ERXEC.newEditingContext();
        ec.lock();
        try {
            EOEnterpriseObject pref = preferenceRecordForKey(key, ec);
            if (pref != null) {
                String encodedValue = (String)pref.valueForKey("value");
                result = decodedValue(encodedValue);
            }
        } finally {
            ec.unlock();
        }
        ec.dispose();
        if (log.isDebugEnabled())
            log.debug("Prefs vfk " + key + " = " + result);
        return result;
    }

    public void takeValueForKey(Object value, String key) {
        Object result=null;
        // we first make sure there is no cruft left
        // !! locking is turned off on the value attribute of UserPreference
        // so that if a user opens two sessions they don't get locking failures
        // this is OK for display style prefs (how many items, how they are sorted)
        // but might not be for more behavior-style prefs!!
        EOEditingContext ec = ERXEC.newEditingContext();
        ec.lock();
        try {
            EOEnterpriseObject pref = preferenceRecordForKey(key, ec);
            ERCoreUserInterface u = (ERCoreUserInterface)ERCoreBusinessLogic.actor(ec);
            if (pref != null) {
                if (value != null) {
                    String encodedValue = encodedValue(value);
                    if (ERXExtensions.safeDifferent(encodedValue,pref.valueForKey("value"))) {
                        if (log.isDebugEnabled())
                            log.debug("Updating preference "+u+": "+key+"="+encodedValue);
                        pref.takeValueForKey(encodedValue,"value");
                    }
                } else {
                    if (log.isDebugEnabled())
                        log.debug("Removing preference "+u+": "+key);
                    ec.deleteObject(pref);
                }
            } else if (value!=null) {
                pref = ERXUtilities.createEO("ERCPreference", ec);
                u.newPreference(pref);
                // done this way to not force you to sub-class our User entity
                pref.takeValueForKey(ERXExtensions.rawPrimaryKeyForObject((EOEnterpriseObject)u),"userID");
                pref.takeValueForKey(key,"key");
                pref.takeValueForKey(encodedValue(value),"value");
                if (log.isDebugEnabled())
                    log.debug("Creating preference "+u+": "+key+" - "+value+" -- "+encodedValue(value));
            }
            if (ec.hasChanges()) {
                ec.saveChanges();
            }
        } finally {
            ec.unlock();
        }
        ec.dispose();
        NSNotificationCenter.defaultCenter().postNotification(PreferenceDidChangeNotification,
                                                              new NSDictionary(value, key));
    }

    public static class _UserPreferenceHandler {

        public void handleBatchSizeChange(NSNotification n) { handleChange("batchSize", n); }
        public void handleSortOrderingChange(NSNotification n) { handleChange("sortOrdering", n); }

        public void handleChange(String prefName, NSNotification n) {
            if (ERCoreBusinessLogic.actor() != null) {
                NSKeyValueCoding context=(NSKeyValueCoding)n.userInfo().objectForKey("d2wContext");
                if (context!=null && context.valueForKey("pageConfiguration") != null) {
                    userPreferences().takeValueForKey(n.object(),
                                                      prefName+"."+(String)context.valueForKey("pageConfiguration"));
                }
            }
        }
    }
}
