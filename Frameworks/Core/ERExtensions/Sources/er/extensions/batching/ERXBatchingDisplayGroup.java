package er.extensions.batching;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WODisplayGroup;
import com.webobjects.eoaccess.EODatabaseContext;
import com.webobjects.eoaccess.EODatabaseDataSource;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOModel;
import com.webobjects.eoaccess.EOModelGroup;
import com.webobjects.eoaccess.EOSQLExpression;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EODataSource;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOKeyValueUnarchiver;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSNotificationCenter;
import com.webobjects.foundation._NSDelegate;

import er.extensions.appserver.ERXDisplayGroup;
import er.extensions.eof.ERXEOAccessUtilities;
import er.extensions.eof.ERXEOControlUtilities;
import er.extensions.eof.ERXEOGlobalIDUtilities;
import er.extensions.eof.ERXRecursiveBatchFetching;
import er.extensions.eof.ERXS;
import er.extensions.foundation.ERXArrayUtilities;
import er.extensions.jdbc.ERXSQLHelper;

/**
 * Extends {@link WODisplayGroup} in order to provide real batching. This is
 * done by adding database specific code to the select statement from the
 * {@link EOFetchSpecification} from the {@link WODisplayGroup}'s
 * {@link EODataSource} which <b>must</b> be an {@link EODatabaseDataSource}.
 * If used with other datasources, it reverts to the default behaviour.
 * 
 * @author dt first version
 * @author ak gross hacks, made functional and usable.
 */
public class ERXBatchingDisplayGroup<T> extends ERXDisplayGroup<T> {

	/** Logging support */
	private static final Logger log = Logger.getLogger(ERXBatchingDisplayGroup.class);

	/** total number of batches */
	protected int _batchCount;

	/** cache for the displayed objects */
	protected NSArray<T> _displayedObjects;

	/** cache batching flag */
	protected Boolean _isBatching;
	
	protected NSArray<String> _prefetchingRelationshipKeyPaths;
	
	protected int _rowCount = -1;
	
	/**
	 * Creates a new ERXBatchingDisplayGroup.
	 */
	public ERXBatchingDisplayGroup() {
	}
	
	/**
	 * Decodes an ERXBatchingDisplayGroup from the given unarchiver.
	 * 
	 * @param unarchiver the unarchiver to construct this display group with
	 * @return the corresponding batching display group
	 */
	public static Object decodeWithKeyValueUnarchiver(EOKeyValueUnarchiver unarchiver) {
		return new ERXBatchingDisplayGroup<Object>(unarchiver);
	}

	/**
	 * Creates a new ERXBatchingDisplayGroup from an unarchiver.
	 * 
	 * @param unarchiver the unarchiver to construct this display group with
	 */
	@SuppressWarnings("unchecked")
	private ERXBatchingDisplayGroup(EOKeyValueUnarchiver unarchiver) {
		this();
		setCurrentBatchIndex(1);
		setNumberOfObjectsPerBatch(unarchiver.decodeIntForKey("numberOfObjectsPerBatch"));
		setFetchesOnLoad(unarchiver.decodeBoolForKey("fetchesOnLoad"));
		setValidatesChangesImmediately(unarchiver.decodeBoolForKey("validatesChangesImmediately"));
		setSelectsFirstObjectAfterFetch(unarchiver.decodeBoolForKey("selectsFirstObjectAfterFetch"));
		setLocalKeys((NSArray) unarchiver.decodeObjectForKey("localKeys"));
		setDataSource((EODataSource) unarchiver.decodeObjectForKey("dataSource"));
		setSortOrderings((NSArray) unarchiver.decodeObjectForKey("sortOrdering"));
		setQualifier((EOQualifier) unarchiver.decodeObjectForKey("qualifier"));
		setDefaultStringMatchFormat((String) unarchiver.decodeObjectForKey("formatForLikeQualifier"));
		NSDictionary insertedObjectDefaultValues = (NSDictionary) unarchiver.decodeObjectForKey("insertedObjectDefaultValues");
		if (insertedObjectDefaultValues == null) {
			insertedObjectDefaultValues = NSDictionary.EmptyDictionary;
		}
		setInsertedObjectDefaultValues(insertedObjectDefaultValues);
		finishInitialization();
	}
	
	/**
	 * If we're batching and the displayed objects have not been fetched,
	 * do a refetch() of them.
	 */
	protected void refetchIfNecessary() {
		if (isBatching() && _displayedObjects == null) {
			refetch();
		}
	}
	
	/**
	 * Determines if batching is possible.
	 * 
	 * @return true if dataSource is an instance of EODatabaseDataSource
	 */
	protected boolean isBatching() {
		return _isBatching == null ? false : _isBatching.booleanValue();
	}

	/**
	 * Overridden to set the isBatching flag to true if we have an
	 * EODatabaseDataSource.
	 */
	@Override
	public void setDataSource(EODataSource eodatasource) {
		_isBatching = (eodatasource instanceof EODatabaseDataSource) ? Boolean.TRUE : Boolean.FALSE;
		super.setDataSource(eodatasource);
	}
	
	/**
	 * Override the number of rows of results (if you can
	 * provide a better estimate than the default behavior).  If you
	 * guess too low, you will never get more than what you set, but
	 * if you guess too high, it will adjust.
	 *  
	 * @param rowCount the number of rows of results 
	 */
	public void setRowCount(int rowCount) {
		_rowCount = rowCount;
	}
	
	/**
	 * Overridden to return the pre-calculated number of batches
	 */
	@Override
	public int batchCount() {
		if (isBatching()) {
			if (_displayedObjects == null) {
				refetch();
			}
			return _batchCount;
		}
		return super.batchCount();
	}

	/**
	 * Overriden to clear out our array of fetched objects.
	 */
	@Override
	public void setCurrentBatchIndex(int index) {
		int previousBatchIndex = currentBatchIndex();
		super.setCurrentBatchIndex(index);
		if (isBatching() && previousBatchIndex != index) {
			_displayedObjects = null;
		}
	}

	/**
	 * Overriden to clear out our array of fetched objects.
	 */
	@Override
	public void setNumberOfObjectsPerBatch(int count) {
		boolean didFetch = _displayedObjects != null;
		if (isBatching() && numberOfObjectsPerBatch() != count) {
			_displayedObjects = null;
		}
		NSArray<T> selectedObjects = selectedObjects();
		super.setNumberOfObjectsPerBatch(count);
		setSelectedObjects(selectedObjects);
		// we have already fetched, so we need to adapt the batch count
		if (didFetch) {
			updateBatchCount();
		}
	}

	/**
	 * Overridden method in order to fetch -only- the rows that are needed. This
	 * is different to the editors methods because a {@link WODisplayGroup}
	 * would always fetch from the start until the end of the objects from the
	 * fetch limit.
	 * 
	 * @return the objects that should be diplayed.
	 */
	@Override
	public NSArray<T> displayedObjects() {
		if (isBatching()) {
			refetchIfNecessary();
			return _displayedObjects;
		}
		return super.displayedObjects();
	}

	/**
	 * Overridden to return allObjects() when batching, as we can't qualify in memory.
	 */
	@Override
	public NSArray<T> filteredObjects() {
		if (isBatching()) {
			return allObjects();
		}
		return super.filteredObjects();
	}

	/**
	 * Overridden to trigger a refetch.
	 */
	@Override
	public void setQualifier(EOQualifier aEoqualifier) {
		super.setQualifier(aEoqualifier);
		_displayedObjects = null;
	}

	/**
	 * Overridden to preserve the selected objects.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void setSortOrderings(NSArray nsarray) {
		NSArray<T> selectedObjects = selectedObjects();
		super.setSortOrderings(nsarray);
		setSelectedObjects(selectedObjects);
		if (isBatching()) {
			_displayedObjects = null;
		}
	}
	
	/**
	 * Sets the prefetching key paths for the underlying fetch spec.
	 * 
	 * @param prefetchingRelationshipKeyPaths the prefetching key paths for the underlying fetch spec
	 */
	public void setPrefetchingRelationshipKeyPaths(NSArray<String> prefetchingRelationshipKeyPaths) {
		_prefetchingRelationshipKeyPaths = prefetchingRelationshipKeyPaths;
	}
	
	/**
	 * Returns the prefetching key paths for the underlying fetch spec.
	 * @return the prefetching key paths for the underlying fetch spec
	 */
	public NSArray<String> prefetchingRelationshipKeyPaths() {
		return _prefetchingRelationshipKeyPaths;
	}

	/**
	 * Utility to get the fetch spec from the datasource and the filter
	 * qualifier.
	 * 
	 */
	protected EOFetchSpecification fetchSpecification() {
		EODatabaseDataSource ds = (EODatabaseDataSource) dataSource();
		EOFetchSpecification spec = (EOFetchSpecification) ds.fetchSpecificationForFetch().clone();
		spec.setSortOrderings(ERXArrayUtilities.arrayByAddingObjectsFromArrayWithoutDuplicates(sortOrderings(), spec.sortOrderings()));
		EOQualifier dgQualifier = qualifier();
		EOQualifier qualifier = spec.qualifier();
		if (dgQualifier != null) {
			if (qualifier != null) {
				qualifier = new EOAndQualifier(new NSArray<EOQualifier>(new EOQualifier[] { dgQualifier, qualifier }));
			}
			else {
				qualifier = dgQualifier;
			}
			spec.setQualifier(qualifier);
		}
		return spec;
	}

	/**
	 * Utility to get at the number of rows when batching.
	 */
	protected int rowCount() {
		EOEditingContext ec = dataSource().editingContext();
		EOFetchSpecification spec = fetchSpecification();

		int rowCount = _rowCount;
		if (rowCount == -1) {
			rowCount = ERXEOAccessUtilities.rowCountForFetchSpecification(ec, spec);
		}
		return rowCount;
	}

	/**
	 * Utility to fetch the object in a given range.
	 * @param start
	 * @param end
	 */
	protected NSArray<T> objectsInRange(int start, int end) {
		EOEditingContext ec = dataSource().editingContext();
		EOFetchSpecification spec = fetchSpecification();
		NSArray result = null;
		if (spec.hints() == null || spec.hints().isEmpty() || spec.hints().valueForKey(EODatabaseContext.CustomQueryExpressionHintKey) == null) {
			// no hints on the fs
			
			// This used to turn the EO's into faults and then fetch the faults. The problem
			// with that is that for abstract entities, this would fetch the objects one-by-one
			// to determine what the correct EOGlobalID is for each subentity. Instead, I've
			// changed this to created a global ID for each row (which supports being a guess)
			// and then fetch the objects with the global IDs.
			NSArray primKeys = ERXEOControlUtilities.primaryKeyValuesInRange(ec, spec, start, end);
			EOEntity entity = ERXEOAccessUtilities.entityNamed(ec, spec.entityName());
			NSMutableArray<EOGlobalID> gids = new NSMutableArray<EOGlobalID>();
			for (Object obj : primKeys) {
				NSDictionary pkDict = (NSDictionary) obj;
				EOGlobalID gid = entity.globalIDForRow(pkDict);
				gids.addObject(gid);
			}
			NSMutableArray objects = ERXEOGlobalIDUtilities.fetchObjectsWithGlobalIDs(ec, gids);

			NSArray prefetchingRelationshipKeyPaths = _prefetchingRelationshipKeyPaths;
			if (prefetchingRelationshipKeyPaths == null || prefetchingRelationshipKeyPaths.count() == 0) {
				prefetchingRelationshipKeyPaths = spec.prefetchingRelationshipKeyPaths();
			}
			if (prefetchingRelationshipKeyPaths != null && prefetchingRelationshipKeyPaths.count() > 0) {
				ERXRecursiveBatchFetching.batchFetch(objects, prefetchingRelationshipKeyPaths, true);
			}

			ERXS.sort(objects, spec.sortOrderings());
			result = objects.immutableClone();
		}
		else {
			// we have hints, use them
			
			EOModel model = EOModelGroup.defaultGroup().entityNamed(spec.entityName()).model();
			ERXSQLHelper sqlHelper = ERXSQLHelper.newSQLHelper(ec, model.name());
			Object hint = spec.hints().valueForKey(EODatabaseContext.CustomQueryExpressionHintKey);
			String sql = sqlHelper.customQueryExpressionHintAsString(hint);
			sql = sqlHelper.limitExpressionForSQL(null, spec, sql, start, end);
			result = EOUtilities.rawRowsForSQL(ec, model.name(), sql, null);
		}
		// fetch the primary keys, turn them into faults, then batch-fetch all
		// the non-resident objects
		//NSArray primKeys = ERXEOControlUtilities.primaryKeyValuesInRange(ec, spec, start, end);
		//NSArray faults = ERXEOControlUtilities.faultsForRawRowsFromEntity(ec, primKeys, spec.entityName());
		//NSArray objects = ERXEOControlUtilities.objectsForFaultWithSortOrderings(ec, faults, sortOrderings());
		return result;
	}

	/**
	 * Utility that does the actual fetching, if a qualifier() is set, it adds
	 * it to the dataSource() fetch qualifier.
	 */
	protected void refetch() {
		int rowCount = rowCount();

		int start = (currentBatchIndex() - 1) * numberOfObjectsPerBatch();
		int end = start + numberOfObjectsPerBatch();

		if (numberOfObjectsPerBatch() == 0) {
			start = 0;
			end = rowCount;
		}
		
		if (start > rowCount) {
			start = rowCount;
		}

		if (end > rowCount) {
			end = rowCount;
		}

		if(filteredObjects().count() != rowCount) {
			NSArray<T> selectedObjects = selectedObjects();
			setObjectArray(new FakeArray(rowCount));
			setSelectedObjects(selectedObjects);
		}
		
		_displayedObjects = objectsInRange(start, end);
		
		// MS: Adjust our guess of the row count if it was
		// too high.
		if (_rowCount != -1) {
			int displayedObjectsCount = _displayedObjects.count();
			if (displayedObjectsCount < numberOfObjectsPerBatch()) {
				_rowCount = start + _displayedObjects.count();
			}
		}
	}
	
	protected void updateBatchCount() {
		if (numberOfObjectsPerBatch() == 0) {
			_batchCount = 0;
		}
		else if (allObjects().count() == 0) {
			_batchCount = 1;
		}
		else {
			_batchCount = (allObjects().count() - 1) / numberOfObjectsPerBatch() + 1;
		}
	}

	/**
	 * Overridden to update the batch count.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void setObjectArray(NSArray objects) {
		super.setObjectArray(objects);
		updateBatchCount();
	}

	/**
	 * Overridden to fetch only within displayed limits.
	 */
	@Override
	public Object fetch() {
		if (isBatching()) {
			_NSDelegate delegate = null;
			if (this.delegate() != null) {
				delegate = new _NSDelegate(WODisplayGroup.Delegate.class, delegate());
				if(delegate.respondsTo("displayGroupShouldFetch") && !delegate.booleanPerform("displayGroupShouldFetch", this)) {
		            return null;
				}
			}
			
			if (undoManager() != null) {
				undoManager().removeAllActionsWithTarget(this);
			}
			NSNotificationCenter.defaultCenter().postNotification("WODisplayGroupWillFetch", this);
			refetch();
			if (delegate != null) {
				// was initialized above
				if (delegate.respondsTo("displayGroupDidFetchObjects")) {
					delegate.perform("displayGroupDidFetchObjects", this, _displayedObjects);
				}
			}
			return null;
		}
		return super.fetch();
	}

	@Override
	public void updateDisplayedObjects() {
		if (isBatching()) {
			// refetch();
			NSMutableArray<T> selectedObjects = (NSMutableArray<T>) selectedObjects();
			NSArray<T> obj = allObjects();
			if (delegate() != null) {
				_NSDelegate delegate = new _NSDelegate(WODisplayGroup.Delegate.class, delegate());
				if (delegate != null && delegate.respondsTo("displayGroupDisplayArrayForObjects")) {
					delegate.perform("displayGroupDisplayArrayForObjects", this, obj);
				}
			}
			// _displayedObjects = new NSMutableArray(obj);
			setSelectedObjects(selectedObjects);
			// selectObjectsIdenticalToSelectFirstOnNoMatch(selectedObjects,
			// false);
			redisplay();
		}
		else {
			super.updateDisplayedObjects();
		}
	}

	/**
	 * Selects the visible objects, overridden to fetch all objects. Note that
	 * this makes sense only when there are only a "few" objects in the list.
	 * 
	 */

	@Override
	public Object selectFilteredObjects() {
		if (isBatching()) {
			setSelectedObjects(objectsInRange(0, rowCount()));
			return null;
		}
		return super.selectFilteredObjects();
	}

	/**
	 * Dummy array class that is used to provide a certain number of entries. We
	 * just fake that we an array with the number of objects the display group
	 * should display.
	 */
	protected class FakeArray extends NSMutableArray<Object> {
		public FakeArray(int count) {
			super(count);
			Object fakeObject = new NSKeyValueCoding.ErrorHandling() {
				public Object handleQueryWithUnboundKey(String anS) {
					return null;
				}

				public void handleTakeValueForUnboundKey(Object anObj, String anS) {
				}

				public void unableToSetNullForKey(String anS) {
				}
			};
			for (int i = 0; i < count; i++) {
				// GROSS HACK: (ak) WO wants to sort the given array via KVC so
				// we just
				// let it sort "nothing" objects
				insertObjectAtIndex(fakeObject, i);
			}
		}
	}
}
