//
// ERXEOControlUtilities.java
// Project ERExtensions
//
// Created by max on Wed Oct 09 2002
//
package er.extensions;

import com.webobjects.foundation.NSArray;

import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOModelGroup;
import com.webobjects.eoaccess.EOUtilities;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOObjectStoreCoordinator;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSharedEditingContext;

import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;

/**
 * Collection of EOF utility method centered around
 * EOControl.
 */
public class ERXEOControlUtilities {

    /** logging support */
    public static final ERXLogger log = ERXLogger.getERXLogger(ERXEOControlUtilities.class);
    
    /**
     * Turns a given enterprise object back into a fault.
     * @param eo enterprise object to refault
     */
    public static void refaultObject(EOEnterpriseObject eo) {
        if (eo != null && !eo.isFault()) {
            EOEditingContext ec = eo.editingContext();
            NSArray gids = new NSArray(ec.globalIDForObject(eo));
            ec.invalidateObjectsWithGlobalIDs(gids);
        }
    }

    /**
     * Constructs a fetch specification that will only fetch the primary
     * keys for a given qualifier.
     * @param ec editing context, only used to determine the entity
     * @param entityName name of the entity, only used to determine the entity
     * @param eoqualifier to construct the fetch spec with
     * @param sortOrderings array of sort orderings to sort the result set
     *		with.
     * @param additionalKeys array of additional key paths to construct the
     *		raw rows key paths to fetch.
     * @return fetch specification that can be used to fetch primary keys for
     * 		a given qualifier and sort orderings.
     */
    public static EOFetchSpecification primaryKeyFetchSpecificationForEntity(EOEditingContext ec,
                                                                      String entityName,
                                                                      EOQualifier eoqualifier,
                                                                      NSArray sortOrderings,
                                                                      NSArray additionalKeys) {
        EOModelGroup group = EOModelGroup.modelGroupForObjectStoreCoordinator((EOObjectStoreCoordinator)ec.rootObjectStore());
        EOEntity entity = group.entityNamed(entityName);
        EOFetchSpecification fs = new EOFetchSpecification(entityName, eoqualifier, sortOrderings);
        fs.setFetchesRawRows(true);
        NSMutableArray keys = new NSMutableArray(entity.primaryKeyAttributeNames());
        if (additionalKeys != null) {
            keys.addObjectsFromArray(additionalKeys);
        }
        fs.setRawRowKeyPaths(keys);
        return fs;
    }

    /**
    * Fetches an array of primary keys matching a given qualifier
     * and sorted with a given array of sort orderings.
     * @param ec editing context to fetch into
     * @param entityName name of the entity
     * @param eoqualifier to restrict matching primary keys
     * @param sortOrderings array of sort orders to sort result set
     * @return array of primary keys matching a given qualifier
     */
    public static NSArray primaryKeysMatchingQualifier(EOEditingContext ec, String entityName, EOQualifier eoqualifier, NSArray sortOrderings) {
        EOFetchSpecification fs = ERXEOControlUtilities.primaryKeyFetchSpecificationForEntity(ec, entityName, eoqualifier, sortOrderings, null);
        return ec.objectsWithFetchSpecification(fs);
    }

    /**
     * Returns the number of objects matching the given
     * qualifier for a given entity name. Implementation
     * wise this method will generate the correct sql to only
     * perform a count, i.e. all of the objects wouldn't be
     * pulled into memory.
     * @param ec editing context to use for the count qualification
     * @param entityName name of the entity to fetch
     * @param qualifier to find the matching objects
     * @return number of matching objects
     */
    public static Number objectCountWithQualifier(EOEditingContext ec, String entityName, EOQualifier qualifier) {
        EOEntity entity = EOUtilities.entityNamed(ec, entityName);
        
        NSArray results = null;

        EOAttribute attribute = EOGenericRecordClazz.objectCountAttribute();
        
        EOQualifier schemaBasedQualifier = entity.schemaBasedQualifier(qualifier);
        EOFetchSpecification fs = new EOFetchSpecification(entityName, schemaBasedQualifier, null);
        synchronized (entity) {
            entity.addAttribute(attribute);

            fs.setFetchesRawRows(true);
            fs.setRawRowKeyPaths(new NSArray(attribute.name()));

            results = ec.objectsWithFetchSpecification(fs);

            entity.removeAttribute(attribute);
        }
        if ((results != null) && (results.count() == 1)) {
            NSDictionary row = (NSDictionary) results.lastObject();
            return (Number)row.objectForKey(attribute.name());
        }
        return null;
    }

    /**
     * Finds an object  in the shared editing context matching a key
     * and value. This has the benifit of not requiring a database
     * round trip if the entity is shared.
     * @param entityName name of the shared entity
     * @param key to match against
     * @param value value to match
     * @return matching shared object
     */
    public static EOEnterpriseObject sharedObjectMatchingKeyAndValue(String entityName, String key, Object value) {
        NSArray filtered = sharedObjectsMatchingKeyAndValue(entityName, key, value);
        if (filtered.count() > 1)
            log.warn("Found multiple shared objects for entityName: " + entityName + " matching key: "
                     + key + " value: " + value + " matched against: " + filtered);
        return filtered.count() > 0 ? (EOEnterpriseObject)filtered.lastObject() : null;
    }

    /**
     * Finds objects in the shared editing context matching a key
     * and value. This has the benifit of not requiring a database
     * round trip if the entity is shared.
     * @param entityName name of the shared entity
     * @param key to match against
     * @param value value to match
     * @return array of shared objects matching key and value
     */
    public static NSArray sharedObjectsMatchingKeyAndValue(String entityName, String key, Object value) {
        NSArray filtered = null;
        EOKeyValueQualifier qualifier = new EOKeyValueQualifier(key, EOQualifier.QualifierOperatorEqual, value);
        NSArray sharedEos = (NSArray)EOSharedEditingContext.defaultSharedEditingContext().objectsByEntityName().objectForKey(entityName);
        if (sharedEos != null) {
            filtered = EOQualifier.filteredArrayWithQualifier(sharedEos, qualifier);
        } else {
            log.warn("Unable to find any shared objects for entity name: " + entityName);
        }
        return filtered != null ? filtered : NSArray.EmptyArray;
    }

    /**
     * Gets all of the shared objects for a given entity name. Note that
     * if the entity has not been loaded yet, ie it's model has not been
     * accessed then this method will return an empty array.
     * @param entityName name of the shared entity
     * @return array of bound shared objects for the given entity name
     */
    public static NSArray sharedObjectsForEntityNamed(String entityName) {
        NSArray sharedEos = (NSArray)EOSharedEditingContext.defaultSharedEditingContext().objectsByEntityName().objectForKey(entityName);
        if (sharedEos == null) {
            log.warn("Unable to find any shared objects for the entity named: " + entityName);
        }
        return sharedEos != null ? sharedEos : NSArray.EmptyArray;
    }

    /**
     * Gets a fetch specification from a given entity. If qualifier binding variables
     * are passed in then the fetchspecification is cloned and the binding variables
     * are substituted returning a fetch specification that can be used.
     * @param entityName name of the entity that the fetch specification is bound to
     * @param fetchSpecificationName name of the fetch specification
     * @param dictionary of qualifier bindings
     * @return fetch specification identified by name and potentially with the qualifier
     * 		bindings replaced.
     */
    public static EOFetchSpecification fetchSpecificationNamedWithBindings(String entityName,
                                                                    String fetchSpecificationName,
                                                                    NSDictionary bindings) {
        EOFetchSpecification fetchSpec = EOFetchSpecification.fetchSpecificationNamed(fetchSpecificationName,
                                                                                      entityName);
        return fetchSpec != null && bindings != null ?
            fetchSpec.fetchSpecificationWithQualifierBindings(bindings) : fetchSpec;
    }
}
