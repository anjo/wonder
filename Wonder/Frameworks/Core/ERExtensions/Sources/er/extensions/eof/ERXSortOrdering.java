package er.extensions.eof;

import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSSelector;

import er.extensions.ERXExtensions;


/**
 * <p>
 * ERXSortOrdering is an EOSortOrdering subclass that provides support for
 * chaining (like ERXKey).
 * </p>
 * <p>
 * Examples:
 * </p>
 * 
 * <pre>
 * Person.COMPANY.dot(Company.NAME).asc().then(Person.FIRST_NAME.desc())
 * </pre>
 * 
 * @author mschrag
 * 
 */
public class ERXSortOrdering extends EOSortOrdering {
	/**
	 * Constructs an ERXSortOrdering (see EOSortOrdering).
	 * 
	 * @param key
	 *            the key to sort on
	 * @param selector
	 *            the sort selector
	 */
	public ERXSortOrdering(String key, NSSelector selector) {
		super(key, selector);
	}

	/**
	 * Constructs an ERXSortOrdering (see EOSortOrdering).
	 * 
	 * @param key
	 *            the key to sort on
	 * @param selector
	 *            the sort selector
	 */
	public ERXSortOrdering(ERXKey key, NSSelector selector) {
		this(key.key(), selector);
	}

	/**
	 * Returns ERXSortOrderings with this sort ordering followed by the provided
	 * next sort ordering.
	 * 
	 * @param nextSortOrdering
	 *            the next sort ordering to chain to this
	 * @return an array of sort orderings
	 */
	public ERXSortOrderings then(EOSortOrdering nextSortOrdering) {
		ERXSortOrderings sortOrderings = array();
		sortOrderings.addObject(nextSortOrdering);
		return sortOrderings;
	}

	/**
	 * Returns this sort ordering as an array.
	 * 
	 * @return this sort ordering as an array
	 */
	public ERXSortOrderings array() {
		ERXSortOrderings sortOrderings = new ERXSortOrderings();
		sortOrderings.addObject(this);
		return sortOrderings;
	}

	/**
	 * Returns an array sorted with this sort ordering.
	 * 
	 * @param <T> the type of the array
	 * @param array the array to sort
	 * @return a sorted copy of the array
	 */
	public <T> NSArray<T> sorted(NSArray<T> array) {
		return ERXS.sorted(array, this);
	}

	/**
	 * Sorts the given array with this sort ordering.
	 * 
	 * @param <T> the type of the array
	 * @param array the array to sort
	 */
	public <T> void sort(NSMutableArray<T> array) {
		ERXS.sort(array, this);
	}
	
	/**
	 * Provide the equals() method missing from EOSortOrder.
	 * 
	 * @param obj the Object to compare to
	 * @return <code>true</code> if obj is an EOSortOrder with the same key and selector as this object
	 */
	public boolean equals(Object obj) {
		if (obj instanceof EOSortOrdering) {
			EOSortOrdering other = (EOSortOrdering)obj;
			return ERXExtensions.safeEquals(key(), other.key()) && ERXExtensions.safeEquals(selector(), other.selector());
		}
		return false;
	}

	/**
	 * Constructs an ERXSortOrdering (see EOSortOrdering).
	 * 
	 * @param key
	 *            the key to sort on
	 * @param selector
	 *            the sort selector
	 * @return a new ERXSortOrdering
	 */
	public static ERXSortOrdering sortOrderingWithKey(String key, NSSelector selector) {
		return new ERXSortOrdering(key, selector);
	}

	/**
	 * Constructs an ERXSortOrdering (see EOSortOrdering).
	 * 
	 * @param key
	 *            the key to sort on
	 * @param selector
	 *            the sort selector
	 * @return a new ERXSortOrdering
	 */
	public static ERXSortOrdering sortOrderingWithKey(ERXKey key, NSSelector selector) {
		return new ERXSortOrdering(key, selector);
	}
	
	/**
	 * ERXSortOrderings is an NSMutableArray<EOSortOrdering> that
	 * provides methods for chaining.
	 * 
	 * @author mschrag
	 */
	public static class ERXSortOrderings extends NSMutableArray<EOSortOrdering> {
		/**
		 * Constructs an empty ERXSortOrderings.
		 */
		public ERXSortOrderings() {
			super();
		}

		/**
		 * Constructs an ERXSortOrderings with one sort order.
		 * 
		 * @param sortOrdering the sort ordering to add
		 */
		public ERXSortOrderings(EOSortOrdering sortOrdering) {
			super(sortOrdering);
		}

		/**
		 * Constructs an ERXSortOrderings with the list of sort orders.
		 * 
		 * @param sortOrderings the sort orderings to add
		 */
		public ERXSortOrderings(EOSortOrdering... sortOrderings) {
			super(sortOrderings);
		}

		/**
		 * Constructs an ERXSortOrderings with the array of sort orders.
		 * 
		 * @param sortOrderings the sort orderings to add
		 */
		public ERXSortOrderings(NSArray<EOSortOrdering> sortOrderings) {
			super(sortOrderings);
		}

		/**
		 * Adds the given sort ordering to the end of this list and
		 * returns "this" so it can be chained again.
		 * 
		 * @param nextSortOrdering the sort ordering to add
		 * @return this (with the sort ordering appended)
		 */
		public ERXSortOrderings then(EOSortOrdering nextSortOrdering) {
			addObject(nextSortOrdering);
			return this;
		}

		/**
		 * Returns an array sorted with this sort ordering.
		 * 
		 * @param <T> the type of the array
		 * @param array the array to sort
		 * @return a sorted copy of the array
		 */
		public <T> NSArray<T> sorted(NSArray<T> array) {
			return ERXS.sorted(array, this);
		}

		/**
		 * Sorts the given array with this sort ordering.
		 * 
		 * @param <T> the type of the array
		 * @param array the array to sort
		 */
		public <T> void sort(NSMutableArray<T> array) {
			ERXS.sort(array, this);
		}
	}
}
