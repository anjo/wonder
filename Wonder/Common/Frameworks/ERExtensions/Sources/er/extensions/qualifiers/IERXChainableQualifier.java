package er.extensions.qualifiers;

import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

/**
 * IERXQualifier is the definition of methods required for chainable
 * EOAndQualifier.
 * 
 * @author mschrag
 */
public interface IERXChainableQualifier {
	/**
	 * Returns a new qualifier that represents this qualifier and'd to the given
	 * list of qualifiers.
	 * 
	 * @param qualifiers
	 *            the qualifiers to and with this qualifier
	 * @return an ERXAndQualifier
	 */
	public ERXAndQualifier and(EOQualifier... qualifiers);

	/**
	 * Returns a new qualifier that represents this qualifier or'd with the
	 * given list of qualifiers.
	 * 
	 * @param qualifiers
	 *            the qualifiers to or with this qualifier
	 * @return an ERXOrQualifier
	 */
	public ERXOrQualifier or(EOQualifier... qualifiers);

	/**
	 * Returns a new qualifier that represents this qualifier not'd.
	 * 
	 * @return an ERXNotQualifier
	 */
	public ERXNotQualifier not();

	/**
	 * Equivalent to EOQualifier.filteredArrayWithQualifier(NSArray,
	 * EOQualifier)
	 * 
	 * @param <T>
	 *            the type of the array
	 * @param array
	 *            the array to filter
	 * @return the filtered array
	 */
	public NSArray filtered(NSArray array);

	/**
	 * Equivalent to EOQualifier.filterArrayWithQualifier(NSMutableArray,
	 * EOQualfier)
	 * 
	 * @param array
	 *            the array to filter (in place)
	 */
	public void filter(NSMutableArray array);

	/**
	 * Equivalent to EOQualifier.first(NSMutableArray, EOQualfier)
	 * 
	 * @param <T>
	 *            the type of the array
	 * @param array
	 *            the array to filter (in place)
	 * @return the first matching object or null
	 */
	public <T> T first(NSArray array);

	/**
	 * Equivalent to EOQualifier.one(NSMutableArray, EOQualfier)
	 * 
	 * @param <T>
	 *            the type of the array
	 * @param array
	 *            the array to filter (in place)
	 * @return one matching object or null
	 * @throw IllegalStateException if more than one object matched
	 */
	public <T> T one(NSArray array);

	/**
	 * Equivalent to EOQualifier.requiredOne(NSArray, EOQualfier)
	 * 
	 * @param <T>
	 *            the type of the array
	 * @param array
	 *            the array to filter (in place)
	 * @return one matching object
	 * @throw IllegalStateException if more than one object matched
	 * @throw NoSuchElementException if no objects matched
	 */
	public <T> T requiredOne(NSArray array);
}
