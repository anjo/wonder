package er.extensions.appserver;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WODisplayGroup;
import com.webobjects.eoaccess.EODatabaseDataSource;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EODataSource;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueUnarchiver;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

import er.extensions.ERXExtensions;
import er.extensions.eof.ERXEOAccessUtilities;

/**
 * Extends {@link WODisplayGroup}
 * <ul>
 * <li>provide access to the filtered objects</li>
 * <li>allows you to add qualifiers to the final query qualifier (as opposed to just min/equals/max with the keys)</li>
 * <li>clears out the sort ordering when the datasource changes. This is a cure fix to prevent errors when using switch components.
 * </ul>
 * @author ak
 */
public class ERXDisplayGroup<T> extends WODisplayGroup {

	/** Logging support */
	private static final Logger log = Logger.getLogger(ERXDisplayGroup.class);

	public ERXDisplayGroup() {
		super();
	}
	
	/**
	 * Decodes an ERXDisplayGroup from the given unarchiver.
	 * 
	 * @param unarchiver the unarchiver to construct this display group with
	 * @return the corresponding batching display group
	 */
	public static Object decodeWithKeyValueUnarchiver(EOKeyValueUnarchiver unarchiver) {
		return new ERXDisplayGroup<Object>(unarchiver);
	}

	/**
	 * Creates a new ERXBatchingDisplayGroup from an unarchiver.
	 * 
	 * @param unarchiver the unarchiver to construct this display group with
	 */
	@SuppressWarnings("unchecked")
	private ERXDisplayGroup(EOKeyValueUnarchiver unarchiver) {
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
	 * Holds the extra qualifiers.
	 */
	private NSMutableDictionary<String, EOQualifier> _extraQualifiers = new NSMutableDictionary<String, EOQualifier>();

	public void setQualifierForKey(EOQualifier qualifier, String key) {
		if(qualifier != null) {
			_extraQualifiers.setObjectForKey(qualifier, key);
		} else {
			_extraQualifiers.removeObjectForKey(key);
		}
	}

	/**
	 * Overridden to support extra qualifiers.
	 */
	@Override
	public EOQualifier qualifierFromQueryValues() {
		EOQualifier q1 = super.qualifierFromQueryValues();
		EOQualifier q2 = null;
		if(_extraQualifiers.allValues().count() > 1) {
			q2 = new EOAndQualifier(_extraQualifiers.allValues());
		} else if(_extraQualifiers.allValues().count() > 0) {
			q2 = _extraQualifiers.allValues().lastObject();
		}
		return q1 == null ? q2 : (q2 == null ? q1 : new EOAndQualifier(new NSArray<EOQualifier>(new EOQualifier[] {q1, q2})));
	}

	/**
	 * Overridden to localize the fetch specification if needed.
	 */
	@Override
	public Object fetch() {
		if(log.isDebugEnabled()) {
			log.debug("Fetching: " + toString(), new RuntimeException("Dummy for Stacktrace"));
		}
		Object result;
		// ak: we need to transform localized keys (foo.name->foo.name_de)
		// when we do a real fetch. This actually
		// belongs into ERXEC, but I'm reluctant to have this morphing done
		// every time a fetch occurs as it affects mainly sort ordering
		// from the display group
		if (dataSource() instanceof EODatabaseDataSource) {
			EODatabaseDataSource ds = (EODatabaseDataSource) dataSource();
			EOFetchSpecification old = ds.fetchSpecification();
			EOFetchSpecification fs = ERXEOAccessUtilities.localizeFetchSpecification(ds.editingContext(), old);
			ds.setFetchSpecification(fs);
			try {
				result = super.fetch();
			} finally {
				ds.setFetchSpecification(old);
			}
		} else {
			result = super.fetch();
		}
		return result;
	}

	/**
	 * Returns all objects, filtered by the qualifier().
	 */
	@SuppressWarnings("unchecked")
	public NSArray<T> filteredObjects() {
		// FIXME AK: need to cache here
		NSArray<T> result;
		EOQualifier q=qualifier();
		if (q!=null) {
			result=EOQualifier.filteredArrayWithQualifier(allObjects(),q);
		} else {
			result=allObjects();
		}
		return result;
	}

	/**
	 * Overridden to track selection changes.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public NSArray<T> selectedObjects() {
		if(log.isDebugEnabled()) {
			log.debug("selectedObjects@" + hashCode() +  ":" + super.selectedObjects().count());
		}
		return super.selectedObjects();
	}

	/**
	 * Overridden to track selection changes.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void setSelectedObjects(NSArray nsarray) {
		if(log.isDebugEnabled()) {
			log.debug("setSelectedObjects@" + hashCode()  + ":" + (nsarray != null ? nsarray.count() : "0"));
		}
		super.setSelectedObjects(nsarray);
	}

	/**
	 * Overridden to track selection changes.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean setSelectionIndexes(NSArray nsarray) {
		if(log.isDebugEnabled()) {
			log.debug("setSelectionIndexes@" + hashCode()  + ":" + (nsarray != null ? nsarray.count() : "0"),
					new RuntimeException("Dummy for Stacktrace"));
		}
		return super.setSelectionIndexes(nsarray);
	}

	/**
	 * Overriden to re-set the selection. Why is this cleared in the super class?
	 */
	@Override
	public void setNumberOfObjectsPerBatch(int i) {
		NSArray<T> oldSelection = selectedObjects();
		super.setNumberOfObjectsPerBatch(i);
		setSelectedObjects(oldSelection);
	}

	/**
	 * Overridden to clear out the sort ordering if it is no longer applicable.
	 */
	@Override
	public void setDataSource(EODataSource eodatasource) {
		EODataSource old = dataSource();
		super.setDataSource(eodatasource);
		if(old != null && eodatasource != null && ERXExtensions.safeDifferent(old.classDescriptionForObjects(), eodatasource.classDescriptionForObjects())) {
			setSortOrderings(NSArray.EmptyArray);
		}
	}

	/**
	 * Overriden to re-set the selection. Why is this cleared in the super class?
	 */
	@Override
	public Object displayNextBatch() {
		NSArray<T> oldSelection = selectedObjects();
		Object result = super.displayNextBatch();
		setSelectedObjects(oldSelection);
		return result;
	}

	/**
	 * Overriden to re-set the selection. Why is this cleared in the super class?
	 */
	@Override
	public Object displayPreviousBatch() {
		NSArray<T> oldSelection = selectedObjects();
		Object result = super.displayPreviousBatch();
		setSelectedObjects(oldSelection);
		return result;
	}
	
	/**
	 * Selects the visible objects.
	 *
	 */
	public Object selectFilteredObjects() {
		setSelectedObjects(filteredObjects());
		return null;
	}

	/**
	 * Overridden to log a message when more than one sort order exists. Useful to track down errors.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void setSortOrderings(NSArray nsarray) {
		super.setSortOrderings(nsarray);
		if(nsarray != null && nsarray.count() > 1) {
			if(log.isDebugEnabled()) {
				log.debug("More than one sort order: " + nsarray);
			}
		}
	}

	public void clearExtraQualifiers() {
		_extraQualifiers.removeAllObjects();
	}
	
	/* Generified methods */
	
	/**
	 * Overridden to return generic types
	 */
	@Override
	@SuppressWarnings("unchecked")
	public NSArray<T> allObjects() {
		return super.allObjects();
	}
	
	/**
	 * Overridden to return generic types
	 */
	@Override
	@SuppressWarnings("unchecked")
	public NSArray<String> allQualifierOperators() {
		return super.allQualifierOperators();
	}
	
	/**
	 * Overridden to return generic types
	 */
	@Override
	@SuppressWarnings("unchecked")
	public NSArray<T> displayedObjects() {
		return super.displayedObjects();
	}
	
	/**
	 * Overridden to return generic types
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T selectedObject() {
		return (T) super.selectedObject();
	}
	
	/**
	 * Overridden to return generic types
	 */
	@Override
	@SuppressWarnings("unchecked")
	public NSArray<EOSortOrdering> sortOrderings() {
		return super.sortOrderings();
	}
	
	/**
	 * Overridden to return correct result when no objects are displayed
	 */
	
	@Override
	public int indexOfFirstDisplayedObject() {
		if (currentBatchIndex() == 1 && displayedObjects().count() == 0)
			return 0;
		return super.indexOfFirstDisplayedObject();
	}

	/**
	 * Overridden to return correct index if the number of filtered objects
	 * is not a multiple of <code>numberOfObjectsPerBatch</code> and we are
	 * on the last batch index. The superclass incorrectly uses allObjects
	 * instead of displayedObjects to determine the index value.
	 */
	@Override
	public int indexOfLastDisplayedObject() {
        int computedEnd = numberOfObjectsPerBatch() * currentBatchIndex();
        int realEnd = displayedObjects().count();
        if(numberOfObjectsPerBatch() == 0) {
            return realEnd;
        }
        if (currentBatchIndex() > 1) {
        	realEnd += numberOfObjectsPerBatch() * (currentBatchIndex() - 1);
        }
        return realEnd >= computedEnd ? computedEnd : realEnd;
    }
}
