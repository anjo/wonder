package er.extensions;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import java.util.*;
import java.io.*;
import java.math.BigDecimal;

/**
 * Collection of {@link com.webobjects.foundation.NSArray NSArray} utilities.
 */
public class ERXArrayUtilities extends Object {
    /**
     * Holds the null grouping key for use when grouping objects
     * based on a key that might return null and nulls are allowed
     */
    public static final String NULL_GROUPING_KEY="**** NULL GROUPING KEY ****";

    /** caches if array utilities have been initialized */
    private static boolean initialized = false;
    
    /**
     * Groups an array of objects by a given key path. The dictionary
     * that is returned contains keys that correspond to the grouped
     * keys values. This means that the object pointed to by the key
     * path must be a cloneable object. For instance using the key path
     * 'company' would not work because enterprise objects are not
     * cloneable. Instead you might choose to use the key path 'company.name'
     * or 'company.primaryKey', if your enterprise objects support this
     * see {@link ERXGenericRecord} if interested.
     * @param objects array of objects to be grouped
     * @param keyPath path used to group the objects.
     * @return a dictionary where the keys are the grouped values and the
     * 		objects are arrays of the objects that have the grouped
     *		characteristic. Note that if the key path returns null
     *		then one of the keys will be the static ivar NULL_GROUPING_KEY
     */
    public static NSDictionary arrayGroupedByKeyPath(NSArray objects, String keyPath) {
        return arrayGroupedByKeyPath(objects,keyPath,true,null);
    }

    /**
     * Groups an array of objects by a given key path. The dictionary
     * that is returned contains keys that correspond to the grouped
     * keys values. This means that the object pointed to by the key
     * path must be a cloneable object. For instance using the key path
     * 'company' would not work because enterprise objects are not
     * cloneable. Instead you might choose to use the key path 'company.name'
     * of 'company.primaryKey', if your enterprise objects support this
     * see {@link ERXGenericRecord} if interested.
     * @param eos array of objects to be grouped
     * @param keyPath path used to group the objects.
     * @param includeNulls determines if keyPaths that resolve to null
     *		should be allowed into the group.
     * @param extraKeyPathForValues allows a selected object to include
     *		more objects in the group. This is going away in the
     *		future.
     * @return a dictionary where the keys are the grouped values and the
     * 		objects are arrays of the objects that have the grouped
     *		characteristic. Note that if the key path returns null
     *		then one of the keys will be the static ivar NULL_GROUPING_KEY
     */
    // FIXME: Get rid of extraKeyPathForValues, it doesn't make sense.
    public static NSDictionary arrayGroupedByKeyPath(NSArray eos,
                                                     String keyPath,
                                                     boolean includeNulls,
                                                     String extraKeyPathForValues) {
        NSMutableDictionary result=new NSMutableDictionary();
        for (Enumeration e=eos.objectEnumerator(); e.hasMoreElements();) {
            Object eo = e.nextElement();
            Object key = NSKeyValueCodingAdditions.Utility.valueForKeyPath(eo,keyPath);
            boolean isNullKey = key==null || key instanceof NSKeyValueCoding.Null;
            if (!isNullKey || includeNulls) {
                if (isNullKey) key=NULL_GROUPING_KEY;
                NSMutableArray existingGroup=(NSMutableArray)result.objectForKey(key);
                if (existingGroup==null) {
                    existingGroup=new NSMutableArray();
                    result.setObjectForKey(existingGroup,key);
                }
                if (extraKeyPathForValues!=null) {
                    Object value=NSKeyValueCodingAdditions.Utility.valueForKeyPath(eo,extraKeyPathForValues);
                    if (value!=null) existingGroup.addObject(value);
                } else
                    existingGroup.addObject(eo);
            }
        }
        return result;
    }


    /**
     * Simple comparision method to see if two array
     * objects are identical sets.
     * @param a1 first array
     * @param a2 second array
     * @return result of comparison
     */
    public static boolean arraysAreIdenticalSets(NSArray a1, NSArray a2) {
        boolean result=false;
        for (Enumeration e=a1.objectEnumerator();e.hasMoreElements();) {
            Object i=e.nextElement();
            if (!a2.containsObject(i)) {
                result=false; break;
            }
        }
        if (result) {
            for (Enumeration e=a2.objectEnumerator();e.hasMoreElements();) {
                Object i=e.nextElement();
                if (!a1.containsObject(i)) {
                    result=false; break;
                }
            }
        }
        return result;
    }

    /**
     * Filters an array using the {@link com.webobjects.eocontrol.EOQualifierEvaluation EOQualifierEvaluation} interface.
     * 
     * @param array to be filtered
     * @param qualifier to do the filtering
     * @return array of filtered results.
     */
    public static NSArray filteredArrayWithQualifierEvaluation(NSArray array, EOQualifierEvaluation qualifier) {
        if (array == null) 
            return NSArray.EmptyArray;
        else 
            return filteredArrayWithQualifierEvaluation(array.objectEnumerator(), qualifier);
    }

    /**
     * Filters any kinds of collections that implements {@link Enumeration} 
     * interface such as {@link com.webobjects.foundation.NSArray NSArray}, {@link com.webobjects.foundation.NSSet NSSet}, {@link Vector} 
     * and {@link Hashtable} using the {@link com.webobjects.eocontrol.EOQualifierEvaluation EOQualifierEvaluation} interface. 
     *
     * @param enumeration to be filtered; to obtain an enumeration, 
     *             use objectEnumerator() for the collections in 
     *             com.webobjects.foundation package 
     *             and use elements() for the Vector and Hashtable
     * @param qualifier to do the filtering
     * @return array of filtered results.
     */
    public static NSArray filteredArrayWithQualifierEvaluation(Enumeration enumeration, EOQualifierEvaluation qualifier) {
        NSMutableArray result = new NSMutableArray();
        while (enumeration.hasMoreElements()) {
            Object object = enumeration.nextElement();
            if (qualifier.evaluateWithObject(object)) 
                result.addObject(object);
        }
        return result;
    }

    /**
     * Filters any kind of collections that implements {@link Iterator} 
     * interface such as {@link ArrayList}, {@link HashMap}, {@link SortedSet} 
     * and {@link TreeSet} using the {@link com.webobjects.eocontrol.EOQualifierEvaluation EOQualifierEvaluation} interface. 
     *
     * @param iterator to be filtered; use iterator() to obtain 
     *             an iterator from the collections
     * @param qualifier to do the filtering
     * @return array of filtered results.
     */
    public static NSArray filteredArrayWithQualifierEvaluation(Iterator iterator, EOQualifierEvaluation qualifier) {
        NSMutableArray result = new NSMutableArray();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (qualifier.evaluateWithObject(object)) 
                result.addObject(object);
        }
        return result;
    }

    /**
     * Filters out duplicates of an array of enterprise objects
     * based on the value of the given key off of those objects.
     * Note: Current implementation depends on the key returning a
     * cloneable object. Also the order is not preseved from the
     * original array.
     * @param eos array of enterprise objects
     * @param key key path to be evaluated off of every enterprise
     *		object
     * @return filter array of objects based on the value of a key-path.
     */
    // FIXME: Broken implementation, relies on the value returned by the key to be Cloneable
    //		also doesn't handle the case of the key returning null or an actual keyPath
    //		and has the last object in the array winning the duplicate tie.
    // FIXME: Does not preserve order.
    public static NSArray arrayWithoutDuplicateKeyValue(NSArray eos, String key){
        NSMutableDictionary dico = new NSMutableDictionary();
        for(Enumeration e = eos.objectEnumerator(); e.hasMoreElements(); ){
            NSKeyValueCoding eo = (NSKeyValueCoding)e.nextElement();
            Object value = eo.valueForKey(key);
            if(value != null){
                dico.setObjectForKey(eo, value);
            }
        }
        return dico.allValues();
    }

    /**
     * Subtracts the contents of one array from another.
     * Note: Current implementation does not preserve order.
     * @param main array to have values removed from it.
     * @param minus array of values to remove from the main array
     * @param result array after performing subtraction.
     */
    // FIXME: This has the side effect of removing any duplicate elements from
    //		the main array as well as not preserving the order of the array
    public static NSArray arrayMinusArray(NSArray main, NSArray minus){
        NSSet result = ERXUtilities.setFromArray(main);
        return result.setBySubtractingSet(ERXUtilities.setFromArray(minus)).allObjects();
    }

    /**
     * Subtracts a single object from an array.
     * @param main array to have value removed from it.
     * @param minus object to be removed
     * @param result array after performing subtraction.
     */
    public static NSArray arrayMinusObject(NSArray main, Object object) {
        NSMutableArray mutable = new NSMutableArray(main);
        mutable.removeObject(object);
        return mutable.immutableClone();
    }
        
    /**
     * Creates an array preserving order by adding all of the
     * non-duplicate values from the second array to the first.
     * @param a1 first array
     * @param a2 second array
     * @return array containing all of the elements of the first
     *		array and all of the non-duplicate elements of
     *		the second array.
     */
    public static NSArray arrayByAddingObjectsFromArrayWithoutDuplicates(NSArray a1, NSArray a2) {
        // FIXME this is n2 -- could be made n lg n
        NSArray result=null;
        if (a2.count()==0)
            result=a1;
        else {
            NSMutableArray mutableResult=new NSMutableArray(a1);
            for (Enumeration e=a2.objectEnumerator(); e.hasMoreElements();) {
                Object elt=e.nextElement();
                if (!mutableResult.containsObject(elt)) mutableResult.addObject(elt);
            }
            result=mutableResult;
        }
        return result;
    }

    /**
     * Adds all of the non-duplicate elements from the second
     * array to the mutable array.
     * @param a1 mutable array where non-duplicate objects are
     *		added
     * @param a2 array to be added to a1
     */
    public static void addObjectsFromArrayWithoutDuplicates(NSMutableArray a1, NSArray a2) {
        for (Enumeration e=a2.objectEnumerator(); e.hasMoreElements();) {
            Object elt=e.nextElement();
            if (!a1.containsObject(elt)) a1.addObject(elt);
        }
    }

    /**
     * Recursively flattens an array of arrays into a single
     * array of elements.<br/>
     * <br/>
     * For example:<br/>
     * <code>NSArray foos;</code> //Assume exists<br/>
     * <code>NSArray bars = (NSArray)foos.valueForKey("toBars");</code>
     * In this case if <code>foos</code> contained five elements
     * then the array <code>bars</code> will contain five arrays
     * each corresponding to what <code>aFoo.toBars</code> would
     * return. To have the entire collection of <code>bars</code>
     * in one single array you would call:
     * <code>NSArray allBars = flatten(bars)</code>
     * @param array to be flattened
     * @return an array containing all of the elements from
     *		all of the arrays contained within the array
     *		passed in.
     */    
    public static NSArray flatten(NSArray array) {
        return flatten(array, false);
    }
    
    /**
     * Recursively flattens an array of arrays into a single
     * array of elements.<br/>
     * <br/>
     * For example:<br/>
     * <code>NSArray foos;</code> //Assume exists<br/>
     * <code>NSArray bars = (NSArray)foos.valueForKey("toBars");</code>
     * In this case if <code>foos</code> contained five elements
     * then the array <code>bars</code> will contain five arrays
     * each corresponding to what <code>aFoo.toBars</code> would
     * return. To have the entire collection of <code>bars</code>
     * in one single array you would call:
     * <code>NSArray allBars = flatten(bars)</code>
     * @param array to be flattened
     * @param filterDuplicates determines if the duplicate values
     * 		should be filtered
     * @return an array containing all of the elements from
     *		all of the arrays contained within the array
     *		passed in.
     */
    public static NSArray flatten(NSArray array, boolean filterDuplicates) {
        NSMutableArray newArray = null;
        for (int i=0; i < array.count(); i++) {
            Object element = array.objectAtIndex(i);
            if (element instanceof NSArray) {
                if (newArray==null) {
                    newArray = new NSMutableArray();
                    for (int j = 0; j < i; j++) {
                        if (array.objectAtIndex(j) != null) {
                            if (!filterDuplicates || !newArray.containsObject(array.objectAtIndex(j))) {
                                newArray.addObject(array.objectAtIndex(j));                                
                            }
                        }
                    }
                }
                NSArray a = flatten((NSArray)element);
                for (int j = 0; j < a.count(); j++) {
                    if (a.objectAtIndex(j) != null) {
                        if (!filterDuplicates || !newArray.containsObject(array.objectAtIndex(j))) {
                            newArray.addObject(a.objectAtIndex(j));
                        }
                    }
                }
            }
        }
        return (newArray !=null) ? newArray : array;
    }

    /**
     * Creates an NSArray from a resource associated with a given bundle
     * that is in property list format.<br/>
     * @param name name of the file or resource.
     * @param bundle NSBundle to which the resource belongs.
     * @return NSArray de-serialized from the property list.
     */
    public static NSArray arrayFromPropertyList(String name, NSBundle bundle) {
        return (NSArray)NSPropertyListSerialization.propertyListFromString(ERXStringUtilities.stringFromResource(name, "plist", bundle));
    }

    /**
     * Performs multiple key-value coding calls against an array.
     * @param array object to be acted upon.
     * @param paths array of keypaths.
     * @return returns an array containing an array of values for
     *         every keypath.
     */
    public static NSArray valuesForKeyPaths(Object array, NSArray paths) {
        NSMutableArray result = new NSMutableArray();

        Enumeration e = paths.objectEnumerator();
        while(e.hasMoreElements()) {
            result.addObject(NSKeyValueCodingAdditions.Utility.valueForKeyPath(array, (String)e.nextElement()));
        }
        return result;
    }

    /**
     * Sorts a given array with a key in ascending fashion and returns a mutable clone of the result.
     * @param array array to be sorted.
     * @param key sort key.
     * @return mutable clone of sorted array.
     */
    // CHECKME ak: I probably wrote this, but do we really need it?
    public static NSMutableArray sortedMutableArraySortedWithKey(NSArray array, String key) {
        return sortedArraySortedWithKey(array, key).mutableClone();
    }

    /**
     * Sorts a given array with a key in ascending fashion.
     * @param array array to be sorted.
     * @param key sort key.
     * @return mutable clone of sorted array.
     */
    public static NSArray sortedArraySortedWithKey(NSArray array, String key) {
        return sortedArraySortedWithKey(array, key, null);
    }

    /**
     * Sorts a given array with a key in ascending fashion.
     * @param array array to be sorted.
     * @param key sort key.
     * @param selector sort order selector to use, if null, then sort will be ascending.
     * @return sorted array.
     */
    public static NSArray sortedArraySortedWithKey(NSArray array, String key, NSSelector selector) {
        ERXAssert.PRE.notNull("Attempting to sort null array of eos.", array);
        ERXAssert.PRE.notNull("Attepting to sort array of eos with null key.", key);
        NSArray order=new NSArray(new Object[] {EOSortOrdering.sortOrderingWithKey(key, selector == null ? EOSortOrdering.CompareCaseInsensitiveAscending : selector)});
        return EOSortOrdering.sortedArrayUsingKeyOrderArray(array, order);
    }

    /**
     * Sorts a given mutable array with a key in place.
     * @param array array to be sorted.
     * @param key sort key.
     */
    public static void sortArrayWithKey(NSMutableArray array, String key) {
        sortArrayWithKey(array, key, null);
    }

    /**
     * Sorts a given mutable array with a key in place.
     * @param array array to be sorted.
     * @param key sort key.
     * @param selector sort order selector to use, if null, then sort will be ascending.
     */
    public static void sortArrayWithKey(NSMutableArray array, String key, NSSelector selector) {
        ERXAssert.PRE.notNull("Attempting to sort null array of eos.", array);
        ERXAssert.PRE.notNull("Attepting to sort array of eos with null key.", key);
        NSArray order=new NSArray(new Object[] {EOSortOrdering.sortOrderingWithKey(key, selector == null ? EOSortOrdering.CompareCaseInsensitiveAscending : selector)});
        EOSortOrdering.sortArrayUsingKeyOrderArray(array, order);
    }

    /**
     * The core class of {@link com.webobjects.foundation.NSArray.Operator NSArray.Operator}, which adds support for keyPaths.<br/>
     */

    static abstract class BaseOperator implements NSArray.Operator {
        public NSArray contents(NSArray array, String keypath) {
            if(array != null && array.count() > 0  && keypath != null && keypath.length() > 0) {
                array = (NSArray)NSKeyValueCodingAdditions.Utility.valueForKeyPath(array, keypath);
            }
            return array;
        }
    }

    /**
     * Define an {@link com.webobjects.foundation.NSArray.Operator NSArray.Operator} for the key <b>sort</b>.<br/>
     * <br/>
     * This allows for key value paths like:<br/>
     * <br/>
     * <code>myArray.valueForKey("@sort.firstName");</code><br/>
     * <br/>
     * Which in this case would return myArray sorted ascending by first name.
     */
    static class SortOperator implements NSArray.Operator
    {
        private NSSelector selector;
        
        /** public empty constructor */
	public SortOperator(NSSelector selector) {
            this.selector = selector;
        }

        /**
         * Sorts the given array by the keypath.
         * @param array array to be sorted.
         * @param keypath sort key.
         * @return immutable sorted array.
         */
	public Object compute(NSArray array, String keypath) {
	    synchronized (array) {
		if (array.count() < 2)
		    return array;
		return sortedArraySortedWithKey(array, keypath, selector);
	    }
	}
    }

    /**
     * Define an {@link com.webobjects.foundation.NSArray.Operator NSArray.Operator} for the key <b>fetchSpec</b>.<br/>
     * <br/>
     * This allows for key value paths like:<br/>
     * <br/>
     * <code>myArray.valueForKey("@fetchSpec.fetchUsers");</code><br/>
     * <br/>
     * Which in this case would return myArray filtered and sorted by the
     * EOFetchSpecification named fetchUsers.
     */
    static class FetchSpecOperator implements NSArray.Operator
    {
        /** public empty constructor */
	public FetchSpecOperator() {}

    /**
     * Filters and sorts the given array by the named fetchspecification.
     * @param array array to be filtered.
     * @param keypath name of fetch specification.
     * @return immutable filtered array.
     */
	public Object compute(NSArray array, String keypath) {
	    synchronized(array) {
		if(array.count() == 0) {
		    return array;
		}
		EOEnterpriseObject eo = (EOEnterpriseObject)array.objectAtIndex(0);
		return filteredArrayWithFetchSpecificationNamedEntityNamed(array, keypath, eo.entityName());
	    }
	}
    }

    /**
     * Define an {@link com.webobjects.foundation.NSArray.Operator NSArray.Operator} for the key <b>flatten</b>.<br/>
     * <br/>
     * This allows for key value paths like:<br/>
     * <br/>
     * <code>myArray.valueForKey("@flatten");</code><br/>
     * <br/>
     * Which in this case would return myArray flattened.
     */
    static class FlattenOperator extends BaseOperator {
        /** public empty constructor */
        public FlattenOperator() {}

        /**
        * Flattens the given array.
         * @param array array to be filtered.
         * @param keypath name of fetch specification.
         * @return immutable filtered array.
         */
        public Object compute(NSArray array, String keypath) {
            array = flatten(array);
            return contents(array, keypath);
        }
    }

    /**
     * Define an {@link com.webobjects.foundation.NSArray.Operator NSArray.Operator} for the key <b>isEmpty</b>.<br/>
     * <br/>
     * This allows for key value paths like:<br/>
     * <br/>
     * <code>myArray.valueForKey("@isEmpty");</code><br/>
     * <br/>
     * 
     */
    static class IsEmptyOperator implements NSArray.Operator {
        /** public empty constructor */
        public IsEmptyOperator() {}

        /**
        * returns true if the given array is empty, usefull for WOHyperlink disabled binding.
         * @param array array to be checked.
         * @param keypath name of fetch specification.
         * @return <code>Boolean.TRUE</code> if array is empty, <code>Boolean.FALSE</code> otherwise.
         */
        public Object compute(NSArray array, String keypath) {
	    synchronized (array) {
		return array.count() == 0 ? Boolean.TRUE : Boolean.FALSE;
	    }
        }
    }


    /**
     * Define an {@link com.webobjects.foundation.NSArray.Operator NSArray.Operator} for the key <b>subarrayWithRange</b>.<br/>
     * <br/>
     * This allows for key value paths like:<br/>
     * <br/>
     * <code>myArray.valueForKey("@subarrayWithRange.3-20");</code><br/>
     * <br/>
     *
     */
    static class SubarrayWithRangeOperator implements NSArray.Operator {
        /** public empty constructor */
        public SubarrayWithRangeOperator() {}

        /**
         * @param array array to be checked.
         * @param keypath name of fetch specification.
         * @return <code>Boolean.TRUE</code> if array is empty, <code>Boolean.FALSE</code> otherwise.
         */
        public Object compute(NSArray array, String keypath) {
	    synchronized (array) {
		int i1 = keypath.indexOf(".");
		int i2 = keypath.indexOf("-");
		if ( i1 == -1 || i2 == -1 ) {
		    throw new IllegalArgumentException("subarrayWithRange must be used like @subarrayWithRange.start-length");
		}
		int start = Integer.parseInt(keypath.substring(i1, i2));
		int length = Integer.parseInt(keypath.substring(i2, keypath.length()));
		return array.subarrayWithRange(new NSRange(start, length));
	    }
        }
    }

    

    /**
     * Define an {@link com.webobjects.foundation.NSArray.Operator NSArray.Operator} for the key <b>flatten</b>.<br/>
     * <br/>
     * This allows for key value paths like:<br/>
     * <br/>
     * <code>myArray.valueForKey("@flatten");</code><br/>
     * <br/>
     * Which in this case would return myArray flattened.
     */
    static class UniqueOperator extends BaseOperator {
        /** public empty constructor */
        public UniqueOperator() {}

        /**
        * Flattens the given array.
         * @param array array to be filtered.
         * @param keypath name of fetch specification.
         * @return immutable filtered array.
         */
        public Object compute(NSArray array, String keypath) {
	    synchronized (array) {
                array = arrayWithoutDuplicates(array);
                array = contents(array, keypath);
                return array;
	    }
        }
    }


    /**
     * Define an {@link com.webobjects.foundation.NSArray.Operator NSArray.Operator} for the key <b>objectAtIndex</b>.<br/>
     * <br/>
     * This allows for key value paths like:<br/>
     * <br/>
     * <code>myArray.valueForKey("@objectAtIndex.3.firstName");</code><br/>
     * <br/>
     *
     */
    static class ObjectAtIndexOperator implements NSArray.Operator {
        /** public empty constructor */
        public ObjectAtIndexOperator() {}

        /**
         * returns the keypath value for n-ths object.
         * @param array array to be checked.
         * @param keypath integer value of index (zero based).
         * @return <code>null</code> if array is empty or value is not in index, <code>keypath</code> value otherwise.
         */
        public Object compute(NSArray array, String keypath) {
            synchronized (array) {
                int end = keypath.indexOf(".");
                int index = Integer.parseInt(keypath.substring(0, end == -1 ? keypath.length() : end));
                Object value = null;
                if(index < array.count() )
                    value = array.objectAtIndex(index);
                if(end != -1 && value != null) {
                    value = NSKeyValueCodingAdditions.Utility.valueForKeyPath(value, keypath.substring(end+1));
                }
                return value;
            }
        }
    }

    /**
     * Define an {@link com.webobjects.foundation.NSArray.Operator NSArray.Operator} for the key <b>avgNonNull</b>.<br/>
     * <br/>
     * This allows for key value paths like:<br/>
     * <br/>
     * <code>myArray.valueForKey("@avgNonNull.revenue");</code><br/>
     * <br/>
     * which will sum up all values and divide by the number of nun-null entries. 
     */
    static class AvgNonNullOperator implements NSArray.Operator {
        /** public empty constructor */
        public AvgNonNullOperator() {}

        /**
         * returns the average value for over all non-null values.
         * @param array array to be checked.
         * @param keypath value of average.
         * @return computed average as double or <code>NULL</code>.
         */
        public Object compute(NSArray array, String keypath) {
            synchronized (array) {
                BigDecimal result = new BigDecimal(0L);
                int count = 0;
                
                for(Enumeration e = array.objectEnumerator(); e.hasMoreElements();) {
                    Object value = NSKeyValueCodingAdditions.Utility.valueForKeyPath(e.nextElement(), keypath);
                    if(value != null && value != NSKeyValueCoding.NullValue) {
                        count = count+1;
                        result = result.add(ERXValueUtilities.bigDecimalValue(value));
                    }
                }
                if(count == 0) {
                    return null;
                }
                return result.divide(BigDecimal.valueOf((long) count), result.scale() + 4, 6);
            }
        }
    }

    /**
     * Define an {@link com.webobjects.foundation.NSArray.Operator NSArray.Operator} for the key <b>reverse</b>.<br/>
     * <br/>
     * This allows for key value paths like:<br/>
     * <br/>
     * <code>myArray.valueForKey("@reverse.someMorePath");</code><br/>
     * <br/>
     * which return a reversed result as to you would normally get.
     */
    static class ReverseOperator extends BaseOperator {
        /** public empty constructor */
        public ReverseOperator() {}

        /**
         * returns the reverse value for the values of the keypath.
         * @param array array to be checked.
         * @param keypath value of reverse.
         * @return reversed array for keypath.
         */
        public Object compute(NSArray array, String keypath) {
            synchronized (array) {
                array = reverse(array);
                array = contents(array, keypath);
                return array;
            }
        }
    }
    /** 
     * Will register new NSArray operators
     * <b>sort</b>, <b>sortAsc</b>, <b>sortDesc</b>, <b>sortInsensitiveAsc</b>,
     * <b>sortInsensitiveDesc</b>, <b>unique</b>, <b>flatten</b>, <b>reverse</b> and <b>fetchSpec</b> 
     */
    public static void initialize() {
	if (initialized) {
	    return;
	}
	initialized = true;
        if (ERXProperties.booleanForKeyWithDefault("er.extensions.ERXArrayUtilities.ShouldRegisterOperators", true)) {
            NSArray.setOperatorForKey("sort", new SortOperator(EOSortOrdering.CompareAscending));
            NSArray.setOperatorForKey("sortAsc", new SortOperator(EOSortOrdering.CompareAscending));
            NSArray.setOperatorForKey("sortDesc", new SortOperator(EOSortOrdering.CompareDescending));
            NSArray.setOperatorForKey("sortInsensitiveAsc", new SortOperator(EOSortOrdering.CompareCaseInsensitiveAscending));
            NSArray.setOperatorForKey("sortInsensitiveDesc", new SortOperator(EOSortOrdering.CompareCaseInsensitiveDescending));
            NSArray.setOperatorForKey("flatten", new FlattenOperator());
            NSArray.setOperatorForKey("fetchSpec", new FetchSpecOperator());
            NSArray.setOperatorForKey("unique", new UniqueOperator());
            NSArray.setOperatorForKey("isEmpty", new IsEmptyOperator());
            NSArray.setOperatorForKey("subarrayWithRange", new SubarrayWithRangeOperator());
            NSArray.setOperatorForKey("objectAtIndex", new ObjectAtIndexOperator());
            NSArray.setOperatorForKey("avgNonNull", new AvgNonNullOperator());
            NSArray.setOperatorForKey("reverse", new ReverseOperator());
        }
    }

    
    /**
     * Filters out all of the duplicate objects in
     * a given array.<br/> Preserves the order now.
     * @param anArray to be filtered
     * @return filtered array.
     */
    public static NSArray arrayWithoutDuplicates(NSArray anArray) {
        String dummy = "DUMMY";
        NSMutableArray result = new NSMutableArray();
        NSMutableDictionary already = new NSMutableDictionary();
        for(Enumeration e = anArray.objectEnumerator(); e.hasMoreElements();){
            Object object = e.nextElement();
            if(already.valueForKey(""+object.hashCode())==null){
                already.takeValueForKey(dummy, ""+object.hashCode());
                result.addObject(object);
            }
        }
        return result;
    }

    /**
     * Batches an NSArray into sub-arrays of the given size.
     * @param array array to batch
     * @param batchSize number of items in each batch
     * @return NSArray of NSArrays, each with at most batchSize items
     */
     public static NSArray batchedArrayWithSize(NSArray array, int batchSize) {
        if(array == null || array.count() == 0)
            return NSArray.EmptyArray;

        NSMutableArray batchedArray = new NSMutableArray();
        int count = array.count();

        for(int i = 0; i < count; i+=batchSize) {
            int length = batchSize;
            if(i + length > count)
                length = count - i;
            batchedArray.addObject(array.subarrayWithRange(new NSRange(i, length)));
        }
        return batchedArray;
    }

    /**
     * Filters a given array with a named fetch specification and bindings.
     *
     * @param array array to be filtered.
     * @param fetchSpec name of the {@link com.webobjects.eocontrol.EOQualifierEvaluation EOQualifierEvaluation}.
     * @param entity name of the {@link com.webobjects.eoaccess.EOEntity EOEntity} 
     * to which the fetch specification is associated.
     * @param bindings bindings dictionary for qualifier variable substitution.
     * @return array filtered and sorted by the named fetch specification.
     */    
    public static NSArray filteredArrayWithEntityFetchSpecification(NSArray array, String entity, String fetchSpec, NSDictionary bindings) {
        EOFetchSpecification spec = EOFetchSpecification.fetchSpecificationNamed(fetchSpec, entity);
        NSArray sortOrderings, result;
        EOQualifier qualifier;

        if (bindings != null) {
            spec = spec.fetchSpecificationWithQualifierBindings(bindings);
        }

        result = new NSArray(array);

        if ((qualifier = spec.qualifier()) != null) {
            result = EOQualifier.filteredArrayWithQualifier(result, qualifier);
        }

        if ((sortOrderings = spec.sortOrderings()) != null) {
            result = EOSortOrdering.sortedArrayUsingKeyOrderArray(result,sortOrderings);
        }

        return result;
    }
    
    /**
     * @deprecated
     */
    public static NSArray filteredArrayWithFetchSpecificationNamedEntityNamedBindings(NSArray array, String fetchSpec, String entity, NSDictionary bindings) {
        return filteredArrayWithEntityFetchSpecification( array, entity, fetchSpec, bindings);
    }

    /**
     * Filters a given array with a named fetch specification.
     *
     * @param array array to be filtered.
     * @param fetchSpec name of the {@link com.webobjects.eocontrol.EOQualifierEvaluation EOQualifierEvaluation}.
     * @param entity name of the {@link com.webobjects.eoaccess.EOEntity EOEntity} 
     * to which the fetch specification is associated.
     * @return array filtered and sorted by the named fetch specification.
     */
    public static NSArray filteredArrayWithEntityFetchSpecification(NSArray array, String fetchSpec, String entity) {
        return ERXArrayUtilities.filteredArrayWithEntityFetchSpecification(array, entity, fetchSpec, null);
    }

    /**
     * @deprecated
     */
    public static NSArray filteredArrayWithFetchSpecificationNamedEntityNamed(NSArray array, String fetchSpec, String entity) {
        return ERXArrayUtilities.filteredArrayWithEntityFetchSpecification(array, entity, fetchSpec, null);
    }
    /**
     * shifts a given object in an array one value to the left (index--).
     *
     * @param array array to be modified.
     * @param object the object that should be moved
     */
    public static void shiftObjectLeft(NSMutableArray array, Object object) {
        int index = array.indexOfObject(object);
        if (index == -1) return;
        if (index > 0) {
            array.insertObjectAtIndex(object, index -1);
            array.removeObjectAtIndex(index + 1);
        }
    }

    /**
     * shifts a given object in an array one value to the right (index++).
     *
     * @param array array to be modified.
     * @param object the object that should be moved
     */
    public static void shiftObjectRight(NSMutableArray array, Object object) {
        int index = array.indexOfObject(object);
        if (index == -1) return;
        if (index < array.count() - 1) {
            array.insertObjectAtIndex(object, index + 2);
            array.removeObjectAtIndex(index);
        }
    }

    /**
     * Function to determine if an array contains any of
     * the elements of another array.
     * @param array to test if it contains any of the objects
     * @param objects array of objects to test if the first array
     *		contains any of
     * @return if the first array contains any elements from the second
     *		array
     */
    public static boolean arrayContainsAnyObjectFromArray(NSArray array, NSArray objects) {
        boolean arrayContainsAnyObject = false;
        if (array != null && objects != null && array.count() > 0 && objects.count() > 0) {
            for (Enumeration e = objects.objectEnumerator(); e.hasMoreElements();) {
                if (array.containsObject(e.nextElement())) {
                    arrayContainsAnyObject = true; break;
                }
            }
        }
        return arrayContainsAnyObject;
    }

    /**
     * Function to determine if an array contains all of
     * the elements of another array.
     * @param array to test if it contains all of the objects of another array
     * @param objects array of objects to test if the first array
     *		contains all of
     * @return if the first array contains all of the elements from the second
     *		array
     */
    public static boolean arrayContainsArray(NSArray array, NSArray objects) {
        boolean arrayContainsAllObjects = true;
        if (array != null && objects != null && array.count() > 0 && objects.count() > 0) {
            for (Enumeration e = objects.objectEnumerator(); e.hasMoreElements();) {
                if (!array.containsObject(e.nextElement())) {
                    arrayContainsAllObjects = false; break;
                }
            }
        } else if (array == null || array.count() == 0) {
            return false;
        }
        return arrayContainsAllObjects;        
    }

    /**
     * Intersects the elements of two arrays. This has the effect of
     * stripping out duplicates.
     * @param array1 the first array
     * @param array2 the second array
     * @return the intersecting elements
     */
    public static NSArray intersectingElements(NSArray array1, NSArray array2) {
        NSMutableArray intersectingElements = null;
        if (array1 != null && array1.count() > 0 && array2 != null && array2.count() > 0) {
            intersectingElements = new NSMutableArray();
            NSArray bigger = array1.count() > array2.count() ? array1 : array2;
            NSArray smaller = array1.count() > array2.count() ? array2 : array1;
            for (Enumeration e = smaller.objectEnumerator(); e.hasMoreElements();) {
                Object object = e.nextElement();
                if (bigger.containsObject(object) && !intersectingElements.containsObject(object))
                    intersectingElements.addObject(object);
            }
        }
        return intersectingElements != null ? intersectingElements : NSArray.EmptyArray;
    }

    /**
     * Reverses the elements of an array
     * @param array to be reversed
     * @return reverse ordered array
     */
    public static NSArray reverse(NSArray array) {
        NSArray reverse = null;
        if (array != null && array.count() > 0) {
            NSMutableArray reverseTemp = new NSMutableArray(array.count());
            for (Enumeration reverseEnumerator = array.reverseObjectEnumerator(); reverseEnumerator.hasMoreElements();) {
                reverseTemp.addObject(reverseEnumerator.nextElement());
            }
            reverse = reverseTemp.immutableClone();
        }
        return reverse != null ? reverse : NSArray.EmptyArray;
    }

    /**
     * Displays a list of attributes off of
     * objects in a 'friendly' manner. <br/>
     * <br/>
     * For example, given an array containing three user
     * objects and the attribute key "firstName", the
     * result of calling this method would be the string:
     * "Max, Anjo and Patrice".
     * @param list of objects to be displayed in a friendly
     *		manner
     * @param attribute key to be called on each object in
     *		the list
     * @param nullArrayDisplay string to be returned if the
     *		list is null or empty
     * @param separator string to be used for the first items
     * @param finalSeparator used between the last items
     * @return friendly display string
     */
    public static String friendlyDisplayForKeyPath(NSArray list, String attribute, String nullArrayDisplay, String separator, String finalSeparator) {
        String result=null;
        int count = list!=null ? list.count() : 0;
        if (count==0) {
            result=nullArrayDisplay;
        } else if (count == 1) {
            result= (attribute!= null ? NSKeyValueCodingAdditions.Utility.valueForKeyPath(list.objectAtIndex(0), attribute) : list.objectAtIndex(0)).toString();
        } else if (count > 1) {
            StringBuffer buffer = new StringBuffer();
            for(int i = 0; i < count; i++) {
                String attributeValue = (attribute!= null ? NSKeyValueCodingAdditions.Utility.valueForKeyPath(list.objectAtIndex(i), attribute) : list.objectAtIndex(i)).toString();
                if (i>0) buffer.append(i == (count - 1) ? finalSeparator : separator);
                buffer.append(attributeValue);
            }
            result=buffer.toString();
        }
        return result;
    }
}
