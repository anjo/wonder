//
// ERDBranchDelegateInterface.java
// Project ERDirectToWeb
//
// Created by max on Tue Sep 24 2002
//
package er.directtoweb;

import com.webobjects.directtoweb.*;
import com.webobjects.foundation.*;

/**
 * Extension of the NextPageDelegate to provide branch 
 * choices from the delegate to the template.
 */
public interface ERDBranchDelegateInterface extends NextPageDelegate {

    /**
     * Calculates which branches to show in the display first
     * asking the context for the key <b>branchChoices</b> if
     * this returns null then using reflection all of the 
     * public methods that take a single WOComponent as a 
     * parameter are returned.
     * @param context current D2W context
     * @return array of branch names.
     */
    public NSArray branchChoicesForContext(D2WContext context);
}
