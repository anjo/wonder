//
//  ERXNavigationComponent.java
//  ERExtensions
//
//  Created by Max Muller on Wed Oct 30 2002.
//
package er.extensions;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;

public abstract class ERXNavigationComponent extends WOComponent {

    public ERXNavigationComponent(WOContext context) {
        super(context);
    }
    
    public void appendToResponse(WOResponse r, WOContext c) {
        ERXNavigationState state = ERXNavigationManager.manager().navigationStateForSession(session());
        if (shouldSetNavigationState()) {
            state.setState(navigationState());
        } else if (shouldSetNavigationLevel()) {
            state.setStateForLevel(navigationLevelState(), navigationLevel());
        }
        state.setAdditionalState(additionalNavigationState());
        super.appendToResponse(r,c);
    }

    public NSArray navigationState() { return null; }
    public NSArray additionalNavigationState() { return null; }
    public boolean shouldSetNavigationState() { return true; }

    // Support for setting nav levels by themselves.  Nav levels are 1, 2, 3 ...
    public boolean shouldSetNavigationLevel() { return false; }
    public int navigationLevel() { return 0; }
    public String navigationLevelState() { return ""; }
        
}
