// Framework.java
// 
package er.bugtracker;
import org.apache.log4j.Logger;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;

import er.extensions.ERXS;

public class Framework extends _Framework {
    static final Logger log = Logger.getLogger(Framework.class);

    public Framework() {
        super();
    }

    public void init(EOEditingContext ec) {
        super.init(ec);
    }
    
    public void grabHat() {
        setOwner(People.clazz.currentUser(editingContext()));
        setOwnedSince(new NSTimestamp());
    }
    
    public void releaseHat() {
        setOwner(null);
        setOwnedSince(null);
    }
    
    // Class methods go here

    public static class FrameworkClazz extends _FrameworkClazz {

        public NSArray orderedFrameworks(EOEditingContext ec) {
            return objectsMatchingQualifier(ec, null, ERXS.ascs(Key.NAME));
        }

    }

    public static FrameworkClazz clazz = new FrameworkClazz();
}
