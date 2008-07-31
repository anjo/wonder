package er.extensions;

import java.util.Enumeration;
import java.util.StringTokenizer;

import com.webobjects.eocontrol.EOClassDescription;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOQualifierEvaluation;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import com.webobjects.foundation.NSMutableSet;
import com.webobjects.foundation.NSSelector;

/**
 * ERXFullTextQualifier provides a qualifier implementation for searching with a
 * fulltext index. This depends on ERXSQLHelper.sqlForFullTextQuery providing an
 * implementation for your particular database. Note that because of differences
 * in implementions of fulltext indexing on various databases, the results you
 * obtain using this qualifier will vary across implementations. Additionally,
 * executing this qualifier on an array of EO's may differ from executing the
 * qualifier in your database. When qualifying on an array, the qualifier does
 * not address stemming and stop words.
 * 
 * @author mschrag
 */
public class ERXFullTextQualifier extends EOQualifier implements Cloneable, EOQualifierEvaluation {
	public static String FullTextContainsSelectorName = "fulltextContains";
	public static NSSelector FullTextContainsSelector = new NSSelector(ERXFullTextQualifier.FullTextContainsSelectorName, new Class[] { String.class });

	public static enum MatchType {
		/**
		 * All search terms must match for the qualifier to match.
		 */
		ALL,

		/**
		 * Only one search term must match for the qualifier to match.
		 */
		ANY
	}

	private String _keyPath;
	private String _indexName;
	private MatchType _matchType;
	private NSArray _terms;

	/**
	 * Constructs an ERXFullTextQualifier.
	 * 
	 * @param keyPath
	 *            the keypath to qualify on (some databases may ignore this)
	 * @param indexName
	 *            the name of the index to use (some databases may ignore this)
	 * @param matchType
	 *            ANY or ALL
	 * @param terms
	 *            the array of search terms
	 */
	public ERXFullTextQualifier(String keyPath, String indexName, MatchType matchType, NSArray terms) {
		_keyPath = keyPath;
		_indexName = indexName;
		_matchType = matchType;
		_terms = terms;
	}

	/**
	 * Constructs an ERXFullTextQualifier.
	 * 
	 * @param keyPath
	 *            the keypath to qualify on (some databases may ignore this)
	 * @param indexName
	 *            the name of the index to use (some databases may ignore this)
	 * @param matchType
	 *            ANY or ALL
	 * @param terms
	 *            the list of search terms
	 */
	public ERXFullTextQualifier(String keyPath, String indexName, MatchType matchType, String... terms) {
		this(keyPath, indexName, matchType, new NSArray(terms));
	}

	/**
	 * Constructs an ERXFullTextQualifier defaulting indexName to the same as
	 * keyPath.
	 * 
	 * @param keyPath
	 *            the keypath to qualify on (some databases may ignore this)
	 * @param matchType
	 *            ANY or ALL
	 * @param terms
	 *            the array of search terms
	 */
	public ERXFullTextQualifier(String keyPath, MatchType matchType, NSArray terms) {
		this(keyPath, keyPath, matchType, terms);
	}

	/**
	 * Constructs an ERXFullTextQualifier defaulting indexName to the same as
	 * keyPath.
	 * 
	 * @param keyPath
	 *            the keypath to qualify on (some databases may ignore this)
	 * @param matchType
	 *            ANY or ALL
	 * @param terms
	 *            the list of search terms
	 */
	public ERXFullTextQualifier(String keyPath, MatchType matchType, String... terms) {
		this(keyPath, keyPath, matchType, terms);
	}

	/**
	 * Returns the keypath for this qualifier.
	 * 
	 * @return the keypath for this qualifier
	 */
	public String keyPath() {
		return _keyPath;
	}

	/**
	 * Returns the index name for this qualifier.
	 * 
	 * @return the index name for this qualifier
	 */
	public String indexName() {
		return _indexName;
	}

	/**
	 * Returns the match type (ANY or ALL) for this qualifier.
	 * 
	 * @return the match type (ANY or ALL) for this qualifier
	 */
	public MatchType matchType() {
		return _matchType;
	}

	/**
	 * Returns the array of search terms for this qualifier.
	 * 
	 * @return the array of search terms for this qualifier
	 */
	public NSArray terms() {
		return _terms;
	}

	@Override
	public ERXFullTextQualifier clone() {
		return new ERXFullTextQualifier(_keyPath, _indexName, _matchType, _terms);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void addQualifierKeysToSet(NSMutableSet keys) {
		keys.addObject(_keyPath);
	}

	@Override
	public EOQualifier qualifierWithBindings(NSDictionary bindings, boolean requiresAll) {
		return clone();
	}

	@Override
	public void validateKeysWithRootClassDescription(EOClassDescription classDescription) {
		StringTokenizer keyPathTokenizer = new StringTokenizer(_keyPath, ".");
		while (keyPathTokenizer.hasMoreElements()) {
			String key = keyPathTokenizer.nextToken();
			if (keyPathTokenizer.hasMoreElements()) {
				classDescription = classDescription.classDescriptionForDestinationKey(key);
				if (classDescription == null) {
					throw new IllegalStateException("Invalid key '" + key + "' for entity '" + classDescription.entityName() + "'.");
				}
			}
			else {
				if (!classDescription.attributeKeys().containsObject(key)) {
					throw new IllegalStateException("Invalid key '" + key + "' for entity '" + classDescription.entityName() + "'.");
				}
			}
		}
	}

	@Override
	public boolean evaluateWithObject(Object object) {
		boolean matches;
		String value = (String) NSKeyValueCodingAdditions.Utility.valueForKeyPath(object, _keyPath);
		if (value != null) {
			String lowercaseValue = value.toLowerCase();
			if (_matchType == MatchType.ANY) {
				matches = false;
				for (Enumeration termsEnum = _terms.objectEnumerator(); termsEnum.hasMoreElements();) {
					String term = (String)termsEnum.nextElement();
					if (lowercaseValue.contains(term.toLowerCase())) {
						matches = true;
						break;
					}
				}
			}
			else if (_matchType == MatchType.ALL) {
				matches = true;
				for (Enumeration termsEnum = _terms.objectEnumerator(); termsEnum.hasMoreElements();) {
					String term = (String)termsEnum.nextElement();
					if (!lowercaseValue.contains(term.toLowerCase())) {
						matches = false;
						break;
					}
				}
			}
			else {
				throw new IllegalArgumentException("Unknown match type '" + _matchType + "'.");
			}
		}
		else {
			matches = false;
		}
		return matches;
	}
}
