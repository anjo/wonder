// Priority.java
// 
package er.bugtracker;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import er.extensions.*;

public class Priority extends _Priority {
    static final ERXLogger log = ERXLogger.getERXLogger(Priority.class);

    public static Priority CRITICAL;
    public static Priority HIGH;
    public static Priority MEDIUM;
    public static Priority LOW;

    public Priority() {
        super();
    }

    public void awakeFromInsertion(EOEditingContext ec) {
        super.awakeFromInsertion(ec);
    }
    
    
    // Class methods go here
    
    public static class PriorityClazz extends _PriorityClazz {
        public Priority sharedStateForKey(String key) {
            return (Priority)objectWithPrimaryKeyValue(EOSharedEditingContext.defaultSharedEditingContext(), key);
        }

        public void initializeSharedData() {
            Priority.CRITICAL = sharedStateForKey("crtl");
            Priority.HIGH = sharedStateForKey("high");
            Priority.MEDIUM = sharedStateForKey("medm");
            Priority.LOW = sharedStateForKey("low ");
        }
    }

    public static final PriorityClazz clazz = (PriorityClazz)EOGenericRecordClazz.clazzForEntityNamed("Priority");
}
