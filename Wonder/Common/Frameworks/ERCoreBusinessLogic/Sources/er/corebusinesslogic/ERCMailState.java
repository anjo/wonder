// ERCMailState.java
// (c) by Anjo Krank (ak@kcmedia.ag)
package er.corebusinesslogic;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import er.extensions.*;

/**
 * Mail state.
 * You must populate your DB via the populate.sql script (you might need to adapt it)
 */
public class ERCMailState extends _ERCMailState {
    static final ERXLogger log = ERXLogger.getLogger(ERCMailState.class);

    public static ERCMailState EXCEPTION_STATE;
    public static ERCMailState READY_TO_BE_SENT_STATE;
    public static ERCMailState SENT_STATE;
    public static ERCMailState RECEIVED_STATE;
    public static ERCMailState WAIT_STATE;

    public ERCMailState() {
        super();
    }

    public void awakeFromInsertion(EOEditingContext ec) {
        super.awakeFromInsertion(ec);
    }
    
    
    // Class methods go here
    
    public static class ERCMailStateClazz extends _ERCMailStateClazz {
        public ERCMailState sharedMailStateForKey(String key) {
            return (ERCMailState)objectWithPrimaryKeyValue(EOSharedEditingContext.defaultSharedEditingContext(), key);
        }
        
        public void initializeSharedData() {
            ERCMailState.EXCEPTION_STATE = sharedMailStateForKey("xcpt");
            ERCMailState.READY_TO_BE_SENT_STATE = sharedMailStateForKey("rtbs");
            ERCMailState.SENT_STATE = sharedMailStateForKey("sent");
            ERCMailState.RECEIVED_STATE = sharedMailStateForKey("rcvd");
            ERCMailState.WAIT_STATE = sharedMailStateForKey("wait");
        }
    }

    public static ERCMailStateClazz mailStateClazz() { return (ERCMailStateClazz)EOGenericRecordClazz.clazzForEntityNamed("ERCMailState"); }
}
