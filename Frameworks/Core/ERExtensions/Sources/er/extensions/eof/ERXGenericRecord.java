/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions.eof;

import java.util.Enumeration;

import org.apache.log4j.Logger;

import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOJoin;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOClassDescription;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFaultHandler;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOGenericRecord;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.eocontrol.EOObjectStoreCoordinator;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EORelationshipManipulation;
import com.webobjects.eocontrol.EOSharedEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSSelector;
import com.webobjects.foundation.NSValidation;

import er.extensions.ERXExtensions;
import er.extensions.crypting.ERXCrypto;
import er.extensions.foundation.ERXArrayUtilities;
import er.extensions.foundation.ERXDictionaryUtilities;
import er.extensions.foundation.ERXProperties;
import er.extensions.foundation.ERXUtilities;
import er.extensions.localization.ERXLocalizer;
import er.extensions.validation.ERXValidationException;
import er.extensions.validation.ERXValidationFactory;

/**
 * This class contains a bunch of extensions to the regular
 * {@link com.webobjects.eocontrol.EOGenericRecord EOGenericRecord} class. Of
 * notable interest is:
 * <ul>
 * <li> it contains built in support for generating primary keys via the
 * {@link ERXGeneratesPrimaryKeyInterface},
 * <li>support for an augmented transaction methods like <code>
 * willUpdate</code>
 * and <code>didDelete</code> and a bunch of handy utility methods like
 * <code>committedSnapshotValueForKey
 * </code>.
 * <li> At the moment it is required that those wishing to take advantage of
 * templatized and localized validation exceptions need to subclass this class.
 * Hopefully in the future we can get rid of this requirement. <br />
 * </ul>
 * Also, this class supports auto-updating of inverse relationships. You can
 * simply call <code>eo.setFoo(other), eo.takeValueForKey(other),
 * eo.addObjectToBothSidesOfRelationshipWithKey(other, "foo")</code>
 * or <code>eo.addToFoos(other)</code> and the inverse relationship will get
 * updated for you automagically, so that you don't need to call
 * <code>other.addToBars(eo)</code> or <code>other.setBar(eo)</code>. Doing
 * so doesn't hurt, though. Giving a <code>null</code> value of removing the
 * object from a to-many will result in the inverse relationship getting
 * cleared. <br />
 * If you *do* call addToBars(), you need to use
 * includeObjectIntoPropertyWithKey() in this method.<br>
 * This feature should greatly help readability and reduce the number errors you
 * make when you forget to update an inverse relationship. To turn this feature
 * on, you must set the system default
 * <code>er.extensions.ERXEnterpriseObject.updateInverseRelationships=true</code>.
 */
public class ERXGenericRecord extends EOGenericRecord implements ERXGuardedObjectInterface, ERXGeneratesPrimaryKeyInterface, ERXEnterpriseObject, ERXKey.ValueCoding {

	/** holds all subclass related Logger's */
	private static NSMutableDictionary<Class, Logger> classLogs = new NSMutableDictionary<Class, Logger>();

	public static boolean shouldTrimSpaces() {
		return ERXProperties.booleanForKeyWithDefault("er.extensions.ERXGenericRecord.shouldTrimSpaces", false);
	}

	public static boolean localizationShouldFallbackToDefaultLanguage() {
		return ERXProperties.booleanForKeyWithDefault("er.extensions.ERXGenericRecord.localizationShouldFallbackToDefaultLanguage", false);
	}
	
	public ERXGenericRecord(EOClassDescription classDescription) {
		super(classDescription);
	}
	
	public ERXGenericRecord() {
		super();
	}

	/**
	 * Returns all available ERXLanguages for the given key
	 * @param key
	 * @return NSArray of language/locale keys
	 */
	@SuppressWarnings("unchecked")
	public NSArray<String> localesForKey(String key) {
		NSArray<String> result = NSArray.<String>emptyArray();
		EOClassDescription cd = classDescription();
		if (cd instanceof ERXEntityClassDescription) {
			ERXEntityClassDescription ecd = (ERXEntityClassDescription) cd;
			EOAttribute attribute = ecd.entity().attributeNamed(key);
			if (attribute == null) {
				attribute = ecd.entity().attributeNamed(localizedKey(key));
			}
			return (NSArray<String>) attribute.userInfo().objectForKey("ERXLanguages");
		}
		return result;
	}

	/**
	 * Type-safe KVC getter (final for now)
	 * @param <T>
	 * @param key
	 */
	@SuppressWarnings("unchecked")
	public final <T> T valueForKey(ERXKey<T> key) {
		return (T)valueForKeyPath(key.key());
	}

	/**
	 * Type-safe KVC setter (final for now)
	 * @param <T>
	 * @param value
	 * @param key
	 */
	public final <T> void takeValueForKey(Object value, ERXKey<T> key) {
		takeValueForKeyPath(value, key.key());
	}
	
	private String localizedKey(String key) {
		EOClassDescription cd = classDescription();
		if (cd instanceof ERXEntityClassDescription) {
			return ((ERXEntityClassDescription) cd).localizedKey(key);
		}
		return null;
	}

	public static class LocalizedBinding extends NSKeyValueCoding._KeyBinding {

		public LocalizedBinding(String key) {
			super(null, key);
		}

		@Override
		public Object valueInObject(Object object) {
			ERXGenericRecord eo = (ERXGenericRecord) object;
			String localizedKey = eo.localizedKey(_key);
			Object value = eo.valueForKey(localizedKey);
			if (localizedKey != null && (value == null || "".equals(value)) && localizationShouldFallbackToDefaultLanguage()) {
				ERXLocalizer currentLocalizer = ERXLocalizer.currentLocalizer();
				String defaultLanguage = ERXLocalizer.defaultLanguage();
				if (!currentLocalizer.language().equals(defaultLanguage)) {
					if (log.isDebugEnabled()) {
						log.debug("no data found for '" + eo.entityName() + ':' + _key + "' for language " + currentLocalizer.language() + ", trying " + defaultLanguage);
					}
					ERXLocalizer.setCurrentLocalizer(ERXLocalizer.localizerForLanguage(defaultLanguage));
					value = eo.valueForKey(eo.localizedKey(_key));
					ERXLocalizer.setCurrentLocalizer(currentLocalizer);
				}
			}
			return value;
		}

		@Override
		public void setValueInObject(Object value, Object object) {
			ERXGenericRecord eo = (ERXGenericRecord) object;
			String localizedKey = eo.localizedKey(_key);
			eo.takeValueForKey(value, localizedKey);
		}
	}

	@Override
	public NSKeyValueCoding._KeyBinding _otherStorageBinding(String key) {
		NSKeyValueCoding._KeyBinding result;
		String localizedKey = localizedKey(key);
		if (localizedKey != null) {
			result = new LocalizedBinding(key);
		}
		else {
			result = super._otherStorageBinding(key);
		}
		return result;
	}

	/**
	 * Clazz object implementation for ERXGenericRecord. See
	 * {@link EOEnterpriseObjectClazz} for more information on this neat design
	 * pattern.
	 */
	public static class ERXGenericRecordClazz<T extends EOEnterpriseObject> extends EOEnterpriseObjectClazz<T> {

	}

	protected String insertionStackTrace = null;

	private boolean _updateInverseRelationships = ERXGenericRecord.InverseRelationshipUpdater.updateInverseRelationships;

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#getClassLog()
	 */
	public Logger getClassLog() {
		Logger classLog = classLogs.objectForKey(this.getClass());
		if (classLog == null) {
			synchronized (classLogs) {
				classLog = Logger.getLogger(this.getClass());
				classLogs.setObjectForKey(classLog, this.getClass());
			}
		}
		return classLog;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#self()
	 */
	public ERXEnterpriseObject self() {
		return this;
	}

	/**
	 * Implementation of {@link ERXGuardedObjectInterface}. This is checked
	 * before the object is deleted in the <code>willDelete</code> method
	 * which is in turn called by {@link ERXEditingContextDelegate}. The
	 * default implementation returns <code>true</code>.
	 * 
	 * @return true
	 */
	public boolean canDelete() {
		return true;
	}

	/**
	 * Implementation of {@link ERXGuardedObjectInterface}. This is checked
	 * before the object is deleted in the <code>willUpdate</code> method
	 * which is in turn called by {@link ERXEditingContextDelegate}. The
	 * default implementation returns <code>true</code>.
	 * 
	 * @return true
	 */
	public boolean canUpdate() {
		return true;
	}

	/**
	 * Implementation of {@link ERXGuardedObjectInterface}. This is used to
	 * work around a bug in EOF that doesn't refresh the relationship in the
	 * parent editingContext for the object.
	 */
	public void delete() {
		editingContext().deleteObject(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#mightDelete()
	 */
	public void mightDelete() {
		if (tranLogMightDelete.isDebugEnabled())
			tranLogMightDelete.debug("Object:" + description());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#willDelete()
	 */
	public void willDelete() throws NSValidation.ValidationException {
		if (canDelete() == false) {
			throw ERXValidationFactory.defaultFactory().createException(this, null, null, "ObjectCannotBeDeletedException");
		}
		if (tranLogWillDelete.isDebugEnabled())
			tranLogWillDelete.debug("Object:" + description());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#willInsert()
	 */
	public void willInsert() {
		/*
		 * Disabling this check by default -- it's causing problems for objects
		 * created and deleted in the same transaction
		 */
		if (tranLogWillInsert.isDebugEnabled()) {
			/* check that all the to manies have an array */
			for (Enumeration e = toManyRelationshipKeys().objectEnumerator(); e.hasMoreElements();) {
				String key = (String) e.nextElement();
				Object o = storedValueForKey(key);
				if (o == null || !EOFaultHandler.isFault(o) && o instanceof NSKeyValueCoding.Null) {
					tranLogWillInsert.error("Found illegal value in to many " + key + " for " + this + ": " + o);
				}
			}
			tranLogWillInsert.debug("Object:" + description());
		}
		if (shouldTrimSpaces())
			trimSpaces();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#willUpdate()
	 */
	public void willUpdate() {
		if (canUpdate() == false) {
			throw ERXValidationFactory.defaultFactory().createException(this, null, null, "ObjectCannotBeUpdatedException");
		}
		/*
		 * Disabling this check by default -- it's causing problems for objects
		 * created and deleted in the same transaction
		 */
		if (tranLogWillUpdate.isDebugEnabled()) {
			/* check that all the to manies have an array */
			for (Enumeration e = toManyRelationshipKeys().objectEnumerator(); e.hasMoreElements();) {
				String key = (String) e.nextElement();
				Object o = storedValueForKey(key);
				if (o == null || !EOFaultHandler.isFault(o) && o instanceof NSKeyValueCoding.Null) {
					tranLogWillUpdate.error("Found illegal value in to many " + key + " for " + this + ": " + o);
				}
			}
			if (tranLogWillUpdate.isDebugEnabled())
				tranLogWillUpdate.debug("Object:" + description() + " changes: " + changesFromCommittedSnapshot());
		}
		if (shouldTrimSpaces())
			trimSpaces();
	}

	protected boolean _updateInverseRelationships() {
		return _updateInverseRelationships && !(editingContext() instanceof EOSharedEditingContext);
	}
	
	public boolean _setUpdateInverseRelationships(boolean newValue) {
		boolean old = _updateInverseRelationships;
		_updateInverseRelationships = newValue;
		return old;
	}

	@Override
	public Object willReadRelationship(Object aObject) {
		boolean old = _setUpdateInverseRelationships(false);
		try {
			return super.willReadRelationship(aObject);
		}
		finally {
			_setUpdateInverseRelationships(old);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#flushCaches()
	 */
	public void flushCaches() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#didDelete(com.webobjects.eocontrol.EOEditingContext)
	 */
	public void didDelete(EOEditingContext ec) {
		if (tranLogDidDelete.isDebugEnabled())
			tranLogDidDelete.debug("Object:" + description());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#didUpdate()
	 */
	public void didUpdate() {
		if (tranLogDidUpdate.isDebugEnabled())
			tranLogDidUpdate.debug("Object:" + description());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#didInsert()
	 */
	public void didInsert() {
		if (tranLogDidInsert.isDebugEnabled())
			tranLogDidInsert.debug("Object:" + description());
		_permanentGlobalID = null;
		
		//We're goung to blow the primaryKey cache:
		_primaryKey = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#willRevert()
	 */
	public void willRevert() {
		if (tranLogWillRevert.isDebugEnabled())
			tranLogWillRevert.debug("Object: " + description());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#didRevert(com.webobjects.eocontrol.EOEditingContext)
	 */
	public void didRevert(EOEditingContext ec) {
		if (tranLogDidRevert.isDebugEnabled())
			tranLogDidRevert.debug("Object: " + description());
		flushCaches();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#addObjectsToBothSidesOfRelationshipWithKey(com.webobjects.foundation.NSArray,
	 *      java.lang.String)
	 */
	public void addObjectsToBothSidesOfRelationshipWithKey(NSArray objects, String key) {
		if (objects != null && objects.count() > 0) {
			NSArray objectsSafe = objects instanceof NSMutableArray ? (NSArray) objects.immutableClone() : objects;
			for (Enumeration e = objectsSafe.objectEnumerator(); e.hasMoreElements();) {
				EOEnterpriseObject eo = (EOEnterpriseObject) e.nextElement();
				addObjectToBothSidesOfRelationshipWithKey(eo, key);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#removeObjectsFromBothSidesOfRelationshipWithKey(com.webobjects.foundation.NSArray,
	 *      java.lang.String)
	 */
	public void removeObjectsFromBothSidesOfRelationshipWithKey(NSArray objects, String key) {
		if (objects != null && objects.count() > 0) {
			NSArray objectsSafe = objects instanceof NSMutableArray ? (NSArray) objects.immutableClone() : objects;
			for (Enumeration e = objectsSafe.objectEnumerator(); e.hasMoreElements();) {
				EOEnterpriseObject eo = (EOEnterpriseObject) e.nextElement();
				removeObjectFromBothSidesOfRelationshipWithKey(eo, key);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#removeObjectsFromPropertyWithKey(com.webobjects.foundation.NSArray,
	 *      java.lang.String)
	 */
	public void removeObjectsFromPropertyWithKey(NSArray objects, String key) {
		if (objects != null && objects.count() > 0) {
			NSArray objectsSafe = objects instanceof NSMutableArray ? (NSArray) objects.immutableClone() : objects;
			for (Enumeration e = objectsSafe.objectEnumerator(); e.hasMoreElements();) {
				EOEnterpriseObject eo = (EOEnterpriseObject) e.nextElement();
				removeObjectFromPropertyWithKey(eo, key);
			}
		}
	}

	/**
	 * By default, and this should change in the future, all editing contexts
	 * that are created and use ERXEnterpriseObjects or subclasses need to have
	 * a delegate set of instance {@link ERXEditingContextDelegate}. These
	 * delegates provide the augmentation to the regular transaction mechanism,
	 * all of the will* methods plus the flushCaching method. To change the
	 * default behaviour set the property:
	 * <b>er.extensions.ERXRaiseOnMissingEditingContextDelegate</b> to false in
	 * your WebObjects.properties file. This method is called when an object is
	 * fetched, updated or inserted.
	 * 
	 * @param editingContext
	 *            to check for the correct delegate.
	 * @return if the editing context has the correct delegate set.
	 */
	private boolean _checkEditingContextDelegate(EOEditingContext editingContext) {
		return ERXEditingContextDelegate._checkEditingContextDelegate(editingContext);
	}

	/**
	 * Checks the editing context delegate before calling super's
	 * implementation. See the method <code>
	 * _checkEditingContextDelegate</code>
	 * for an explanation as to what this check does.
	 * 
	 * @param editingContext
	 *            to be checked to make sure it has the correct type of delegate
	 *            set.
	 */
	@Override
	public void awakeFromClientUpdate(EOEditingContext editingContext) {
		_checkEditingContextDelegate(editingContext);
		super.awakeFromClientUpdate(editingContext);
	}

	/**
	 * Checks the editing context delegate before calling super's
	 * implementation. See the method <code>
	 * _checkEditingContextDelegate</code>
	 * for an explanation as to what this check does.
	 * 
	 * @param editingContext
	 *            to be checked to make sure it has the correct type of delegate
	 *            set.
	 */
	@Override
	public void awakeFromInsertion(EOEditingContext editingContext) {
		boolean old = _setUpdateInverseRelationships(false);
		try {
			_checkEditingContextDelegate(editingContext);
			if (insertionTrackingLog.isDebugEnabled()) {
				insertionStackTrace = ERXUtilities.stackTrace();
				insertionTrackingLog.debug("inserted " + getClass().getName() + " at " + insertionStackTrace);
			}
			super.awakeFromInsertion(editingContext);
		}
		finally {
			_setUpdateInverseRelationships(old);
		}
		EOGlobalID gid = editingContext.globalIDForObject(this);
		if (gid.isTemporary()) {
			init(editingContext);
			if (applyRestrictingQualifierOnInsert()) {
				EOEntity entity = ERXEOAccessUtilities.entityNamed(editingContext, entityName());
				EOQualifier restrictingQualifier = entity.restrictingQualifier();
				if (restrictingQualifier != null) {
					ERXEOControlUtilities.makeQualifierTrue(restrictingQualifier, this);
				}
			}
		}
	}

	protected boolean applyRestrictingQualifierOnInsert() {
		return ERXEnterpriseObject.applyRestrictingQualifierOnInsert;
	}

	@Override
	public void clearProperties() {
		boolean old = _setUpdateInverseRelationships(false);
		try {
			super.clearProperties();
		}
		finally {
			_setUpdateInverseRelationships(old);
		}
	}

	/**
	 * used for initialization stuff instead of awakeFromInsertion.
	 * <code>awakeFromInsertions</code> is buggy because if an EO is deleted
	 * and then its EOEditingContext is reverted using 'revert' for example then
	 * EOF will -insert- this EO again in its EOEditingContext which in turn
	 * calls awakeFromInsertion again.
	 * 
	 * @param ec
	 *            the EOEditingContext in which this new EO is inserted
	 */
	protected void init(EOEditingContext ec) {

	}

	/**
	 * Checks the editing context delegate before calling super's
	 * implementation. See the method <code>
	 * _checkEditingContextDelegate</code>
	 * for an explanation as to what this check does.
	 * 
	 * @param editingContext
	 *            to be checked to make sure it has the correct type of delegate
	 *            set.
	 */
	@Override
	public void awakeFromFetch(EOEditingContext editingContext) {
		boolean old = _setUpdateInverseRelationships(false);
		try {
			_checkEditingContextDelegate(editingContext);
			super.awakeFromFetch(editingContext);
		}
		finally {
			_setUpdateInverseRelationships(old);
		}
	}

	/**
	 * Adds a check to make sure that both the object being added and this
	 * object are in the same editing context. If not then a runtime exception
	 * is thrown instead of getting the somewhat cryptic NSInternalInconsistency
	 * excpetion that is thrown when you attempt to save changes to the
	 * database.
	 * 
	 * @param eo
	 *            enterprise object to be added to the relationship
	 * @param key
	 *            relationship to add the object to.
	 */
	@Override
	public void addObjectToBothSidesOfRelationshipWithKey(EORelationshipManipulation eo, String key) {
		ERXGenericRecord.checkMatchingEditingContexts(this, key, (EOEnterpriseObject) eo);
		super.addObjectToBothSidesOfRelationshipWithKey(eo, key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#primaryKey()
	 */
	protected String _primaryKey = null;
	public String primaryKey() {
	  if (_primaryKey == null) {
	    _primaryKey = ERXEOControlUtilities.primaryKeyStringForObject(this);
	  }
	  return _primaryKey;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#rawPrimaryKeyInTransaction()
	 */
	public Object rawPrimaryKeyInTransaction() {
		Object result = rawPrimaryKey();
		if (result == null) {
			NSDictionary pk = primaryKeyDictionary(false);
			NSArray primaryKeyAttributeNames = primaryKeyAttributeNames();
			result = ERXArrayUtilities.valuesForKeyPaths(pk, primaryKeyAttributeNames);
			if (((NSArray) result).count() == 1)
				result = ((NSArray) result).lastObject();
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#primaryKeyInTransaction()
	 */
	public String primaryKeyInTransaction() {
		return ERXEOControlUtilities._stringForPrimaryKey(rawPrimaryKeyInTransaction());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#rawPrimaryKey()
	 */
	public Object rawPrimaryKey() {
		return ERXEOControlUtilities.primaryKeyObjectForObject(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#encryptedPrimaryKey()
	 */
	public String encryptedPrimaryKey() {
		String pk = ERXEOControlUtilities.primaryKeyStringForObject(this);
		return pk == null ? null : ERXCrypto.blowfishEncode(pk);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#foreignKeyForRelationshipWithKey(java.lang.String)
	 */
	public Object foreignKeyForRelationshipWithKey(String rel) {
		NSDictionary d = EOUtilities.destinationKeyForSourceObject(editingContext(), this, rel);
		return d != null && d.count() > 0 ? d.allValues().objectAtIndex(0) : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#primaryKeyAttributeNames()
	 */
	@SuppressWarnings("unchecked")
	public NSArray<String> primaryKeyAttributeNames() {
		return entity().primaryKeyAttributeNames();
	}

	/**
	 * Returns the entity for the current object. Defers to
	 * {@link ERXEOAccessUtilities#entityNamed ERXEOAccessUtilities.entityNamed()}
	 * for the actual work.
	 * 
	 * @return EOEntity for the current object
	 */
	public EOEntity entity() {
		return ERXEOAccessUtilities.entityNamed(editingContext(), entityName());
	}

	/** caches the primary key dictionary for the given object */
	private NSDictionary _primaryKeyDictionary;

	/**
	 * Implementation of the interface {@link ERXGeneratesPrimaryKeyInterface}.
	 * This implementation operates in the following fashion. If it is called
	 * passing in 'false' and it has not yet been saved to the database, meaning
	 * this object does not yet have a primary key assigned, then it will have
	 * the adaptor channel generate a primary key for it. Then when the object
	 * is saved to the database it will use the previously generated primary key
	 * instead of having the adaptor channel generate another primary key. If
	 * 'true' is passed in then this method will either return the previously
	 * generated primaryKey dictionary or null if it does not have one.
	 * Typically you should only call this method with the 'false' parameter
	 * seeing as unless you are doing something really funky you won't be
	 * dealing with this object when it is in the middle of a transaction. The
	 * delegate {@link ERXDatabaseContextDelegate} is the only class that should
	 * be calling this method and passing in 'true'.
	 * 
	 * @param inTransaction
	 *            boolean flag to tell the object if it is currently in the
	 *            middle of a transaction.
	 * @return primary key dictionary for the current object, if the object does
	 *         not have a primary key assigned yet and is not in the middle of a
	 *         transaction then a new primary key dictionary is created, cached
	 *         and returned.
	 */
	// FIXME: this method is really misnamed; it should be called
	// rawPrimaryKeyDictionary
	@SuppressWarnings("unchecked")
	public NSDictionary<String, Object> primaryKeyDictionary(boolean inTransaction) {
		if (_primaryKeyDictionary == null) {
			if (!inTransaction) {
				Object rawPK = rawPrimaryKey();
				if (rawPK != null) {
					if (log.isDebugEnabled())
						log.debug("Got raw key: " + rawPK);
					NSArray<String> primaryKeyAttributeNames = primaryKeyAttributeNames();
					_primaryKeyDictionary = new NSDictionary<String, Object>(rawPK instanceof NSArray ? (NSArray<Object>) rawPK : new NSArray<Object>(rawPK), primaryKeyAttributeNames);
				}
				else {
					EOEntity entity = entity();
					NSArray<EOAttribute> primaryKeyAttributes = entity.primaryKeyAttributes();
					// If the entity has a composite primary key, then we are
					// going to make
					// the assumption that we can determine its primary key from
					// the values
					// of the relationships that are bound to its primary key
					// attributes rather
					// than attempting to generate a PK with the plugin.
					if (primaryKeyAttributes.count() > 1) {
						NSMutableDictionary<String, Object> compositePrimaryKey = new NSMutableDictionary<String, Object>();
						boolean incompletePK = false;
						for (EOAttribute primaryKeyAttribute : primaryKeyAttributes) {
							Object value = null;
							for (EORelationship relationship : (NSArray<EORelationship>) entity.relationships()) {
								// .. we need to find the relationship that is
								// associated with each PK attribute
								if (relationship._isToOneClassProperty() && relationship.sourceAttributes().contains(primaryKeyAttribute)) {
									Object obj = valueForKey(relationship.name());
									if (obj instanceof ERXGenericRecord) {
										// .. and then get the PK dictionary for
										// the related object
										NSDictionary<String, Object> foreignKey = ((ERXGenericRecord) obj).primaryKeyDictionary(inTransaction);
										for (EOJoin join : (NSArray<EOJoin>) relationship.joins()) {
											// .. find the particular join that
											// is associated with this pk
											// attribute
											if (join.sourceAttribute() == primaryKeyAttribute) {
												// .. and steal its value
												value = foreignKey.objectForKey(join.destinationAttribute().name());
												if (value == null) {
													// for some reason the pk
													// dict is sometimes
													// array=>array instead of
													// String=>Object, but I
													// don't know when it is and
													// when it isn't, so
													// we go ahead and check for
													// both conditions.
													value = foreignKey.objectForKey(new NSArray(join.destinationAttribute().name()));
													if (value instanceof NSArray) {
														value = ((NSArray) value).lastObject();
													}
												}
											}
										}
									}
								}
							}

							if (value == null || value instanceof NSKeyValueCoding.Null) {
								incompletePK = true;
								value = NSKeyValueCoding.NullValue;
							}
							compositePrimaryKey.setObjectForKey(value, primaryKeyAttribute.name());
						}

						// .. if any of the attributes were null, throw an
						// exception, because you're
						// not going to be able to use that PK anyway -- it's
						// bogus
						if (incompletePK) {
							throw new IllegalArgumentException("You requested the primary key for the EO " + this + ", which has a composite primary key. At least one of the attributes of the primary key could not be determined, probably because one of the foreign key relationships was not set properly. The primary keys so far were " + compositePrimaryKey + ".");
						}
						_primaryKeyDictionary = compositePrimaryKey;
					}
					else {
						_primaryKeyDictionary = ERXEOControlUtilities.newPrimaryKeyDictionaryForObject(this);
					}
				}
			}
		}
		return _primaryKeyDictionary;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#committedSnapshotValueForKey(java.lang.String)
	 */
	public Object committedSnapshotValueForKey(String key) {
		NSDictionary snapshot = committedSnapshot();
		return snapshot != null ? snapshot.objectForKey(key) : null;
	}

	/**
	 * This method exists because
	 * {@link com.webobjects.eocontrol.EOEditingContext#committedSnapshotForObject EOEditingContext.committedSnapshotForObject()}
	 * gives unexpected results for newly inserted objects if
	 * {@link com.webobjects.eocontrol.EOEditingContext#processRecentChanges() EOEditingContext.processRecentChanges()}
	 * has been called. This method always returns a dictionary whose values are
	 * all NSKeyValueCoding.NullValue in the case of a newly inserted object.
	 * 
	 * @return the committed snapshot
	 */
	public NSDictionary committedSnapshot() {
		if (!isNewObject()) {
			return editingContext().committedSnapshotForObject(this);
		}
		else {
			NSArray keys = allPropertyKeys();
			NSMutableDictionary allNullDict = new NSMutableDictionary(keys.count());
			ERXDictionaryUtilities.setObjectForKeys(allNullDict, NSKeyValueCoding.NullValue, keys);
			return allNullDict;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#localInstanceOf(com.webobjects.eocontrol.EOEnterpriseObject)
	 */
	public EOEnterpriseObject localInstanceOf(EOEnterpriseObject eo) {
		return ERXEOControlUtilities.localInstanceOfObject(editingContext(), eo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#localInstanceIn(com.webobjects.eocontrol.EOEnterpriseObject)
	 */
	public EOEnterpriseObject localInstanceIn(EOEditingContext ec) {
		return ERXEOControlUtilities.localInstanceOfObject(ec, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#localInstancesOf(com.webobjects.foundation.NSArray)
	 */
	@SuppressWarnings("unchecked")
	public NSArray localInstancesOf(NSArray eos) {
		return ERXEOControlUtilities.localInstancesOfObjects(editingContext(), eos);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#changesFromCommittedSnapshot()
	 */
	public NSDictionary changesFromCommittedSnapshot() {
		return changesFromSnapshot(committedSnapshot());
	}
	
	/**
	 * Returns whether or not the given key has changed when compared to the committed snapshot.
	 * 
	 * @param key The key that you wish to check has changed from the committed snapshot
	 * @return true if it has changed
	 */
	public boolean hasKeyChangedFromCommittedSnapshot(String key) {
		NSDictionary d = changesFromCommittedSnapshot();
		return d.containsKey(key);
	}

	/**
	 * Returns whether or not the given key has changed from the given committed value.

	 * @param key The key that you wish to check has changed from the committed snapshot
	 * @param oldValue The value you wish to see if the key has changed from EG. Has 'status' changed from
	 *            STATUS.PENDING_STATUS
	 * @return true if the specified key value has changed from the specified value
	 */
	public boolean hasKeyChangedFromCommittedSnapshotFromValue(String key, Object oldValue) {
		NSDictionary d = changesFromCommittedSnapshot();
		return d.containsKey(key) && ERXExtensions.safeEquals(oldValue, committedSnapshotValueForKey(key));
	}

	/**
	 * Returns whether or not the given key has changed from the given previous value to the new value since the committed value.
	 * 
	 * @param key The key that you wish to check has changed from the committed snapshot
	 * @param oldValue The value you wish to see if the key has changed from
	 * @param newValue The value you wish to see if the key has changed to EG. Has 'status' changed from
	 *            STATUS.PENDING_STATUS to STATUS.CONFIRMED_STATUS
	 * @return true if the specified key value has changed from the specified value
	 */
	public boolean hasKeyChangedFromCommittedSnapshotFromValueToNewValue(String key, Object oldValue, Object newValue) {
		NSDictionary d = changesFromCommittedSnapshot();
		return d.containsKey(key) && ERXExtensions.safeEquals(newValue, d.objectForKey(key)) && ERXExtensions.safeEquals(oldValue, committedSnapshotValueForKey(key));
	}

	/**
	 * Returns whether or not the given key has changed to the new value since the committed value.
	 * 
	 * @param key The key that you wish to check has changed from the committed snapshot
	 * @param newValue The value you wish to see if the key has changed to EG. Has 'status' changed to
	 *            STATUS.CANCELLED_STATUS
	 * @return true if the specified key value has changed to the specified value
	 */
	public boolean hasKeyChangedFromCommittedSnapshotToValue(String key, Object newValue) {
		NSDictionary d = changesFromCommittedSnapshot();
		return d.containsKey(key) && ERXExtensions.safeEquals(newValue, d.objectForKey(key));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#parentObjectStoreIsObjectStoreCoordinator()
	 */
	public boolean parentObjectStoreIsObjectStoreCoordinator() {
		return editingContext().parentObjectStore() instanceof EOObjectStoreCoordinator;
	}

	/**
	 * Calls the method
	 * <code>refetchObjectFromDBinEditingContext(EOEditingContext ec)</code> and
	 * passes the object's Editing Context as Editing Context parameter.
	 * 
	 * @return the newly fetched object from the DB.
	 */
	public ERXEnterpriseObject refetchObjectFromDB() {
		return refetchObjectFromDBinEditingContext(editingContext());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#refetchObjectFromDBinEditingContext(com.webobjects.eocontrol.EOEditingContext)
	 */
	public ERXEnterpriseObject refetchObjectFromDBinEditingContext(EOEditingContext ec) {
		EOEntity entity = ERXEOAccessUtilities.entityNamed(ec, entityName());
		EOQualifier qual = entity.qualifierForPrimaryKey(primaryKeyDictionary(false));
		EOFetchSpecification fetchSpec = new EOFetchSpecification(entityName(), qual, null);
		fetchSpec.setRefreshesRefetchedObjects(true);
		NSArray results = ec.objectsWithFetchSpecification(fetchSpec);
		ERXEnterpriseObject freshObject = null;
		if (results.count() > 0) {
			freshObject = (ERXEnterpriseObject) results.objectAtIndex(0);
		}
		return freshObject;
	}
	
	private EOKeyGlobalID _permanentGlobalID;

	/**
	 * This method allows you to compute what the permanent EOGlobalID will be
	 * for an object before it has been saved to the database. It functions by
	 * calling into <code>primaryKeyDictionary()</code> to allocate the
	 * primary key if necessary. Then we build an EOKeyGlobalID from it. If the
	 * object already has a permanent global ID, we use that.
	 * 
	 * If you pass false for <code>generateIfMissing</code> and this object
	 * has a temporary global ID, null will be returned.
	 */
	public EOKeyGlobalID permanentGlobalID(boolean generateIfMissing) {
		if (_permanentGlobalID == null) {
			final EOEditingContext ec = editingContext();

			if (ec != null) {
				final EOGlobalID gid = ec.globalIDForObject(this);

				if (!gid.isTemporary()) {
					_permanentGlobalID = (EOKeyGlobalID) gid;
				}
				else if (generateIfMissing) {
					final NSDictionary<String, Object> primaryKeyDictionary = primaryKeyDictionary(false);
					final Object[] values;

					if (primaryKeyDictionary.count() == 1) {
						values = primaryKeyDictionary.allValues().objects();
					}
					else {
						final NSArray<String> sortedKeys = ERXDictionaryUtilities.stringKeysSortedAscending(primaryKeyDictionary);

						values = primaryKeyDictionary.objectsForKeys(sortedKeys, null).objects();
					}

					_permanentGlobalID = EOKeyGlobalID.globalIDWithEntityName(entityName(), values);
				}
			} else if(__globalID() != null && !__globalID().isTemporary()) {
				_permanentGlobalID = (EOKeyGlobalID) __globalID();
			}
		}

		return _permanentGlobalID;
	}

	/**
	 * Calls permanentGlobalID(boolean) passing true for generateIfMissing.
	 * 
	 * @see #permanentGlobalID(boolean)
	 */
	public EOKeyGlobalID permanentGlobalID() {
		return permanentGlobalID(true);
	}

	/**
	 * Overrides the EOGenericRecord's implementation to provide a slightly less
	 * verbose output. A typical output for an object mapped to the class
	 * com.foo.User with a primary key of 50 would look like: <com.foo.User
	 * pk:"50"> EOGenericRecord's implementation is preserved in the method
	 * <code>toLongString</code>. To restore the original verbose logging in
	 * your subclasses override this method and return toLongString.
	 * 
	 * @return much less verbose description of an enterprise object.
	 */
	@Override
	public String toString() {
		String pk = primaryKey();
		pk = (pk == null) ? "null" : pk;
		return "<" + getClass().getName() + " pk:\"" + pk + "\">";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#description()
	 */
	public String description() {
		return toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#toLongString()
	 */
	public String toLongString() {
		return super.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#trimSpaces()
	 */
	public void trimSpaces() {
		ERXEOControlUtilities.trimSpaces(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#isDeletedEO()
	 */
	public boolean isDeletedEO() {
		if (log.isDebugEnabled()) {
			log.debug("editingContext() = " + editingContext() + " this object: " + this);
		}
		// HACK AK: using private API here
		EOGlobalID gid = __globalID();
		boolean isDeleted = (editingContext() == null && (gid != null && !gid.isTemporary()));
		return isDeleted || (editingContext() != null && editingContext().deletedObjects().containsObject(this));
	}

	/**
	 * @deprecated use {@link ERXGenericRecord#isNewObject}
	 */
	@SuppressWarnings("dep-ann")
	public boolean isNewEO() {
		return isNewObject();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#isNewObject()
	 */

	public boolean isNewObject() {
		return ERXEOControlUtilities.isNewObject(this);
	}

	/**
	 * Returns true if this EO has been modified in this editing context. In EOF
	 * terms, this means that the EO's snapshot in this EC is not .equals the
	 * original database snapshot for the EO.
	 * 
	 * @return true if this EO's snapshot does not match the original snapshot
	 */
	public boolean isUpdatedObject() {
		NSDictionary snapshot = snapshot();
		NSDictionary originalSnapshot = __originalSnapshot();
		return originalSnapshot != null && !originalSnapshot.equals(snapshot);
	}

	/**
	 * Overrides the default validation mechanisms to provide a few checks
	 * before invoking super's implementation, which incidently just invokes
	 * validateValueForKey on the object's class description. The class
	 * description for this object should be an
	 * {@link ERXEntityClassDescription} or subclass. It is that class that
	 * provides the hooks to convert model throw validation exceptions into
	 * {@link ERXValidationException} objects.
	 * 
	 * @param value
	 *            to be validated for a given attribute or relationship
	 * @param key
	 *            corresponding to an attribute or relationship
	 * @throws NSValidation.ValidationException
	 *             if the value fails validation
	 * @return the validated value
	 */
	@Override
	public Object validateValueForKey(Object value, String key) throws NSValidation.ValidationException {
		if (validation.isDebugEnabled())
			validation.debug("ValidateValueForKey on eo: " + this + " value: " + value + " key: " + key);
		if (key == null) // better to raise before calling super which will
							// crash
			throw new RuntimeException("validateValueForKey called with null key on " + this);
		Object result = null;
		try {
			result = super.validateValueForKey(value, key);
			EOClassDescription cd = classDescription();
			if (cd instanceof ERXEntityClassDescription) {
				((ERXEntityClassDescription) cd).validateObjectWithUserInfo(this, value, "validateForKey." + key, key);
			}
			value = _validateValueForKey(value, key);
		}
		catch (ERXValidationException e) {
			throw e;
		}
		catch (NSValidation.ValidationException e) {
			if (e.key() == null || e.object() == null || (e.object() != null && !(e.object() instanceof EOEnterpriseObject)))
				e = new NSValidation.ValidationException(e.getMessage(), this, key);
			if (validationException.isDebugEnabled()) {
				validationException.debug("Exception: " + e.getMessage() + " raised while validating object: " + this + " class: " + getClass() + " pKey: " + primaryKey(), e);
			}
			throw e;
		}
		catch (RuntimeException e) {
			log.error("**** During validateValueForKey " + key, e);
			throw e;
		}
		return result;
	}

	protected Object _validateValueForKey(Object value, String key) throws NSValidation.ValidationException {
		return value;
	}

	/**
	 * This method performs a few checks before invoking super's implementation.
	 * If the property key: <b>ERDebuggingEnabled</b> is set to true then the
	 * method <code>checkConsistency</code> will be called on this object.
	 * 
	 * @throws NSValidation.ValidationException
	 *             if the object does not pass validation for saving to the
	 *             database.
	 */
	@Override
	public void validateForSave() throws NSValidation.ValidationException {
		// This condition shouldn't ever happen, but it does ;)
		// CHECKME: This was a 4.5 issue, not sure if this one has been fixed
		// yet.
		if (editingContext() != null && editingContext().deletedObjects().containsObject(this)) {
			validation.warn("Calling validate for save on an eo: " + this + " that has been marked for deletion!");
		}
		super.validateForSave();
		// FIXME: Should move all of the keys into on central place for easier
		// management.
		// Also might want to have a flag off of ERXApplication is debugging is
		// enabled.
		// FIXME: Should have a better flag than just ERDebuggingEnabled
		if (ERXProperties.booleanForKey("ERDebuggingEnabled"))
			checkConsistency();
	}

	/**
	 * Calls up validateForInsert() on the class description if it supports it.
	 * 
	 * @throws NSValidation.ValidationException
	 *             if the object does not pass validation for saving to the
	 *             database.
	 */
	@Override
	public void validateForInsert() throws NSValidation.ValidationException {
		EOClassDescription cd = classDescription();
		if (cd instanceof ERXEntityClassDescription) {
			((ERXEntityClassDescription) cd).validateObjectForInsert(this);
		}
		super.validateForInsert();
	}

	/**
	 * Calls up validateForUpdate() on the class description if it supports it.
	 * 
	 * @throws NSValidation.ValidationException
	 *             if the object does not pass validation for saving to the
	 *             database.
	 */
	@Override
	public void validateForUpdate() throws NSValidation.ValidationException {
		EOClassDescription cd = classDescription();
		if (cd instanceof ERXEntityClassDescription) {
			((ERXEntityClassDescription) cd).validateObjectForUpdate(this);
		}
		super.validateForUpdate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#checkConsistency()
	 */
	// CHECKME: This method was very useful at NS, might not be as useful here.
	public void checkConsistency() throws NSValidation.ValidationException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see er.extensions.ERXEnterpriseObject#batchCheckConsistency()
	 */
	public void batchCheckConsistency() throws NSValidation.ValidationException {
	}

	/**
	 * Overridden to support two-way relationship setting.
	 */
	@Override
	public void includeObjectIntoPropertyWithKey(Object o, String key) {
		if (_updateInverseRelationships()) {
			InverseRelationshipUpdater.includeObjectIntoPropertyWithKey(this, o, key);
		}
		super.includeObjectIntoPropertyWithKey(o, key);
	}

	/**
	 * Overridden to support two-way relationship setting.
	 */
	@Override
	public void excludeObjectFromPropertyWithKey(Object o, String key) {
		if (_updateInverseRelationships()) {
			InverseRelationshipUpdater.excludeObjectFromPropertyWithKey(this, o, key);
		}
		super.excludeObjectFromPropertyWithKey(o, key);
	}
	
	/**
	 * Overridden to support two-way relationship setting.
	 */
	@Override
	public void takeValueForKey(Object value, String key) {
		super.takeValueForKey(value, key);
	}
	
	@Override
	public void takeStoredValueForKey(Object value, String key) {
		if (_updateInverseRelationships()) {
			InverseRelationshipUpdater.takeStoredValueForKey(this, value, key);
		}
		super.takeStoredValueForKey(value, key);
	}
	
	/**
	 * This method explicitly turns off inverse relationship updating, because
	 * it's only called during undo and revert inside of EOF.  If you are 
	 * calling this method, it's presumed that you understand the consequences
	 * of your actions :)
	 */
	@Override
	public void updateFromSnapshot(NSDictionary snapshot) {
		boolean old = _setUpdateInverseRelationships(false);
		try {
			super.updateFromSnapshot(snapshot);
		}
		finally {
			_setUpdateInverseRelationships(old);
		}
	}

	// Debugging aids -- turn off during production
	// These methods are used to catch the classic mistake of:
	// public String foo() { return (String)valueForKey("foo"); }
	// where foo is not a property key

	/*
	 * public Object storedValueForKey(String key) { // FIXME: turn this off
	 * during production if (!allPropertyKeys().containsObject(key)) throw new
	 * RuntimeException("********* Tried to access storedValueForKey on
	 * "+entityName()+" on a non class property: "+key); Object value =
	 * super.storedValueForKey(key); return value; }
	 * 
	 * public void takeStoredValueForKey(Object value, String key) { // FIXME:
	 * turn this off during production if
	 * (!allPropertyKeys().containsObject(key)) { throw new
	 * RuntimeException("********* Tried to takeStoredValueForKey on
	 * "+entityName()+" on a non class property: "+key); }
	 * super.takeStoredValueForKey(value,key); }
	 * 
	 */
	
	/**
	 * Checks that the editing contexts in source and destination matches and throws an exception if they do not.
	 * 
	 * @param source the source object
	 * @param relationshipName the name of the relationship that is being updated
	 * @param destination the destination object
	 * @throws RuntimeException if the editing contexts do not match
	 */
	public static void checkMatchingEditingContexts(EOEnterpriseObject source, String relationshipName, EOEnterpriseObject destination) {
		if (destination != null) {
			EOEditingContext sourceEditingContext = source.editingContext();
			EOEditingContext destinationEditingContext = destination.editingContext();
			if (destinationEditingContext != sourceEditingContext && !(sourceEditingContext instanceof EOSharedEditingContext) && !(destinationEditingContext instanceof EOSharedEditingContext)) {
				if (destinationEditingContext == null || sourceEditingContext == null) {
					if (sourceEditingContext == null) {
						if (!(destination instanceof ERXGenericRecord && ERXGenericRecord.InverseRelationshipUpdater.updateInverseRelationships() && !((ERXGenericRecord)destination)._updateInverseRelationships)) {
							throw new RuntimeException("You crossed editing context boundaries attempting to set the '" + relationshipName + "' relationship of " + source + " (which is not in an editing context) to " + destination + " (in EC " + destinationEditingContext + ").");
						}
					}
					else {
						// MS: Why is this not considered an error?
						//throw new RuntimeException("You crossed editing context boundaries attempting to set the '" + relationshipName + "' relationship of " + source + " (in EC " + sourceEditingContext + ") to " + destination + " (which is not an in editing context).");
					}
				}
				else {
					throw new RuntimeException("You crossed editing context boundaries attempting to set the '" + relationshipName + "' relationship of " + source + " (in EC " + sourceEditingContext + ") to " + destination + " (in EC " + destinationEditingContext + ").");
				}
			}
		}
	}
	
	/**
	 * Provides automatic inverse relationship updating for ERXGenericRecord and ERXCustomObject.
	 * 
	 * @property er.extensions.ERXEnterpriseObject.updateInverseRelationships if true, inverse relationships are automatically updated
	 * 
	 * @author mschrag
	 */
	public static class InverseRelationshipUpdater {
	    private static boolean updateInverseRelationships = ERXProperties.booleanForKey("er.extensions.ERXEnterpriseObject.updateInverseRelationships");

	    /**
	     * Toggles the global setting for updating inverse relationships.
	     * 
	     * @param updateInverseRelationships if true, inverse relationships are automatically updated
	     */
	    public static void setUpdateInverseRelationships(boolean updateInverseRelationships) {
	    	ERXGenericRecord.InverseRelationshipUpdater.updateInverseRelationships = updateInverseRelationships;
		}
	    
	    /**
	     * Returns whether or not inverse relationships are automatically updated.
	     * 
	     * @return if true, inverse relationships are automatically updated
	     */
	    public static boolean updateInverseRelationships() {
			return ERXGenericRecord.InverseRelationshipUpdater.updateInverseRelationships;
		}
	    
	    /**
	     * Called from eo.includeObjectIntoPropertyWithKey.
	     * 
	     * @param object the object being updated
	     * @param value the value to include in the relationship
	     * @param key the name of the relationship to update
	     */
		public static void includeObjectIntoPropertyWithKey(ERXEnterpriseObject object, Object value, String key) {
			if (value != null) {
				String inverse = object.classDescription().inverseForRelationshipKey(key);
				if (inverse != null) {
					ERXEnterpriseObject eo = (ERXEnterpriseObject) value;
					if (!eo.isToManyKey(inverse)) {
						EOEnterpriseObject inverseValue = (EOEnterpriseObject) eo.valueForKey(inverse);
						if (inverseValue != object) {
							boolean oldUpdateInverseRelationship = eo._setUpdateInverseRelationships(false);
							try {
								eo.takeValueForKey(object, inverse);
							}
							finally {
								eo._setUpdateInverseRelationships(oldUpdateInverseRelationship);
							}
						}
					}
					else {
						NSArray values = (NSArray) eo.valueForKey(inverse);
						if (values != null && !values.containsObject(object)) {
							boolean oldUpdateInverseRelationship = eo._setUpdateInverseRelationships(false);
							try {
								eo.addObjectToPropertyWithKey(object, inverse);
							}
							finally {
								eo._setUpdateInverseRelationships(oldUpdateInverseRelationship);
							}
						}
					}
				}
			}
		}
		
	    /**
	     * Called from eo.excludeObjectFromPropertyWithKey.
	     * 
	     * @param object the object being updated
	     * @param value the value to remove from the relationship
	     * @param key the name of the relationship to update
	     */
		public static void excludeObjectFromPropertyWithKey(ERXEnterpriseObject object, Object value, String key) {
			if (value != null) {
				String inverse = object.classDescription().inverseForRelationshipKey(key);
				if (inverse != null) {
					ERXEnterpriseObject eo = (ERXEnterpriseObject) value;
					if (!eo.isToManyKey(inverse)) {
						if (eo.valueForKey(inverse) != null) {
							boolean oldUpdateInverseRelationship = eo._setUpdateInverseRelationships(false);
							try {
								eo.takeValueForKey(null, inverse);
							}
							finally {
								eo._setUpdateInverseRelationships(oldUpdateInverseRelationship);
							}
						}
					}
					else {
						NSArray values = (NSArray) eo.valueForKey(inverse);
						if (values.containsObject(object)) {
							boolean oldUpdateInverseRelationship = eo._setUpdateInverseRelationships(false);
							try {
								eo.removeObjectFromPropertyWithKey(object, inverse);
							}
							finally {
								eo._setUpdateInverseRelationships(oldUpdateInverseRelationship);
							}
						}
					}
				}
			}
		}
	
	    /**
	     * Called from eo.takeValueForKey.
	     * 
	     * @param object the object being updated
	     * @param value the value to set on the key
	     * @param key the name of the key to update
	     */
		public static void takeStoredValueForKey(ERXEnterpriseObject object, Object value, String key) {
			// we only handle toOne keys here, but there is no API for that so
			// this unreadable monster first checks the fastest thing, the the
			// slower conditions
			if (value instanceof EOEnterpriseObject || (value == null && !object.isToManyKey(key) && object.classDescriptionForDestinationKey(key) != null)) {
				String inverse = object.classDescription().inverseForRelationshipKey(key);
				// Is there any inverse relationship?
				if (inverse != null) {
					ERXEnterpriseObject oldValueEO = (ERXEnterpriseObject) object.valueForKey(key);

					// If the object isn't null, we need to perform the equivalent of
					// an addObjectToBothSidesOfRelationshipWithKey
					if (value != null) {
						// If we're replacing a previous value (that isn't the same instance), 
						// then we want to remove this object from the CURRENT inverse
						// relationship
						if (oldValueEO != null && oldValueEO != object) {
							oldValueEO.removeObjectFromPropertyWithKey(object, inverse);
						}
						
						ERXEnterpriseObject newValueEO = (ERXEnterpriseObject) value;
						
						ERXGenericRecord.checkMatchingEditingContexts(object, key, newValueEO);

						//object._superTakeValueForKey(value, key);
						if (newValueEO.isToManyKey(inverse)) {
							NSArray inverseOldValues = (NSArray) newValueEO.valueForKey(inverse);
							if (inverseOldValues == null || !inverseOldValues.containsObject(object)) {
								boolean oldUpdateInverseRelationship = newValueEO._setUpdateInverseRelationships(false);
								try {
									newValueEO.addObjectToPropertyWithKey(object, inverse);
								}
								finally {
									newValueEO._setUpdateInverseRelationships(oldUpdateInverseRelationship);
								}
							}
						}
						else {
							EOEnterpriseObject inverseOldValue = (EOEnterpriseObject) newValueEO.valueForKey(inverse);
							if (inverseOldValue != object) {
								boolean oldUpdateInverseRelationship = newValueEO._setUpdateInverseRelationships(false);
								try {
									newValueEO.takeStoredValueForKey(object, inverse);
								}
								finally {
									newValueEO._setUpdateInverseRelationships(oldUpdateInverseRelationship);
								}
							}
						}
					}
					// If the object is a null, we need to perform the equivalent of
					// an removeObjectFromBothSidesOfRelationshipWithKey
					else {
						//object._superTakeValueForKey(null, key);
						if (oldValueEO != null) {
							if (oldValueEO.isToManyKey(inverse)) {
								NSArray inverseOldValues = (NSArray) oldValueEO.valueForKey(inverse);
								if (inverseOldValues != null && inverseOldValues.containsObject(object)) {
									boolean oldUpdateInverseRelationship = oldValueEO._setUpdateInverseRelationships(false);
									try {
										oldValueEO.removeObjectFromPropertyWithKey(object, inverse);
									}
									finally {
										oldValueEO._setUpdateInverseRelationships(oldUpdateInverseRelationship);
									}
								}
							}
							else {
								if (oldValueEO == object) {
									boolean oldUpdateInverseRelationship = oldValueEO._setUpdateInverseRelationships(false);
									try {
										oldValueEO.takeStoredValueForKey(null, inverse);
									}
									finally {
										oldValueEO._setUpdateInverseRelationships(oldUpdateInverseRelationship);
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
