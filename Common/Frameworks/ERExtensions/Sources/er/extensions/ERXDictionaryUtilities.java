//
// NSDictionaryUtilities.java
// Project vwdBussinessLogicJava
//
// Created by ak on Wed Jun 06 2001
//
package er.extensions;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import java.util.Enumeration;
import java.lang.*;

/**
 * Collection of {@link com.webobjects.foundation.NSDictionary NSDictionary} utilities.
 */
public class ERXDictionaryUtilities extends Object {

    /**
     * Creates an immutable dictionary containing all of the keys and
     * objects from two dictionaries.
     * @param dict1 the first dictionary
     * @param dict2 the second dictionary
     * @return immutbale dictionary containing the union of the two dictionaries.
     */
    public static NSDictionary dictionaryWithDictionaryAndDictionary(NSDictionary dict1, NSDictionary dict2) {
        if(dict1 == null || dict1.allKeys().count() == 0) 
            return dict2;
        if(dict2 == null || dict2.allKeys().count() == 0) 
            return dict1;
            
        NSMutableDictionary result = new NSMutableDictionary(dict2);
        result.addEntriesFromDictionary(dict1);
        return new NSDictionary(result);
    }
    
    /**
     * Creates an NSDictionary from a resource associated with a given bundle
     * that is in property list format.<br/>
     * @param name name of the file or resource.
     * @param bundle NSBundle to which the resource belongs.
     * @return NSDictionary de-serialized from the property list.
     */
    public static NSDictionary dictionaryFromPropertyList(String name, NSBundle bundle) {
        return (NSDictionary)NSPropertyListSerialization.propertyListFromString(ERXStringUtilities.stringFromResource(name, "plist", bundle));
    }

    /**
     * Creates a dictionary from a list of alternating objects and keys
     * starting with an object.
     * @param objectsAndKeys alternating list of objects and keys
     * @return NSDictionary containing all of the object-key pairs.
     */
    public static NSDictionary dictionaryWithObjectsAndKeys(Object[] objectsAndKeys) {
        NSMutableDictionary result = new NSMutableDictionary();
        Object object;
        String key;
        int length = objectsAndKeys.length;
        for(int i = 0; i < length; i+=2) {
            object = objectsAndKeys[i];
            if(object == null) {
                break;
            }
            key = (String)objectsAndKeys[i+1];
            result.setObjectForKey(object,key);
        }
        return new NSDictionary(result);
    }
}
