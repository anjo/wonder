package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.directtoweb.*;
import er.extensions.*;
import java.util.*;

/**
 * This assignment runs it's value through the localizer and evaluates
 * it as a template before returning it. Meaning something like
 *   *true* => cancelCreationMessage = "ERD2W.cancelCreationMessage"
 * will look up "ERD2W.cancelCreationMessage", then look at the translation
 * and finally parses it with the D2WContext as a parameter.
 * Of course, you can also simply give it a string like
 *  "Cancel creating this @@displayNameForEntity@@"
 */

public class ERDLocalizedAssignment extends ERDAssignment implements ERDLocalizableAssignmentInterface {

    /** logging support */
    static final ERXLogger log = ERXLogger.getERXLogger(ERDLocalizedAssignment.class);

    /**
     * Static constructor required by the EOKeyValueUnarchiver
     * interface. If this isn't implemented then the default
     * behavior is to construct the first super class that does
     * implement this method. Very lame.
     * @param eokeyvalueunarchiver to be unarchived
     * @return decoded assignment of this class
     */
    public static Object decodeWithKeyValueUnarchiver(EOKeyValueUnarchiver eokeyvalueunarchiver)  {
        return new ERDLocalizedAssignment (eokeyvalueunarchiver);
    }
    
    /** 
     * Public constructor
     * @param u key-value unarchiver used when unarchiving
     *		from rule files. 
     */
    public ERDLocalizedAssignment (EOKeyValueUnarchiver u) { super(u); }
    
    /** 
     * Public constructor
     * @param key context key
     * @param value of the assignment
     */
    public ERDLocalizedAssignment (String key, Object value) { super(key,value); }

    /**
     * Implementation of the {@link ERDComputingAssignmentInterface}. This
     * assignment depends upon the template keys from the value of this assignment.
     * This array of keys is used when constructing the
     * significant keys for the passed in keyPath.
     * @param keyPath to compute significant keys for.
     * @return array of context keys this assignment depends upon.
     */
    public NSArray dependentKeys(String keyPath) {
        NSMutableArray dependentKeys = new NSMutableArray();
        String key = (String)value();
        for(Enumeration languages = ERXLocalizer.availableLanguages().objectEnumerator(); languages.hasMoreElements();) {
            String language = (String)languages.nextElement();
            String format = ERXLocalizer.localizerForLanguage(language).localizedStringForKeyWithDefault(key);
            dependentKeys.addObjectsFromArray(ERXSimpleTemplateParser.sharedInstance().keysInTemplate(format, null));
        }
        if (log.isDebugEnabled())
            log.debug("dependentKeys: " + dependentKeys);
        return ERXArrayUtilities.arrayWithoutDuplicates(dependentKeys);
    }

    public Object fire(D2WContext c) {
        String key = (String)value();
        if (log.isDebugEnabled()) {
            String format = (String)ERXLocalizer.currentLocalizer().localizedStringForKeyWithDefault(key);
            log.debug("Fire for template \"" + key + "\": " + format);
        }
        return ERXLocalizer.currentLocalizer().localizedTemplateStringForKeyWithObject(key, c);
    }
}
