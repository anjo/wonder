package com.webobjects.foundation;

import java.util.Collection;

/**
 * NSSet reimplementation to support JDK 1.5 templates. Use with
 * 
 * <pre>
 * NSMutableSet&lt;T&gt; set = new NSMutableSet&lt;T&gt;();
 * set.put(new T())
 * 
 * for (T t : set)
 *     logger.debug(t);
 * </pre>
 * 
 * @param &lt;T&gt;
 *            type of set contents
 */

//TODO iterator.remove() throws unimplemented

public class NSMutableSet<E> extends NSSet<E> {
	public NSMutableSet() {
	}

	public NSMutableSet(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("Capacity cannot be less than 0");
		}
		
		_ensureCapacity(capacity);
	}

	public NSMutableSet(E object) {
		super(object);
	}

	public NSMutableSet(E[] objects) {
		super(objects);
	}

	public NSMutableSet(NSArray<? extends E> objects) {
		super(objects);
	}

	public NSMutableSet(NSSet<? extends E> otherSet) {
		super(otherSet);
	}

	public void addObject(E object) {
		if (object == null) {
			throw new IllegalArgumentException("Attempt to insert null into an  " + getClass().getName() + ".");
		}
		int capacity = _capacity;
		int count = _count;
		if (++count > capacity) {
			_ensureCapacity(count);
		}
		if (_NSCollectionPrimitives.addValueToSet(object, _objects, _flags)) {
			_count = count;
			_objectsCache = null;
		}
	}

	public E removeObject(Object object) {
		Object result = null;
		if (object != null && _count != 0) {
			result = _NSCollectionPrimitives.removeValueInHashTable(object, _objects, _objects, _flags);
			if (result != null) {
				_count--;
				_deletionLimit--;
				if (_count == 0 || _deletionLimit == 0) {
					_clearDeletionsAndCollisions();
				}
				_objectsCache = null;
			}
		}
		return (E) result;
	}

	public void removeAllObjects() {
		if (_count != 0) {
			_objects = new Object[_hashtableBuckets];
			_flags = new byte[_hashtableBuckets];
			_count = 0;
			_objectsCache = null;
			_deletionLimit = _NSCollectionPrimitives.deletionLimitForTableBuckets(_hashtableBuckets);
		}
	}

	public void setSet(NSSet<? extends E> otherSet) {
		if (otherSet != this) {
			removeAllObjects();
			if (otherSet != null) {
				E[] objects = (E[])otherSet.objectsNoCopy();
				for (int i = 0; i < objects.length; i++) {
					addObject(objects[i]);
				}

			}
		}
	}

	public void addObjectsFromArray(NSArray<? extends E> array) {
		if (array != null) {
			E[] objects = (E[])array.objectsNoCopy();
			for (int i = 0; i < objects.length; i++) {
				addObject(objects[i]);
			}

		}
	}

	public void intersectSet(NSSet<?> otherSet) {
		if (otherSet != this) {
			if (otherSet == null || otherSet.count() == 0) {
				removeAllObjects();
				return;
			}
			E[] objects = (E[])objectsNoCopy();
			for (int i = 0; i < objects.length; i++) {
				if (otherSet.member(objects[i]) == null) {
					removeObject(objects[i]);
				}
			}

		}
	}

	public void subtractSet(NSSet<?> otherSet) {
		if (otherSet == null || otherSet.count() == 0) {
			return;
		}
		if (otherSet == this) {
			removeAllObjects();
			return;
		}
		Object[] objects = otherSet.objectsNoCopy();
		for (int i = 0; i < objects.length; i++) {
			if (member(objects[i]) != null) {
				removeObject(objects[i]);
			}
		}

	}

	public void unionSet(NSSet<? extends E> otherSet) {
		if (otherSet == null || otherSet.count() == 0 || otherSet == this) {
			return;
		}
		E[] objects = (E[])otherSet.objectsNoCopy();
		for (int i = 0; i < objects.length; i++) {
			addObject(objects[i]);
		}

	}

	public Object clone() {
		return new NSMutableSet<E>(this);
	}

	public NSSet<E> immutableClone() {
		return new NSSet<E>(this);
	}

	public NSMutableSet<E> mutableClone() {
		return (NSMutableSet<E>) clone();
	}

	@SuppressWarnings({ "hiding", "unchecked" })
	public static final Class _CLASS = _NSUtilities._classWithFullySpecifiedName("com.webobjects.foundation.NSMutableSet");
	static final long serialVersionUID = -6054074706096120227L;

	/**
	 * Add <tt>o</tt> to the set and return true if <tt>o</tt> is not null
	 * and the set does not already contain <tt>o</tt>. Null elements are not
	 * permitted.
	 * 
	 * @param o
	 *            element to add to this set
	 * 
	 * @return true if o is not null and the set did not already contain o
	 * @throws IllegalArgumentException
	 *             if o is null
	 */
	public boolean add(E o) {
		if (contains(o)) {
			return false;
		}

		addObject(o);

		return true;
	}

	/**
	 * If the set contains <tt>o</tt>, remove it and returns true.
	 * 
	 * @param o
	 *            element to remove from this set.
	 * 
	 * @return true if the set contained <tt>o</tt>
	 */
	public boolean remove(Object o) {
		return removeObject(o) != null ? true : false;
	}

	/**
	 * Add elements in <tt>c</tt> one by one and return true. Existing
	 * elements are not overwritten. Null elements are not permitted.
	 * 
	 * @param c
	 *            collection of elements to add to this set.
	 * 
	 * @return true if this set was modified; false otherwise.
	 */
	public boolean addAll(Collection<? extends E> c) {
		boolean updated = false;
		for (E t : c) {
			if (!contains(t)) {
				add(t);
				updated = true;
			}
		}

		return updated;
	}

	/**
	 * Retain only the elements in this set that are contained in <tt>c</tt>.
	 * 
	 * @see com.webobjects.foundation.NSMutableSet#intersectSet(NSSet)
	 * 
	 * @param c
	 *            items to retain in this set
	 * 
	 * @return true if this set was modified; false otherwise.
	 */
	public boolean retainAll(Collection<?> c) {
		NSMutableSet<Object> s = new NSMutableSet<Object>();
		boolean updated = false;
		for (Object o : c) {
			s.add(o);
			if (!contains(o)) {
				updated = true;
			}

		}
		intersectSet(s);

		return updated;
	}

	/**
	 * Remove from this set all of its elements that are contained in <tt>c</tt>.
	 * 
	 * @see com.webobjects.foundation.NSMutableSet#subtractSet(NSSet)
	 * 
	 * @param c
	 *            items to remove from this set
	 * 
	 * @return true if this set was modified; false otherwise.
	 */
	public boolean removeAll(Collection<?> c) {
		NSMutableSet<Object> s = new NSMutableSet<Object>();
		boolean updated = false;
		for (Object o : c) {
			s.add(o);
			if (!contains(o)) {
				updated = true;
			}
		}
		subtractSet(s);

		return updated;
	}

	/**
	 * Remove all of the elements from this set.
	 * 
	 * @see com.webobjects.foundation.NSMutableSet#removeAllObjects()
	 */
	public void clear() {
		removeAllObjects();
	}

}
