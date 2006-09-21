package er.corebusinesslogic;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOGlobalID;

import er.extensions.EOEnterpriseObjectClazz;
import er.extensions.ERXEC;
import er.extensions.ERXEnterpriseObjectCache;

public class ERCHelpText extends _ERCHelpText {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ERCHelpText.class);

    public final static String ENTITY = "ERCHelpText";
    public static final ERCHelpTextClazz clazz = (ERCHelpTextClazz)EOEnterpriseObjectClazz.clazzForEntityNamed(ENTITY); 
    
    public static class ERCHelpTextClazz extends _ERCHelpTextClazz {

        ERXEnterpriseObjectCache cache = new ERXEnterpriseObjectCache(ENTITY, Key.KEY) {

            protected void handleUnsuccessfulQueryForKey(Object key) {
                EOEditingContext ec = ERXEC.newEditingContext();
                EOGlobalID gid = NO_GID_MARKER;
                ec.lock();
                try {
                    EOEnterpriseObject eo = EOUtilities.objectMatchingKeyAndValue(ec, ENTITY, Key.KEY, key);
                    if(eo != null) {
                        gid = ec.globalIDForObject(eo);
                    }
                } finally {
                    ec.unlock();
                }
                cache().put(key, gid);
            }
        };
        
        public ERCHelpText helpTextForKey(EOEditingContext ec, String key) {
            return (ERCHelpText) cache.objectForKey(ec, key);
        }
    }

    public interface Key extends _ERCHelpText.Key {
    }


    /**
     * Intitializes the EO. This is called when an EO is created, not when it is 
     * inserted into an EC.
     */
    public void init(EOEditingContext ec) {
        super.init(ec);
    }
    
    // more EO methods here
}
