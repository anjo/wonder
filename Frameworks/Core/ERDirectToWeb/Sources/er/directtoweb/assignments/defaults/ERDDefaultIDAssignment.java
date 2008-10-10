package er.directtoweb.assignments.defaults;

import org.apache.log4j.Logger;

import com.webobjects.directtoweb.D2WContext;
import com.webobjects.eocontrol.EOKeyValueUnarchiver;
import com.webobjects.foundation.*;

import er.directtoweb.assignments.ERDAssignment;
import er.extensions.foundation.ERXDictionaryUtilities;
import er.extensions.foundation.ERXStringUtilities;

/**
 * An assignment to auto compute/infer a unique-ish, human-readable DOM ids from the d2wContext for Selenium, CSS, Ajax, Javascript, etc.
 * Providing defaults for the following keys:
 * <ul>
 * <li><code>idForProperty</code></li>
 * <li><code>idForSection</code></li>
 * <li><code>idForPageConfiguration</code></li>
 * </ul>
 * 
 * To use: Bind D2W component id binding to d2wContext.id
 * 
 * You may also override the auto computed id by setting rules for propertyKeys, sections, entities, etc.
 * For Ajax updates you may also employ (key value) rules for an updateContainerID key and set it to idForSection, etc as required.
 * 
 * @author mendis
 *
 */
public class ERDDefaultIDAssignment extends ERDAssignment {
	
    /** logging support */
    public final static Logger log = Logger.getLogger(ERDDefaultIDAssignment.class);

    /** holds the array of keys this assignment depends upon */
    protected static final NSDictionary keys = ERXDictionaryUtilities.dictionaryWithObjectsAndKeys( new Object [] {
        new NSArray(new Object[] {"propertyKey", "task", "entity.name"}), "idForProperty",
        new NSArray(new Object[] {"pageConfiguration", "task", "entity.name", "sectionKey"}), "idForSection",
        new NSArray(new Object[] {"pageConfiguration", "task", "entity.name"}), "idForPageConfiguration",
    });

    /**
     * Implementation of the {@link ERDComputingAssignmentInterface}. This array
     * of keys is used when constructing the
     * significant keys for the passed in keyPath.
     * @param keyPath to compute significant keys for.
     * @return array of context keys this assignment depends upon.
     */
    public NSArray dependentKeys(String keyPath) {
        return (NSArray)keys.valueForKey(keyPath);
    }
    
    /**
     * Static constructor required by the EOKeyValueUnarchiver
     * interface. If this isn't implemented then the default
     * behavior is to construct the first super class that does
     * implement this method. Very lame.
     * @param eokeyvalueunarchiver to be unarchived
     * @return decoded assignment of this class
     */
     // ENHANCEME: Only ever need one of these assignments.
    public static Object decodeWithKeyValueUnarchiver(EOKeyValueUnarchiver eokeyvalueunarchiver)  {
        return new ERDDefaultIDAssignment(eokeyvalueunarchiver);
    }
    
    /** 
     * Public constructor
     * @param u key-value unarchiver used when unarchiving
     *		from rule files. 
     */
    public ERDDefaultIDAssignment (EOKeyValueUnarchiver u) { super(u); }
    
    /** 
     * Public constructor
     * @param key context key
     * @param value of the assignment
     */
    public ERDDefaultIDAssignment (String key, Object value) { super(key,value); }
    
    /**
     * a DOM id based on the triple <task, entity, propertyKey>
     * 
     * @param c d2w context
     * @return an id representing the <task, entity, propertyKey>
     * 
     * TODO Maybe change to pageConfig + propertyKey?
     */
    public Object idForProperty(D2WContext c) {
    	return ERXStringUtilities.safeIdentifierName(c.entity().name() + "_" + c.task() + "_" + c.propertyKey());
    }
    
    /**
     * A DOM id based on the pageConfig
     * 
     * @param c d2w context
     * @return an id representing the pageConfig
     */
    public Object idForPageConfiguration(D2WContext c) {
    	return ERXStringUtilities.safeIdentifierName((String) c.valueForKey("pageConfiguration"));
    }
    
    /**
     * A DOM id based on the pageConfig
     * 
     * @param c d2w context
     * @return an id representing the section in a tab page
     */
    public Object idForSection(D2WContext c) {
    	return ERXStringUtilities.safeIdentifierName((String) c.valueForKey("pageConfiguration") + "_" + c.valueForKey("sectionKey"));
    }
}
