//
//  ERXPathDirectAction.java
//  ERExtensions
//
//  Created by Max Muller III on Fri Sep 19 2003.
//
package er.extensions;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.log4j.Logger;

import com.webobjects.appserver.*;
import com.webobjects.appserver._private.*;
import com.webobjects.foundation.*;

/**
 * Utility direct action class that provides a bunch of nice utility methods
 * if the direct action is accessed via a path direct action. See
 * {@class ERXPathDirectActionRequestHandler ERXPathDirectActionRequestHandler}
 * for more information.
 */
public class ERXPathDirectAction extends WODirectAction {

    //	===========================================================================
    //	Class Constant(s)
    //	---------------------------------------------------------------------------
    
    /** logging support */
    protected static final Logger log = Logger.getLogger(ERXPathDirectAction.class);

    //	===========================================================================
    //	Instance Variable(s)
    //	---------------------------------------------------------------------------

    /** caches the path parts */
    protected NSArray pathParts;
    /** caches the path parts by keys */    
    protected NSDictionary pathPartsByKeys;
    /** caches the path parts by case insensitive keys */    
    protected NSDictionary pathPartsByKeysCaseInsensitive;

    //	===========================================================================
    //	Instance Constructor(s)
    //	---------------------------------------------------------------------------

    /**
     * Just calls super.
     */
    public ERXPathDirectAction(WORequest aRequest) {
        super(aRequest);
    }

    //	===========================================================================
    //	Instance Method(s)
    //	---------------------------------------------------------------------------    

    /**
     * The path parts of a request correspond to to all of the pieces between the
     * request handler key and the class name. For instance given the direct action
     * path /WebObjects/MyApp.woa/wpa/foo/bar/MyDirectActionClass/action would
     * produce the path parts (foo, bar).
     * @return array of path parts for the given request
     */
    public NSArray pathParts() {
        if (pathParts == null) {
            if (request().requestHandlerPathArray().count() > 2) {
                pathParts = request().requestHandlerPathArray().subarrayWithRange(new NSRange(1, request().requestHandlerPathArray().count() - 2));
            }
            if (pathParts == null) {
                pathParts = NSArray.EmptyArray;
            } else {
                // Need to correctly WOUrlDecode the path parts
                NSMutableArray temp = new NSMutableArray(pathParts.count());
                for (Enumeration pathPartEnumerator = pathParts.objectEnumerator();
                     pathPartEnumerator.hasMoreElements();) {
                    try {
                        temp.addObject(URLDecoder.decode((String)pathPartEnumerator.nextElement(), WOURLEncoder.WO_URL_ENCODING));
                    } catch (UnsupportedEncodingException e) {
                        log.error("Encoding not found: " + WOURLEncoder.WO_URL_ENCODING, e);
                    }
                }
                pathParts = temp;
            }
            if (log.isDebugEnabled())
                log.debug("Generated path parts: " + pathParts + " for uri: " + request().uri());
        }
        return pathParts;
    }

    /**
     * Given an array of path parts (foo=food, bar=baz, gee) this will
     * produce a dictionary of the form: { foo=food; bar=baz; }. In the current
     * for this method does not handle multiple keys with the same name.
     * @return path parts broken down by key
     */
    public NSDictionary pathPartsByKeys() {
        if (pathPartsByKeys == null) {
            NSMutableDictionary temp = null;
            NSMutableDictionary tempCaseInsensitive = null;
            for (Enumeration pathEnumerator = pathParts().objectEnumerator();
                 pathEnumerator.hasMoreElements();) {
                String path = (String)pathEnumerator.nextElement();
                if (path.indexOf('=') != -1) {
                    if (temp == null) {
                        temp = new NSMutableDictionary();
                        tempCaseInsensitive = new NSMutableDictionary();
                    }
                    NSArray parts = NSArray.componentsSeparatedByString(path, "=");
                    if (parts.count() == 2) {
                        temp.setObjectForKey(parts.objectAtIndex(1),
                                             parts.objectAtIndex(0));
                        tempCaseInsensitive.setObjectForKey(parts.objectAtIndex(1),
                                             ((String)parts.objectAtIndex(0)).toLowerCase());                        
                    }
                }
            }
            pathPartsByKeys = temp != null ? temp : NSDictionary.EmptyDictionary;
            pathPartsByKeysCaseInsensitive = tempCaseInsensitive != null ? tempCaseInsensitive : NSDictionary.EmptyDictionary;
        }
        return pathPartsByKeys;
    }

    /**
     * Same method as pathPartsByKeys except all of the keys have been down cased, ie
     * for path parts (Foo=food, bAr=baz, gee) this will give a dictionary of the form
     * { foo=food; bar=baz; }.
     * @return path parts by case insensitive key
     */
    public NSDictionary pathPartsByKeysCaseInsensitive() {
        if (pathPartsByKeysCaseInsensitive == null)
            pathPartsByKeys();
        return pathPartsByKeysCaseInsensitive;
    }

    /**
     * Determines if a path part exists for a given key.
     * @param key path part key
     * @return if a path was specified 
     */
    public boolean hasPathPartForKey(String key) {
        return hasPathPartForKey(key, false);
    }

    /**
     * Determines if a path part exists for a given key
     * with the option of performing a case insensitve
     * comparison.
     * @param key path part key
     * @param caseInsensitive key comparison should be case sensitive
     * @return if a path was specified
     */
    public boolean hasPathPartForKey(String key, boolean caseInsensitive) {
        if (caseInsensitive) {
            key = key.toLowerCase();
        }
        return caseInsensitive ? pathPartsByKeysCaseInsensitive().objectForKey(key) != null : pathPartsByKeys().objectForKey(key) != null;
    }

    /**
     * Gets a path part for a given key.
     * @param key path part key
     * @return path part for the key
     */
    public String pathPartForKey(String key) {
        return pathPartForKeyWithDefault(key, null);
    }

    /**
     * Gets a path part for a given key, returning the default
     * value if nothing was specified.
     * @param key path part key
     * @param defaultValue default value
     * @return path part for the key or the default value
     */    
    public String pathPartForKeyWithDefault(String key, String defaultValue) {
        return pathPartForKeyWithDefault(key, defaultValue, false);
    }

    /**
     * Gets a path part for a given key, returning the default
     * value if nothing was specified. Adds the option for a
     * case insensitive comparison.
     * @param key path part key
     * @param defaultValue default value
     * @param caseInsensitiveCompare key comparison should ignore case
     * @return path part for the key or the default value
     */    
    public String pathPartForKeyWithDefault(String key,
                                            String defaultValue,
                                            boolean caseInsensitiveCompare) {
        String value = null;
        if (caseInsensitiveCompare) {
            key = key.toLowerCase();
            value = (String)pathPartsByKeysCaseInsensitive().objectForKey(key);
        } else {
            value = (String)pathPartsByKeys().objectForKey(key);            
        }
        return value != null ? value : defaultValue;
    }
}
