/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import java.util.*;

import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

/**
 * This delegate implements several methods from the formal interface
 * {@link com.webobjects.eoaccess.EODatabaseContext.Delegate EODatabaseContext.Delegate}. 
 * Of special note this class adds the ability
 * for enterpriseobjects to generate their own primary keys, correctly throws an
 * exception when a toOne relationship object is not found in the database and adds
 * debugging abilities to tracking down when faults are fired. It also supports a cache for
 * array fault that is checked before they are fetched from the database.
 */
public class ERXDatabaseContextDelegate {
             
    /** Basic logging support */
    public final static ERXLogger log = ERXLogger.getERXLogger(ERXDatabaseContextDelegate.class);
    /** Faulting logging support, logging category: <b>er.transaction.adaptor.FaultFiring</b> */
    public final static ERXLogger dbLog = ERXLogger.getERXLogger("er.transaction.adaptor.FaultFiring");
    /** Faulting logging support, logging category: <b>er.transaction.adaptor.Exceptions</b> */
    public final static ERXLogger exLog = ERXLogger.getERXLogger("er.transaction.adaptor.Exceptions");

    /** Holds onto the singleton of the default delegate */
    private static ERXDatabaseContextDelegate _defaultDelegate = new ERXDatabaseContextDelegate();
    
    private ERXArrayFaultCache arrayFaultCache = null;
    
    /** Returns the singleton of the database context delegate */
    public static ERXDatabaseContextDelegate defaultDelegate() {
        return _defaultDelegate;
    }

    public ERXArrayFaultCache arrayFaultCache() {
        return arrayFaultCache;
    }

    public void setArrayFaultCache(ERXArrayFaultCache value) {
        arrayFaultCache = value;
    }

    /**
     * Provides for a hook to get at the original exceptions from the JDBC driver, as opposed to the cooked
     * EOGeneralAdaptorException you get from EOF. To see the exceptions, set the logger 
     * er.transaction.adaptor.Exceptions to debug. 
     * @param databaseContext
     * @param throwable
     * @return
     */
    public boolean databaseContextShouldHandleDatabaseException(EODatabaseContext databaseContext, Throwable throwable) {
        if(exLog.isDebugEnabled()) {
            exLog.debug("JDBC Exception occured: " + throwable, throwable);
        }
        return true;
    }
    
    /**
     * Provides the ability for new enterprise objects that implement the interface {@link ERXGeneratesPrimaryKeyInterface}
     * to provide their own primary key dictionary. If the enterprise object implements the above interface then the
     * method <code>primaryKeyDictionary(true)</code> will be called on the object. If the object returns null then a
     * primary key will be generated for the object in the usual fashion.
     * @param databaseContext databasecontext
     * @param object the new enterprise object
     * @param entity the entity of the object
     * @return primary key dictionary to be used or null if a primary key should be generated for the object.
     */
    public NSDictionary databaseContextNewPrimaryKey(EODatabaseContext databaseContext, Object object, EOEntity entity) {
        return object instanceof ERXGeneratesPrimaryKeyInterface ? ((ERXGeneratesPrimaryKeyInterface)object).primaryKeyDictionary(true) : null;
    }

    /**
     * Allows custom handling of dropped connection exceptions. This was needed in WebObjects 4.5 because the
     * OracleEOAdaptor wouldn't correctly handle all exceptions of dropped connections. This may not be needed
     * now.
     * @param dbc current database context
     * @param e throw exception
     * @return if the exception is one of the bad ones that isn't handled then the method <code>handleDroppedConnection</code>
     *         is called directly on the database object of the context and <code>false</code> is returned otherwise <code>true</code>.
     */
    // CHECKME: Is this still needed now?
    public boolean databaseContextShouldHandleDatabaseException(EODatabaseContext dbc, Exception e) throws Throwable {
        EOAdaptor adaptor=dbc.adaptorContext().adaptor();
        boolean shouldHandleConnection = false;
        if(e instanceof EOGeneralAdaptorException)
            log.error(((EOGeneralAdaptorException)e).userInfo());
        else
            log.error(e);
        if (adaptor.isDroppedConnectionException(e))
            shouldHandleConnection = true;
        // FIXME: Should provide api to extend the list of bad exceptions.
        else if (e.toString().indexOf("ORA-01041")!=-1) {
            // just returning true here does not seem to do the trick. why !?!?
            log.error("ORA-01041 detecting -- forcing reconnect");
            dbc.database().handleDroppedConnection();
            shouldHandleConnection = false;
        } else {
            if(e instanceof EOGeneralAdaptorException)
                log.info(((EOGeneralAdaptorException)e).userInfo());
            throw e;
        }
        return shouldHandleConnection;
    }

    /**
     * This is Kelly Hawks' fix for the missing to one relationship. 
     * Delegate on EODatabaseContext that gets called when a to-one fault cannot find its data in
     * the database. The object that is returned is a cleared fault.
     * We raise here to restore the functionality that existed prior to WebObjects 4.5.
     * Whenever a fault fails for a globalID (i.e. the object is NOT found in the database), we raise
     * an {@link com.webobjects.eoaccess.EOObjectNotAvailableException EOObjectNotAvailableException}.
     * @param context database context
     * @param object object that is firing the fault for a given to-one relationship
     * @param gid global id that wasn't found in the database.
     */
    public boolean databaseContextFailedToFetchObject(EODatabaseContext context, Object object, EOGlobalID gid) {
        if (object!=null) {
            EOEditingContext ec = ((EOEnterpriseObject)object).editingContext();

            // we need to refault the object before raising, otherwise, if the caller traps
            // the exception, it will be a successful lookup the next time a fault with the
            // same global id fires.  NOTE: refaulting in a sharedEditingContext is illegal,
            // so we specifically check for that special case.

            if (!(ec instanceof EOSharedEditingContext)) {
                context.refaultObject((EOEnterpriseObject)object, gid, ec);
            }
        }
        String gidString;
        if(gid instanceof EOKeyGlobalID) {
            // ak: when you use 24 byte PKs, the output is unreadable otherwise 
            EOKeyGlobalID kgid = (EOKeyGlobalID)gid;
            gidString = "<" +  kgid.entityName() + ": [" ;
            EOEntity entity = ERXEOAccessUtilities.entityNamed(null, kgid.entityName());
            NSArray pks = entity.primaryKeyAttributes();
            NSArray values = kgid.keyValuesArray();
            EOSQLExpression expression = context.database().adaptor().expressionFactory().expressionForEntity(entity);
            for(int i = 0; i < pks.count(); i++) {
                Object value = values.objectAtIndex(i);
                EOAttribute attribute = (EOAttribute) pks.objectAtIndex(i);
                gidString += attribute.name() + ": \'" +  expression.formatValueForAttribute(value, attribute) + "\'"
                + (i == pks.count() - 1 ? "" : ", ");
            }
            gidString += "] >";
            
        } else {
            gidString = gid.toString();
        }
        throw new EOObjectNotAvailableException("No " + (object!=null ? object.getClass().getName() : "N/A") + " found with globalID: " + gidString);            
    }
    
    /**
     * This delegate method is called every time a fault is fired that needs
     * to go to the database. All we have added is logging statement of the
     * debug priority. This way during runtime a developer can toggle the
     * logger priority settting on and off to see what faults are firing. Also
     * note that when using {@link ERXPatternLayout} one can set the option to
     * see full backtraces to the calling method. With this option specified
     * a developer can see exactly which methods are firing faults.
     * @param dc the databasecontext
     * @param fs the fetchspecification
     * @param channel the databasechannel
     */
    public void databaseContextDidSelectObjects(EODatabaseContext dc,
                                                EOFetchSpecification fs,
                                                EODatabaseChannel channel) {
        if (dbLog.isDebugEnabled()) {
            dbLog.debug("databaseContextDidSelectObjects " + fs, new Exception());
        }
    }

   /**
    * This delegate method first checks the arrayFaultCache if it is set before trying to
    * resolve the fault from the DB. It can be a severe performance optimisation depending
    * on your setup.
    * @param eodatabasecontext
    * @param obj
    * @return
    */
    public boolean databaseContextShouldFetchArrayFault(EODatabaseContext eodatabasecontext, Object obj) {
        if(arrayFaultCache != null) {
            arrayFaultCache.clearFault(obj);
            if(!EOFaultHandler.isFault(obj)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Overridden to remove inserts and deletes of the "same" row. When you
     * delete, from a join table and then re-add the same object, then the
     * order of operations would be insert, then delete and you will get
     * an error because the delete would try to also delete the newly inserted
     * row. Here we just check every insert and see if the deleted contain the same
     * object. If they do, we just skip both operations,
     * @author chello team!
     * @param dbCtxt
     * @param adaptorOps
     * @param adChannel
     * @return
     */
    public NSArray databaseContextWillPerformAdaptorOperations(EODatabaseContext dbCtxt, 
    		NSArray adaptorOps, EOAdaptorChannel adChannel) {
    	NSMutableArray result = new NSMutableArray();
    	NSDictionary groupedOps = ERXArrayUtilities.arrayGroupedByKeyPath(adaptorOps, "adaptorOperator");
    	Integer insertKey = new Integer(EODatabaseOperation.AdaptorInsertOperator);
		NSArray insertOps = (NSArray) groupedOps.objectForKey(insertKey);
		Integer deleteKey = new Integer(EODatabaseOperation.AdaptorDeleteOperator);
		NSArray deleteOps = (NSArray) groupedOps.objectForKey(deleteKey);
		NSMutableSet skippedOps = new NSMutableSet();
		
		if (insertOps!=null) {
			for(Enumeration e = insertOps.objectEnumerator(); e.hasMoreElements();) {
				EOAdaptorOperation insertOp = (EOAdaptorOperation)e.nextElement();
				for(Enumeration e1 = deleteOps.objectEnumerator(); e1.hasMoreElements();) {
					EOAdaptorOperation deleteOp = (EOAdaptorOperation)e1.nextElement();
					if(!skippedOps.containsObject(deleteOp)) {
						if(insertOp.entity() == deleteOp.entity()) {
							if(deleteOp.qualifier().evaluateWithObject(insertOp.changedValues())) {
								if(false) {
									// here we remove both the delete and the 
									// insert. this might fail if we didn't lock on all rows
									// FIXME: check the current snapshot in the database and
									// see if it is the same as the new insert
									
									skippedOps.addObject(deleteOp);
									skippedOps.addObject(insertOp);
								} else {
									// here we put the delete up front, this might fail if
									// we have cascading delete rules in the database
									result.addObject(deleteOp);
									skippedOps.addObject(deleteOp);
								}
								log.warn("Skipped: " + insertOp + "\n" + deleteOp);
							}
						}
					}
				}
			}
			for(Enumeration e = adaptorOps.objectEnumerator(); e.hasMoreElements();) {
				EOAdaptorOperation op = (EOAdaptorOperation)e.nextElement();
				if(!skippedOps.containsObject(op)) {
					result.addObject(op);
				}
			}
		}
		return result;
    }
}
